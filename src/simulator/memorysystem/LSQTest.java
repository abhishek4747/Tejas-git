/*****************************************************************************
				BhartiSim Simulator
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
package memorysystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.junit.Test;
import config.SystemConfig;
import config.XMLParser;
import generic.*;

public class LSQTest 
{
	private BufferedReader currentFile;
	private static GenericMemorySystem memSys;
	private static LSQ[] lsqueue;
	
	static void initializeRun(String args)
	{
		// main trace file
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
		
		// initializing the system
		memSys = new GenericMemorySystem();
		lsqueue = new LSQ[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			lsqueue[i] = new LSQ(SystemConfig.core[i].LSQSize);
		}
	}

	@Test
	public void testLSQ() 
	{
			initializeRun("jitter_pintrace.out");

			start();

			// print statistics 
			for (int i=0; i < SystemConfig.NoOfCores; i++) 
			{
				System.out.println("LSQ[" + i + "] Loads " + lsqueue[i].NoOfLd + " : LSQ[" + i + "] forwards " + lsqueue[i].NoOfForwards);
				//System.out.println("L1[" + i + "] Hits " + Global.memSys[i].l1Cache.hits + " : L1[" + i + "] misses " + Global.memSys[i].l1Cache.misses);
			}
			System.out.println(" ");
			//for (int i=0; i < SystemConfig.NoOfCores; i++) 
			//{
				//System.out.println("TLB[" + i + "] Hits " + Global.memSys[i].TLBuffer.tlbHits + " : TLB[" + i + "] misses " + Global.memSys[i].TLBuffer.tlbMisses);
			//	System.out.println("L1[" + i + "] Hits " + memSys.L1[i].hits + " : L1[" + i + "] misses " + memSys.L1[i].misses);
			//}
			//System.out.println("L2 Hits " + (memSys.cacheList.get("L2")).hits + " : L2 misses " + (memSys.cacheList.get("L2")).misses);
			
		//	System.out.println("Total Loads : " + process.NoOfLd + " Forwards : " + process.lsq1.NoOfForwards);
			// finished successfully 
			System.out.println("Finished Successfully");
			////////System.out.println("Total number of records processed : " + Global.nlines);
			//System.out.println("Running Time : " + elapsed);
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
		int index;
		Random generator = new Random();
		int randomIndex;
		
		// keep reading and processing lines
		try
		{
			while(true) 
			{
				line = currentFile.readLine();
				CacheRequestPacket request = processLine(line);
				if (request == null)
					return;
				
				//Code added for LSQ on top of cache
				if (request.getType() ==  MemoryAccessType.READ)
				{
					lsqueue[request.getThreadID()].NoOfLd++;
					index = lsqueue[request.getThreadID()].addEntry(true, request.getAddr());
					while (index == LSQ.QUEUE_FULL)
					{
						randomIndex = generator.nextInt(3);
						if (randomIndex == 1)
							lsqueue[request.getThreadID()].processROBCommitForPerfectPipeline(lsqueue[request.getThreadID()].head);
						index = lsqueue[request.getThreadID()].addEntry(true, request.getAddr());
					}
					lsqueue[request.getThreadID()].loadValidate(index, request.getAddr());
				}
				else if (request.getType() == MemoryAccessType.WRITE)
				{
					index = lsqueue[request.getThreadID()].addEntry(false, request.getAddr());
					while (index == LSQ.QUEUE_FULL)
					{
						randomIndex = generator.nextInt(3);
						if (randomIndex == 1)
							lsqueue[request.getThreadID()].processROBCommitForPerfectPipeline(lsqueue[request.getThreadID()].head);
						index = lsqueue[request.getThreadID()].addEntry(false, request.getAddr());
					}
					lsqueue[request.getThreadID()].storeValidate(index, request.getAddr());
				}
				randomIndex = generator.nextInt(2);
				if ((randomIndex == 1)&&(lsqueue[request.getThreadID()].curSize != 0))
					lsqueue[request.getThreadID()].processROBCommitForPerfectPipeline(lsqueue[request.getThreadID()].head);
				//Code added for LSQ on top of cache ENDS
				
				//processEntry(request);
				/*if (request.type == CacheRequestPacket.readWrite.READ)
					memSys.read(request.tid, request.addr);
				else
					memSys.write(request.tid, request.addr);*/
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
