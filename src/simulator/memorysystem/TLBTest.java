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

public class TLBTest 
{
	private BufferedReader currentFile;
	@Test
	public void testTLB() 
	{
		try
		{
			currentFile = new BufferedReader(new FileReader("jitter_pintrace.out"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		if(currentFile == null)
		{
			System.err.println("Error opening the specified file");
			System.exit(0);
		}
		
		//Read the XML file
		XMLParser.parse();
		
		TLB[] Buffer = new TLB[SystemConfig.NoOfCores];
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			Buffer[i] = new TLB(SystemConfig.core[i].TLBSize);
		}
		
		try
		{
			while(true) 
			{
				long addr = 0;
				String line = currentFile.readLine();

				if (line.equals("#eof"))
				{
					break;
				}
				StrTok str = new StrTok(line, " ");
				String firstToken = str.next(" ");
				
				int tid = Integer.parseInt(firstToken);
				int cnt = 0;
				while(true) 
				{
					String token = str.next(" \n");
					if(token == null)
						break;
					switch(cnt) 
					{
						case 0:;
							break;
						case 1:
							break;
						case 2:
							addr = getAddr(token);
							break;
					}
					cnt ++;
				}
				Buffer[tid].getPhyAddrPage(addr);
			}
			
			for (int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				System.out.println("TLB ["+ i +"] hits : " + Buffer[i].tlbHits + "\tTLB ["+ i +"] misses : " + Buffer[i].tlbMisses);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	private long getAddr(String token)
	{
		token = token.substring(2, token.length());
		long value = Long.parseLong(token, 16);
		return value;
	}
}
