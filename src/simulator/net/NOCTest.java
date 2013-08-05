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
package net;

import static org.junit.Assert.*;

import java.util.Vector;

import net.NOC.TOPOLOGY;
import net.RoutingAlgo.DIRECTION;
import org.junit.Test;

public class NOCTest {

	RoutingAlgo tester = new RoutingAlgo();
	Vector<Integer> cur = new Vector<Integer>();
	Vector<Integer> dest = new Vector<Integer>();
	Vector<DIRECTION> res = new Vector<RoutingAlgo.DIRECTION>();
	@Test
	public void test() {
		/* MESH */
		cur.add(0);
		cur.add(0);
		dest.add(1);
		dest.add(1);
		res.add(DIRECTION.DOWN);
		res.add(DIRECTION.RIGHT);
		assertEquals("SIMPLE routing algo, MESH", res, tester.XYnextBank(cur, dest, TOPOLOGY.MESH, 4, 4));
		dest.remove(1);
		dest.add(0);
		res.remove(1);
		assertEquals("WestFirstNextBank routing algo, MESH", res, tester.WestFirstnextBank(cur, dest, TOPOLOGY.MESH, 4, 4));
		cur.clear();
		cur.add(0);
		cur.add(1);
		res.clear();
		res.add(DIRECTION.LEFT);
		assertEquals("WestFirstNextBank routing algo, MESH", res, tester.WestFirstnextBank(cur, dest, TOPOLOGY.MESH, 4, 4));
		res.clear();
		res.add(DIRECTION.DOWN);
		res.add(DIRECTION.LEFT);
		assertEquals("NorthLastnextBank routing algo, MESH", res, tester.NorthLastnextBank(cur, dest, TOPOLOGY.MESH, 4, 4));
		assertEquals("NegativeFirstnextBank routing algo, MESH", res, tester.NegativeFirstnextBank(cur, dest, TOPOLOGY.MESH, 4, 4));
		
		/* TORUS */
		
		cur.clear();
		cur.add(0);
		cur.add(0);
		dest.clear();
		dest.add(8);
		dest.add(8);
		res.clear();
		res.add(DIRECTION.UP);
		res.add(DIRECTION.LEFT);
		assertEquals("SIMPLE routing algo, TORUS", res, tester.XYnextBank(cur, dest, TOPOLOGY.TORUS, 8, 8));
		dest.remove(1);
		dest.add(0);
		res.remove(1);
		assertEquals("WestFirstNextBank routing algo, TORUS", res, tester.WestFirstnextBank(cur, dest, TOPOLOGY.TORUS, 8, 8));
		cur.clear();
		cur.add(0);
		cur.add(1);
		res.clear();
		res.add(DIRECTION.LEFT);
		assertEquals("WestFirstNextBank routing algo, TORUS", res, tester.WestFirstnextBank(cur, dest, TOPOLOGY.TORUS, 8, 8));
		res.clear();
		res.add(DIRECTION.UP);
		res.add(DIRECTION.LEFT);
		assertEquals("NorthLastnextBank routing algo, TORUS", res, tester.NorthLastnextBank(cur, dest, TOPOLOGY.TORUS, 8, 8));
		assertEquals("NegativeFirstnextBank routing algo, TORUS", res, tester.NegativeFirstnextBank(cur, dest, TOPOLOGY.TORUS, 8, 8));
		
		/* BUS and RING */
		
		cur.clear();
		cur.add(0);
		cur.add(1);
		dest.clear();
		dest.add(4);
		dest.add(4);		
		System.out.println(cur + " " + dest);
		res.clear();
		res.add(DIRECTION.RIGHT);
		assertEquals("SIMPLE routing algo, BUS", res, tester.XYnextBank(cur, dest, TOPOLOGY.BUS, 4, 4));
		res.clear();
		res.add(DIRECTION.LEFT);
		assertEquals("SIMPLE routing algo, RING", res, tester.XYnextBank(cur, dest, TOPOLOGY.RING, 4, 4));
		
	
		
	}

}
