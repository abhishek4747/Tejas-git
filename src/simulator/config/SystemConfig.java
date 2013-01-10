/*****************************************************************************
				Tejas Simulator
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
package config;

import java.util.Hashtable;

import generic.PortType;

public class SystemConfig 
{
	public static int NoOfCores;
	public static CoreConfig[] core; 
	public static Hashtable<String, CacheConfig> declaredCaches;
	public static int mainMemoryLatency;
	public static long mainMemoryFrequency;
	public static PortType mainMemPortType;
	public static int mainMemoryAccessPorts;
	public static int mainMemoryPortOccupancy;
	public static int cacheBusLatency;
	public static String coherenceEnforcingCache;
	public static CacheConfig directoryConfig;
	//Directory Latencies:
	public static int directoryAccessLatency;
	public static int memWBDelay;
	public static int dataTransferDelay;
	public static int invalidationSendDelay;
	public static int invalidationAckCollectDelay;
	public static int ownershipChangeDelay;
	//Clock Gating Style
	public static int clockGatingStyle;

}
