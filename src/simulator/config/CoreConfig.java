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

import generic.MultiPortingType;

public class CoreConfig 
{
	public int ROBSize;
	
	public int LSQSize;
	public int LSQLatency;
	public int LSQAccessPorts;
	public int LSQPortOccupancy;
	public MultiPortingType LSQMultiportType;
	
	public int TLBSize;
	public int TLBLatency;
	public int TLBAccessPorts;
	public int TLBPortOccupancy;

	public int IntALUNum;
	public int IntMulNum;
	public int IntDivNum;

	public int FloatALUNum;
	public int FloatMulNum;
	public int FloatDivNum;

	public int NumPhyIntReg;
	public int NumPhyFloatReg;
	
	public CacheConfig l1Cache = new CacheConfig();
	public CacheConfig l2Cache = new CacheConfig();
	public CacheConfig l3Cache = new CacheConfig();
}