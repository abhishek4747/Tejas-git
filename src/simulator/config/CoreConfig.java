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
package config;

import generic.PortType;
import generic.MultiPortingType;

public class CoreConfig 
{
	public long frequency;
	
	public int LSQSize;
	public int LSQLatency;
	public PortType LSQPortType;
	public int LSQAccessPorts;
	public int LSQPortOccupancy;
	public MultiPortingType LSQMultiportType;
	
	public int TLBSize;
	public int TLBLatency;
	public PortType TLBPortType;
	public int TLBAccessPorts;
	public int TLBPortOccupancy;

	public int DecodeWidth;
	public int DecodeTime;
	public int RenamingTime;
	public int ROBSize;
	public int IWSize;
	public int IntRegFileSize;
	public int FloatRegFileSize;
	public int IntArchRegNum;
	public int FloatArchRegNum;
	public int MSRegNum;
	public PortType RegFilePortType;
	public int RegFilePorts;
	public int RegFileOccupancy;
	public int BranchMispredPenalty;
	
	public int IntALUNum;
	public int IntMulNum;
	public int IntDivNum;
	public int FloatALUNum;
	public int FloatMulNum;
	public int FloatDivNum;
	public int AddressFUNum;
	
	public int IntALULatency;
	public int IntMulLatency;
	public int IntDivLatency;
	public int FloatALULatency;
	public int FloatMulLatency;
	public int FloatDivLatency;
	public int AddressFULatency;
	
	public CacheConfig l1Cache = new CacheConfig();
	public CacheConfig l2Cache = new CacheConfig();
	public CacheConfig l3Cache = new CacheConfig();
}