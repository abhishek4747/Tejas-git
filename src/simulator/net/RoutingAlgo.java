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


*****************************************************************************/
package net;

import java.util.Vector;

public class RoutingAlgo{

	public static enum ALGO{
		WESTFIRST,
		NORTHLAST,
		NEGATIVEFIRST,
		TABLE,
		SIMPLE
	}
	public static enum DIRECTION{
		UP,
		RIGHT,
		DOWN,
		LEFT
	}
	
	public RoutingAlgo.DIRECTION nextBank(Vector<Integer> current, Vector<Integer> destination){
		
		// to find next bank ID
		if(current.elementAt(0) < destination.elementAt(0))
			return DIRECTION.DOWN;
		else if(current.elementAt(0) > destination.elementAt(0))
			return DIRECTION.UP;
		else if(current.elementAt(0) == destination.elementAt(0) && current.elementAt(1) < destination.elementAt(1))
			return DIRECTION.RIGHT;
		else if(current.elementAt(0) == destination.elementAt(0) && current.elementAt(1) > destination.elementAt(1))
			return DIRECTION.LEFT;
		return null;
	}
	
}