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

import java.util.Comparator;
import java.util.PriorityQueue;
import generic.NewEventComparator;
import generic.NewEvent;

public class MemEventQueue 
{
	protected static int clock = 0;
	private static Comparator<NewEvent> eventComparator = new NewEventComparator();
	//protected static ArrayList<PriorityQueue<Event>> eventQueue = new ArrayList<PriorityQueue<Event>>();
	protected static PriorityQueue<NewEvent> eventQueue = new PriorityQueue<NewEvent>(1, eventComparator);
	
	//public MemEventQueue()
	//{
		//for (int i = 0; i < SystemConfig.NoOfCores; i++)
		//{
			//clock[i] = 0;
			//eventQueue.add(new PriorityQueue<Event>(1, eventComparator));
		//}
	//}
}
