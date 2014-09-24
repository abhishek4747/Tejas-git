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

package net.optical;

import java.util.Vector;

import config.NocConfig;
import memorysystem.nuca.NucaCacheBank;
import net.NOC;
import net.RoutingAlgo;

public class OpticalNOC extends NOC{
	
	//public TopLevelTokenBus tokenBus;
	public TopDataBus dataBus;
	public Vector<BroadcastBus> broadcastBus;
	public EntryPoint entryPoint;

	
	public OpticalNOC() { 
		// TODO Auto-generated constructor stub
	}
	
	public void ConnectBanks(NucaCacheBank cacheBank[][], int bankRows, int bankColumns, NocConfig nocConfig, TopLevelTokenBus tokenBus)
	{
//		int i,j;
//		for(i=0;i<bankRows;i++)
//		{
//			for(j=0;j<bankColumns;j++)
//			{
//				if(i==0)                        //setting null for 0th raw up connection
//					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
//				else
//					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, cacheBank[i-1][j]);
//				
//				cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
//				
//				if(i==bankRows-1)             //down connections
//					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
//				else
//					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, cacheBank[i+1][j]);
//				
//				cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
//			}
//		}
//		CreateOpticalBuses(cacheBank, bankRows,bankColumns,nocConfig, tokenBus);
	}
	
	public void CreateOpticalBuses(NucaCacheBank cacheBank[][], int bankRows, int bankColumns, NocConfig nocConfig, TopLevelTokenBus tokenBus)
	{
//		Vector<TokenBus> tBus = new Vector<TokenBus>();
//		Vector<DataBus> dBus =   new Vector<DataBus>();
//		Vector<BroadcastBus> bcastBus = new Vector<BroadcastBus>();
//		int i;
//		for(i=0; i < bankColumns; i++)
//		{
//			TokenBus tb =     new TokenBus(nocConfig,cacheBank, bankRows, i);
//			DataBus db =      new DataBus(nocConfig, bankRows, cacheBank, i);
//			BroadcastBus bb = new BroadcastBus(nocConfig, bankRows, cacheBank, i);
//			for(int j=0; j < bankRows; j++ )
//			{
//				((OpticalRouter)(cacheBank[j][i].getRouter())).dataBus  = db;
//				((OpticalRouter)(cacheBank[j][i].getRouter())).tokenBus = tb;
//			}
//			tBus.add(tb);
//			dBus.add(db);
//			bcastBus.add(bb);
//		}
//		this.broadcastBus = bcastBus;
//		this.dataBus = new TopDataBus(nocConfig, dBus);
//		
//		this.entryPoint = new EntryPoint(nocConfig, this.dataBus, tokenBus, this.broadcastBus);
//		this.dataBus.setEntryPoint(this.entryPoint);
//	//	TopLevelTokenBus tokenBus1 = new TopLevelTokenBus(nocConfig, tBus, this.entryPoint);
//		tokenBus.setParameters(nocConfig, tBus, this.entryPoint);
//		tokenBus.putToken();
//		for(i=0; i < bankColumns ; i++)
//		{
//			tBus.elementAt(i).topTokenBus = tokenBus;
//			dBus.elementAt(i).topDataBus = dataBus;
//			
//		}
	}
}
