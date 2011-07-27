/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
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
package memorysystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import config.SystemConfig;
import config.XMLParser;
import generic.*;

public class GenericMemorySystemTest 
{
	private BufferedReader currentFile;
	private static GenericMemorySystem memSys;
	//public int NoOfLd = 0;
	//public LSQ lsq1;
	
	static void initializeRun(String args)
	{
		//Trace file
		try
		{
			Global.mainTraceFile = new BufferedReader(new FileReader(args));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if(Global.mainTraceFile == null)
		{
			System.err.println("Error opening the specified file");
			System.exit(0);
		}

		//Read the XML file
		XMLParser.parse();
		
		// initialising the memory system
		memSys = new GenericMemorySystem();
	}

	@Test
	public void testGenericMemorySystem() 
	{
		initializeRun("jitter_pintrace.out");

		start();

		// print statistics 
		//System.out.println("L2 Hits " + MemorySystem.L2.GenericMemorySystem + ": L2 misses " + MemorySystem.L2.GenericMemorySystem);
		for (int i=0; i < SystemConfig.NoOfCores; i++) 
		{
			System.out.println("TLB[" + i + "] Hits " + memSys.TLBuffer[i].tlbHits + " : TLB[" + i + "] misses " + memSys.TLBuffer[i].tlbMisses);
			//System.out.println("L1[" + i + "] Hits " + Global.memSys[i].l1Cache.hits + " : L1[" + i + "] misses " + Global.memSys[i].l1Cache.misses);
		}
		System.out.println(" ");
		for (int i=0; i < SystemConfig.NoOfCores; i++) 
		{
			//System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + " : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
			System.out.println("L1[" + i + "] Hits " + memSys.L1[i].hits + " : L1[" + i + "] misses " + memSys.L1[i].misses);
		}
		System.out.println("L2 Hits " + (memSys.cacheList.get("L2")).hits + " : L2 misses " + (memSys.cacheList.get("L2")).misses);
		
	//	System.out.println("Total Loads : " + process.NoOfLd + " Forwards : " + process.lsq1.NoOfForwards);
		// finished successfully 
		System.out.println("Finished Successfully");
	}
	
	/* methods */
	private CacheRequestPacket processLine(String sourceLine)
	{
		if (sourceLine.equals("#eof"))
		{
			return null;
		}
		
		//Count the lines
		//Global.nlines++;
			
		// create an Entry
		CacheRequestPacket request = new CacheRequestPacket();

		// read the thread id
		String line;
		line = sourceLine;
		StrTok str = new StrTok(line, " ");
		String firstToken = str.next(" ");
		
		request.setThreadID(Integer.parseInt(firstToken));
		//System.out.print(entry.tid + " ");////////////////////////////////////

		// read the remaining ones
		int cnt = 0;
		while(true) 
		{
			String token = str.next(" \n");
			if(token == null)
				break;
			switch(cnt) 
			{
				case 0:
					request.setTime(Double.parseDouble(token));
					//System.out.print(entry.time + " ");
					break;
				case 1:
					request.setType(getType(token));
					//System.out.print(entry.type + " ");
					break;
				case 2:
					request.setAddr(getAddr(token));
					//System.out.println(entry.addr);
					break;
			}
			cnt ++;
		}
		return request;
	}
	
	private long getAddr(String token)
	{
		token = token.substring(2, token.length());
		long value = Long.parseLong(token, 16);
		return value;
	}
	
	private MemoryAccessType getType(String token)
	{
		if(token.indexOf("R") != -1)
			return MemoryAccessType.READ;
		if(token.indexOf("W") != -1)
			return MemoryAccessType.WRITE;

		System.err.println("Undefined request type " + token);
		System.exit(1);
		return null;
	}
	
	private void run()
	{
		//define the line
		String line;
		//int index;
		//Random generator = new Random();
		//int randomIndex;
		
		//lsq1 = new LSQ(64);

		// keep reading and processing lines
		try
		{
			while(true) 
			{
				line = currentFile.readLine();
				CacheRequestPacket request = processLine(line);
				if (request == null)
					return;
				
				/*//Code added for LSQ on top of cache
				if (entry.type ==  CacheRequestPacket.READ)
				{
					NoOfLd++;
					index = lsq1.addEntry(true, entry.addr);
					while (index == LSQ.QUEUE_FULL)
					{
						randomIndex = generator.nextInt(3);
						if (randomIndex == 1)
							lsq1.processROBCommit(lsq1.head);
						index = lsq1.addEntry(true, entry.addr);
					}
					lsq1.loadValidate(index, entry.addr);
				}
				else if (entry.type ==  CacheRequestPacket.WRITE)
				{
					index = lsq1.addEntry(false, entry.addr);
					while (index == LSQ.QUEUE_FULL)
					{
						randomIndex = generator.nextInt(3);
						if (randomIndex == 1)
							lsq1.processROBCommit(lsq1.head);
						index = lsq1.addEntry(false, entry.addr);
					}
					lsq1.storeValidate(index, entry.addr);
				}
				randomIndex = generator.nextInt(2);
				if ((randomIndex == 1)&&(lsq1.curSize != 0))
					lsq1.processROBCommit(lsq1.head);
				//Code added for LSQ on top of cache ENDS*/
				
				//processEntry(request);
				if (request.getType() == MemoryAccessType.READ)
					memSys.read(request.getThreadID(), request.getAddr());
				else
					memSys.write(request.getThreadID(), request.getAddr());
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void start()
	{
		currentFile = Global.mainTraceFile;
		run();
	}


}
