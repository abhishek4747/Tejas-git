/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright 2010 Indian Institute of Technology, Delhi
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem.directory;

import java.util.*;

import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.Cache.CacheType;
import memorysystem.Cache.CoherenceType;
import memorysystem.directory.CentralizedDirectory;
import memorysystem.directory.DirectoryEntry;
import memorysystem.directory.DirectoryState;
import memorysystem.snoopyCoherence.BusController;

import config.CacheConfig;
import config.SystemConfig;
import misc.Util;
import generic.*;

public class CentralizedDirectoryNew extends SimulationElement
{
		
		/* cache parameters */
		protected int blockSize; // in bytes
		protected int blockSizeBits; // in bits
		protected int assoc;
		protected int assocBits; // in bits
		protected int size; // MegaBytes
		protected int numLines;
		protected int numLinesBits;
		protected double timestamp;
		protected int numLinesMask;
		//TODO implement as hashtable:
		protected Vector<Long> evictedLines = new Vector<Long>();
		
		protected DirectoryEntry lines[];
		
		public int noOfRequests;
		public int hits;
		public int misses;
		public int evictions;
		private CoreMemorySystem containingMemSys;
		public static int numCores;
		
		private Hashtable<Long,DirectoryEntry> victimCache;
		public static final long NOT_EVICTED = -1;
		
		public CentralizedDirectoryNew(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, int numCores)
		{
			super(cacheParameters.portType,
					cacheParameters.getAccessPorts(), 
					cacheParameters.getPortOccupancy(),
					cacheParameters.getLatency(),
					cacheParameters.operatingFreq);
			
			this.containingMemSys = containingMemSys;
			
			// set the parameters
			this.blockSize = cacheParameters.getBlockSize();
			this.assoc = cacheParameters.getAssoc();
			this.size = cacheParameters.getSize();
			this.blockSizeBits = Util.logbase2(blockSize);
			this.assocBits = Util.logbase2(assoc);
			this.numLines = getNumLines();
			
			this.numLinesBits = Util.logbase2(numLines);
			this.timestamp = 0;
			this.numLinesMask = numLines - 1;
			this.noOfRequests = 0;
			this.hits = 0;
			this.misses = 0;
			this.evictions = 0;
			this.numCores = numCores;
			
			lines = new DirectoryEntry[numLines];
			for(int i = 0; i < numLines; i++)
			{
				lines[i] = new DirectoryEntry(numCores,i);
			}
			this.victimCache = new Hashtable<Long,DirectoryEntry>();
		}

		
		public DirectoryEntry accessRead(long addr)
		{
			/* remove the block size */
			long tag = addr >>> this.blockSizeBits;

			/* search all the lines that might match */
			
			long laddr = tag >>> this.assocBits;
			laddr = laddr << assocBits; //Replace the associativity bits with zeros.

			/* remove the tag portion */
			laddr = laddr & numLinesMask;

			/* search in a set */
			for(int idx = 0; idx < assoc; idx++) 
			{
				DirectoryEntry ll = this.lines[(int)(laddr + (long)(idx))];
				if(ll.hasTagMatch(tag) && (ll.getState() != DirectoryState.uncached))
					return  ll;
			}
			return null;
		}
		public DirectoryEntry accessWrite(long addr,DirectoryEntry dirEntry)
		{
			/* remove the block size */
			long tag = addr >>> this.blockSizeBits;

			/* search all the lines that might match */
			
			long laddr = tag >>> this.assocBits;
			laddr = laddr << assocBits; //Replace the associativity bits with zeros.

			/* remove the tag portion */
			laddr = laddr & numLinesMask;

			/* search in a set */
			for(int idx = 0; idx < assoc; idx++) 
			{
				DirectoryEntry ll = this.lines[(int)(laddr + (long)(idx))];
				int lineNum = ll.getLine_num();
				if(ll.hasTagMatch(tag) && (ll.getState() != DirectoryState.uncached)){
					this.lines[(int)(laddr + (long)(idx))]=dirEntry;
					this.lines[(int)(laddr + (long)(idx))].setLine_num(lineNum);
					return  ll;
				}
			}
			//If not in the directory, update in the victim cache
			victimCache.put(addr, dirEntry);
			return null;
		}
		
		private void mark(DirectoryEntry ll, long tag)
		{
			ll.setTag(tag);
			mark(ll);
		}
		
		private void mark(DirectoryEntry ll)
		{
			ll.setTimestamp(timestamp);
			timestamp += 1.0;
		}
		
		private int getNumLines()
		{
			long totSize = size * 1024;
			return (int)(totSize / (long)(blockSize));
		}
		
		
		private DirectoryEntry read(long addr)
		{
			DirectoryEntry cl = accessRead(addr);
			if(cl != null)
				mark(cl);
			return cl;
		}
		
		private DirectoryEntry write(long addr, DirectoryEntry dirEntry)
		{
			DirectoryEntry cl = accessWrite(addr,dirEntry);
			if(cl != null)
				mark(cl);
			return cl;
		}
		
		protected DirectoryEntry fill(long addr, DirectoryEntry dirEntry) //Returns a copy of the evicted line
		{
			DirectoryEntry evictedLine = null;
			
			/* remove the block size */
			long tag = addr >>> this.blockSizeBits;

			/* search all the lines that might match */
			long laddr = tag >>> this.assocBits;
			laddr = laddr << assocBits; // replace the associativity bits with zeros.

			/* remove the tag portion */
			laddr = laddr & numLinesMask;

			/* find any invalid lines -- no eviction */
			DirectoryEntry fillLine = null;
			boolean evicted = false;
			for (int idx = 0; idx < assoc; idx++) 
			{
				DirectoryEntry ll = this.lines[(int)(laddr + (long)(idx))];
				if (!(ll.isValid())) 
				{
					fillLine = ll;
					this.lines[(int)(laddr + (long)(idx))]=dirEntry;
					this.lines[(int)(laddr + (long)(idx))].setLine_num(fillLine.getLine_num());
					this.lines[(int)(laddr + (long)(idx))].address=addr;
					break;
				}
			}
			
			/* LRU replacement policy -- has eviction*/
			if (fillLine == null) 
			{
				evicted = true; // We need eviction in this case
				double minTimeStamp = Double.MAX_VALUE;
				long minIdx=0;
				for(int idx=0; idx<assoc; idx++) 
				{
					DirectoryEntry ll = this.lines[(int)(laddr + (long)(idx))];
					if(minTimeStamp > ll.getTimestamp()) 
					{
						minTimeStamp = ll.getTimestamp();
						minIdx=idx;
						fillLine = ll;
					}
					this.lines[(int)(laddr + (long)(minIdx))]=dirEntry;
					this.lines[(int)(laddr + (long)(minIdx))].setLine_num(fillLine.getLine_num());
				}
			}

			/* if there has been an eviction */
			if (evicted) 
			{
				evictedLine = fillLine.copy();
				
					this.evictions++;
					victimCache.put(addr, evictedLine);
					/* log the line */
//					evictedLines.addElement(fillLine.getTag());
			}

			mark(fillLine, tag);
			return evictedLine;
		}
	
		public DirectoryEntry fetch(long addr)
		{
			noOfRequests++;
			/* access the Directory */
			DirectoryEntry ll = null;
			ll = this.read(addr);
			
			if(ll == null)
			{
				/* Miss */
				ll = victimCache.get(addr);
				if(ll==null){
					ll=new DirectoryEntry(numCores, 0);//Line number will change inside fill -
					DirectoryEntry evicted = fill(addr,ll);
					if(evicted!=null){
						victimCache.put(evicted.address, evicted);
					}
				}
				this.misses++;
			} 
			else 
			{
				/* Hit */
				this.hits++;				
			}
			return ll;
		}
		public DirectoryEntry update(long addr,DirectoryEntry dirEntry)
		{
			DirectoryEntry ll = null;
			ll = this.write(addr,dirEntry);
			
			if(ll == null)
			{
				/* Miss */
				ll = victimCache.get(addr);
				if(ll==null){
					ll=new DirectoryEntry(numCores, 0);//Line number will change inside fill -
					fill(addr,ll);
				}
				this.misses++;
			} 
			else 
			{
				/* Hit */
				this.hits++;				
			}
			return ll;
		}

		@Override
		public void handleEvent(EventQueue eventQ, Event event) {
			// TODO Auto-generated method stub
			
		}
}