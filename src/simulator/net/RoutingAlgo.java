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

import java.util.ArrayList;
import java.util.Vector;

import net.NOC.TOPOLOGY;

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
	public RoutingAlgo.DIRECTION XYnextBank(Vector<Integer> current, Vector<Integer> destination, 
			NOC.TOPOLOGY topology, int numRows, int numColums)
	{	
		//XYRouting for mesh,torus,ring,bus
		// to find next bank ID
		int x1,y1,x2,y2;
		x1 = current.elementAt(0);
		y1 = current.elementAt(1);
		x2 = destination.elementAt(0);
		y2 = destination.elementAt(1);
		if(x1 < x2){
			if(topology == TOPOLOGY.TORUS)
				if((x2-x1) > Math.ceil(numRows / 2))
					return DIRECTION.UP;
			return DIRECTION.DOWN;
		}
		else if(x1 > x2){
			if(topology == TOPOLOGY.TORUS)
				if((x1-x2) > Math.ceil(numRows / 2))
					return DIRECTION.DOWN;
			return DIRECTION.UP;
		}
		else if(x1 == x2 && y1< y2){
			if(topology == TOPOLOGY.TORUS || topology == TOPOLOGY.RING)
				if((y2-y1) > Math.ceil(numColums / 2))
					return DIRECTION.LEFT;	
			return DIRECTION.RIGHT;
		}
		else if(x1 == x2 && y1 >y2){
			if(topology == TOPOLOGY.TORUS || topology == TOPOLOGY.RING)
				if((y1-y2) > Math.ceil(numColums / 2))
					return DIRECTION.RIGHT;
			return DIRECTION.LEFT;
		}
		return null;//you should not be here. destinationId == currentId
	}
	public RoutingAlgo.DIRECTION WestFirstnextBank(Vector<Integer> current, Vector<Integer> destination, 
			NOC.TOPOLOGY topology, int numRows, int numColums)
	{
		int y1,y2;
		y1 = current.elementAt(1);
		y2 = destination.elementAt(1);
		if(y2<y1){
			if(topology == TOPOLOGY.TORUS || topology == TOPOLOGY.RING)
				if((y1-y2) > Math.ceil(numColums / 2))
					return DIRECTION.RIGHT;
			return DIRECTION.LEFT;
		}
		else
			return XYnextBank(current, destination, 
					topology, numRows, numColums);
	}
	public RoutingAlgo.DIRECTION NorthLastnextBank(Vector<Integer> current, Vector<Integer> destination, 
			NOC.TOPOLOGY topology, int numRows, int numColums)
	{
		int x1,y1,x2,y2;
		x1 = current.elementAt(0);
		y1 = current.elementAt(1);
		x2 = destination.elementAt(0);
		y2 = destination.elementAt(1);
		if(x2 < x1){
			if(topology == TOPOLOGY.TORUS || topology == TOPOLOGY.RING)
			{
				if(y2>y1)
				{
					if((y2-y1) > Math.ceil(numColums / 2))
						return DIRECTION.LEFT;
					else
						return DIRECTION.RIGHT;
				}
				else if(y1>y2)
				{
					if((y2-y1) > Math.ceil(numColums / 2))
						return DIRECTION.RIGHT;
					else
						return DIRECTION.LEFT;
				}
			}
			if(y2<y1)
				return DIRECTION.LEFT;
			else if(y1>y2)
				return DIRECTION.RIGHT;
		}
		else
			return XYnextBank(current, destination, 
					topology, numRows, numColums);
		return null; // no mans land
	}
	public RoutingAlgo.DIRECTION NegativeFirstnextBank(Vector<Integer> current, Vector<Integer> destination, 
			NOC.TOPOLOGY topology, int numRows, int numColums)
	{
		int x1,y1,x2,y2;
		x1 = current.elementAt(0);
		y1 = current.elementAt(1);
		x2 = destination.elementAt(0);
		y2 = destination.elementAt(1);
		if(y2 < y1 && x2 < x1){
			if(topology == TOPOLOGY.TORUS || topology == TOPOLOGY.RING)
				if(y1-y2 > Math.ceil(numColums / 2))
					return DIRECTION.RIGHT;
			return DIRECTION.LEFT;
		}
		else
			return XYnextBank(current, destination, 
					topology, numRows, numColums);
	}
	public ArrayList<RoutingAlgo.DIRECTION> XYRouting(Vector<Integer> current, Vector<Integer> destination)
	{
		int x1,y1,x2,y2;
		x1 = current.elementAt(0);
		y1 = current.elementAt(1);
		x2 = destination.elementAt(0);
		y2 = destination.elementAt(1);
		ArrayList<RoutingAlgo.DIRECTION> path = new ArrayList<DIRECTION>();
		while(x1 !=x2 && y1 != y2)
		{
			if(current.elementAt(0) < destination.elementAt(0))
			{
				path.add(DIRECTION.DOWN);
				x1++;
			}
			else if(current.elementAt(0) > destination.elementAt(0))
			{
				path.add(DIRECTION.UP);
				x1--;
			}
			else if(current.elementAt(0) == destination.elementAt(0) && current.elementAt(1) < destination.elementAt(1))
			{
				path.add(DIRECTION.RIGHT);
				y1++;
			}
			else if(current.elementAt(0) == destination.elementAt(0) && current.elementAt(1) > destination.elementAt(1))
			{
				path.add(DIRECTION.LEFT);
				y1--;
			}
			//return null;
		}
		return path;
	}
	
}