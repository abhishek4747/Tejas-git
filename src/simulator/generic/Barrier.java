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

	Contributors:  Eldhose Peter
*****************************************************************************/

package generic;

import java.util.Vector;

public class Barrier {
	
	Long address;
	int numThreads;
	int numThreadsArrived;
	Vector<Integer> blockedThreads;
	
	public Barrier(Long address, int numThreads)
	{
		this.address = address;
		this.numThreads = numThreads;
		this.numThreadsArrived = 0;
		this.blockedThreads = new Vector<Integer>();
	}
	
	public Long getBarrierAddress()
	{
		return this.address;
	}
	public void incrementThreads()
	{
		this.numThreadsArrived ++;
//		this.blockedThreads.add(tid);
	}
	public void addThread(int tid){
		this.blockedThreads.add(tid);
	}
	public boolean timeToCross()
	{
		System.out.println("in timetocross numthreads "+ numThreads + " "+ numThreadsArrived);
		return(this.numThreads == this.numThreadsArrived);
	}
	public int getNumThreads(){
		return this.numThreads;
	}
	public int getNumThreadsArrived(){
		return this.numThreadsArrived;
	}
	public Vector<Integer> getBlockedThreads(){
		return this.blockedThreads;
	}
	public int blockedThreadSize(){
		return this.blockedThreads.size();
	}

}
