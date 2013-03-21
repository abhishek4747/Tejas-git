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

import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Vector;

import net.optical.TopLevelTokenBus;

import config.NocConfig;
import config.SystemConfig;
import memorysystem.nuca.NucaCacheBank;

public class NOC{
	
	public ArrayList<Switch> intermediateSwitch;
	
	public NOC() {
		this.intermediateSwitch = new ArrayList<Switch>();
	}
	
	public static enum TOPOLOGY{
		MESH,
		TORUS,
		TWODMESH,
		HYPERCUBE,
		BUS,
		RING,
		STAR,
		FATTREE,
		OMEGA,
		BUTTERFLY
	}
	public static enum CONNECTIONTYPE{
		ELECTRICAL,
		OPTICAL
	}
	
	public void ConnectBanks(NocInterface networkElements[][],int numRows,int numColumns,NocConfig nocConfig, TopLevelTokenBus tokenBus)
	{
		switch (SystemConfig.nocConfig.topology) {
		case MESH:
			ConnectBanksMesh(networkElements, numRows, numColumns);
			break;
		case TORUS:
			ConnectBanksTorus(networkElements, numRows, numColumns);
			break;
		case BUS :
			ConnectBanksRingBus(networkElements, numRows, numColumns, 1);
			break;
		case RING :
			ConnectBanksRingBus(networkElements, numRows, numColumns, 0);
			break;
		case OMEGA :
			ConnectBanksOmega(networkElements, numColumns,nocConfig);
			break;
		case BUTTERFLY :
			ConnectBanksButterfly(networkElements, numColumns,nocConfig);
			break;
		case FATTREE :
			ConnectBanksFatTree(networkElements, numColumns, nocConfig);
			break;
		}
	}
/*	public void ConnectBanks(NucaCacheBank cacheBank[],int numOfBanks,TOPOLOGY topology){
		switch (topology){
		case BUS :
			ConnectBanksRingBus(cacheBank, numOfBanks, 1);
		case RING :
			ConnectBanksRingBus(cacheBank, numOfBanks, 0);
		}
	}*/
	/************************************************************************
     * Method Name  : ConnectBanksMesh
     * Purpose      : connect a marix of cachebank in mesh topology
     * Parameters   : matrix of cache banks, number of rows & number of columns
     * Return       : void
     *************************************************************************/
	public void ConnectBanksMesh(NocInterface[][] networkElements,int bankRows,int bankColumns)  //connect bank in MESH fashion
	{
		int i,j;
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				if(i==0)                        //setting null for 0th raw up connection
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.UP, networkElements[i-1][j]);
				
				if(j==bankColumns-1)             //right connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, networkElements[i][(j+1)]);
				
				if(i==bankRows-1)             //down connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, networkElements[i+1][j]);
				
				if(j==0)			            //left connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, networkElements[i][(j-1)]);
			}
	    }
	}
	/************************************************************************
     * Method Name  : ConnectBanksTorus
     * Purpose      : connect a marix of cachebank in Torus topology
     * Parameters   : matrix of cache banks, number of rows & number of columns
     * Return       : void
     *************************************************************************/
	public void ConnectBanksTorus(NocInterface[][] networkElements,int bankRows,int bankColumns) //torus connection
	{
		int i,j;
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				if(i==0)                        //setting null for 0th raw up connection
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.UP,networkElements[bankRows-1][j]);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.UP, networkElements[i-1][j]);
				
				if(j==bankColumns-1)             //right connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, networkElements[i][0]);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, networkElements[i][j+1]);
				
				if(i==bankRows-1)             //down connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, networkElements[0][j]);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, networkElements[i+1][j]);
				
				if(j==0)			            //left connections
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, networkElements[i][bankColumns-1]);
				else
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, networkElements[i][j-1]);
			}
	    }
	}
	
	/************************************************************************
     * Method Name  : ConnectBanksRingBus
     * Purpose      : connect a array of cachebank in ring or bus topology
     * Parameters   : matrix of cache banks, number of banks & ring or bus
     * Return       : void
     *************************************************************************/
	public void ConnectBanksRingBus(NocInterface[][] networkElements, int bankRows, int bankColumns, int ringOrBus)
	{
		int i;
		for(i=0;i<bankRows;i++)
		{
			for(int j=0;j<bankColumns;j++)
			{
				networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
				if(j == bankColumns -1)
				{
					if(i== bankRows - 1)
					{
						if(ringOrBus == 0)
							networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT,networkElements[0][0]);
						else
							networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
					}
					else
					{
						networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, networkElements[i+1][0]);
					}
				}
				else
				{
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT,networkElements[i][j+1]);
				}
				networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);

				if(j == 0){
					if(i== 0)
					{
						if(ringOrBus == 0)
							networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT,networkElements[bankRows-1][bankColumns-1]);
						else
							networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
					}
					else
						networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, networkElements[i-1][bankColumns-1]);
				}
				else
				{
					networkElements[i][j].getRouter().SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT,networkElements[i][j-1]);
				}
			}
		}
	}
	
	/* Following method is abandoned. Kept it for reference */
/*	public memorysystem.nuca.NucaCacheBank connectFatTree(NucaCacheBank cacheBank[][], int start, int end)
	{
		if (start > end)
			return null;
		int mid = (start + end)/2;
		NucaCacheBank root = cacheBank[0][mid];
		NucaCacheBank x = connectFatTree(cacheBank, mid+1, end);
		if(x != null)
		{
			x.router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, root);
			root.router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, x);
		}
		else
			root.router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
		root.router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
	    x =  connectFatTree(cacheBank, start, mid-1);
		if(x != null)
		{
			x.router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, root);
			root.router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, x);
		}
		else
			root.router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
		     
		return root;
	}
*/	
	/************************************************************************
     * Method Name  : ConnectBanksFatTree
     * Purpose      : Connect the cache banks in fat tree topology. Initially
     *                it takes a list of cache banks. Create a new element,
     *                add it to head of the list, take last two elements and 
     *                make these two as the children of the head element. 
     *                Repeat this until a single element remains.
     * Parameters   : matrix of cache banks, number of bank columns,
     * 				  NOC configuration
     * Return       : void
     *************************************************************************/
	
	public void ConnectBanksFatTree(NocInterface[][] networkElements, int bankColumns,NocConfig nocConfig)
	{	
		int lastElement;
		String routerClassName = new String("net.Router");
		Vector<Switch> nodes = new Vector<Switch>();
		nodes.clear();
		for(int i=0;i<bankColumns;i++)
			nodes.add(networkElements[0][i].getRouter());
		Switch newOne;
		do{
			newOne = new Switch(nocConfig);
			nodes.add(0,newOne);                                                 //put in front of nodes
			intermediateSwitch.add(newOne);
			newOne.connection[1] = nodes.lastElement();                          //right connection
			if(nodes.lastElement().getClass().getName().equals(routerClassName))
				newOne.range[1] = ((Router)(nodes.lastElement())).reference.getId().elementAt(1);
			else
				newOne.range[1] = nodes.lastElement().range[1];                  //right range
			nodes.lastElement().connection[0] = newOne;
			lastElement = nodes.size();
			nodes.removeElementAt(lastElement - 1);                             //right is connected to rightmost end router in node list
			newOne.connection[3] = nodes.lastElement();                         //left connection
			if(nodes.lastElement().getClass().getName().equals(routerClassName))
				newOne.range[0] = ((Router)(nodes.lastElement())).reference.getId().elementAt(1);
			else
				newOne.range[0] = nodes.lastElement().range[0];                 //left range
			nodes.lastElement().connection[0] = newOne;
			lastElement = nodes.size();
			nodes.removeElementAt(lastElement - 1);                            //left is connected to rightmost end router in node list
		}while(nodes.size()>1);
		//NucaCacheBank root = connectFatTree(cacheBank,0,bankColumns-1);
		System.out.println("test");
		//return nodes.elementAt(0);
	}
	/************************************************************************
     * Method Name  : connectInputOmega
     * Purpose      : connect the first column switches of omega network
     * Parameters   : matrix of cache banks, number of bank columns,
     * 				  NOC configuration
     * Return       : void
     *************************************************************************/
	public Vector<Switch> connectInputOmega(NocInterface[][] networkElements, int bankColumns,NocConfig nocConfig)
	{
		Vector<Switch> switchList = new Vector<Switch>();
		int i;
		Switch newOne;
		for(i=0;i<bankColumns/2;i++)             //connecting first half to first level switch
		{
			newOne = new Switch(nocConfig,0);
			switchList.add(newOne);
			intermediateSwitch.add(newOne);
			networkElements[0][i].getRouter().connection[0] = switchList.elementAt(i);
			switchList.elementAt(i).connection[0] = networkElements[0][i].getRouter();
			networkElements[0][i+bankColumns/2].getRouter().connection[0] = switchList.elementAt(i);  //connecting second half to first level switch
			switchList.elementAt(i).connection[1] = networkElements[0][i+bankColumns/2].getRouter();
		}
		return switchList;
	}
	/************************************************************************
     * Method Name  : connectOutputOmega
     * Purpose      : connect the last column switches of omega network
     * Parameters   : matrix of cache banks, number of bank columns, list of switches
     *                size of list of switches, starting index of switch list
     * Return       : void
     *************************************************************************/
	public void connectOutputOmega(NocInterface[][] networkElements, int bankColumns,Vector<Switch> switchList,
									int switchListsize,int lastLevelStartingindex)
	{
		int i;
		for(i=0;i<bankColumns/2;i++)
		{
			switchList.elementAt(i+lastLevelStartingindex).connection[2] = networkElements[0][2*i].getRouter();
			switchList.elementAt(i+lastLevelStartingindex).connection[3] = networkElements[0][2*i+1].getRouter();
		}
	}
	/************************************************************************
     * Method Name  : ConnectBanksOmega
     * Purpose      : connect a matrix of cachebank in Omega topology.
     * 				  Row number should be 1. Column should be 2^n.
     * 				  Connection consisting of input connection part, output
     * 				  connection part and connection of rest of the parts. 
     * 				  First for loop creates a column of switches and next 
     * 				  for loop connect those switches.   
     * Parameters   : matrix of cache banks,number of bank columns, noc configuration
     * Return       : void
     *************************************************************************/
	public void ConnectBanksOmega(NocInterface[][] networkElements, int bankColumns,NocConfig nocConfig)
	{
		int numberOfSwitchLevels = (int)(Math.log(bankColumns)/Math.log(2));
		int i,j;
		Vector<Switch> switchList;
		Switch newOne;
		switchList = connectInputOmega(networkElements,bankColumns,nocConfig);
		for(i=0;i<numberOfSwitchLevels-1;i++)               //middle connections(note "-1")
		{
			for(j=0;j<bankColumns/2;j++)                    //creating one column on new switches
			{
				newOne = new Switch(nocConfig,i+1);
				switchList.add(newOne);
				intermediateSwitch.add(newOne);
			}
			for(j=0;j<bankColumns/2;j++)
			{												//shuffled connections
				switchList.elementAt(i* bankColumns/2 +j).connection[2] = switchList.elementAt((i+1)* bankColumns/2 + (2*j)%(bankColumns/2));
				switchList.elementAt(i* bankColumns/2 +j).connection[3] = switchList.elementAt((i+1)* bankColumns/2 + (2*j + 1)%(bankColumns/2));
				
			}
		}
		int switchListsize = switchList.size();
		int lastLevelStartingindex = (numberOfSwitchLevels - 1) * bankColumns/2;  //last level connections 
		connectOutputOmega(networkElements,bankColumns,switchList,switchListsize,lastLevelStartingindex);
	}
	/************************************************************************
     * Method Name  : connectInputButterfly
     * Purpose      : connect the first column switches of butterfly network
     * Parameters   : matrix of cache banks, number of bank columns,
     * 				  NOC configuration
     * Return       : void
     *************************************************************************/
	public Vector<Switch> connectInputButterfly(NocInterface[][] networkElements,int bankColumns,NocConfig nocConfig)
	{
		int i;
		Switch newOne;
		Vector<Switch> switchList = new Vector<Switch>();
		for(i=0;i<bankColumns/2;i++){
			newOne = new Switch(nocConfig,0);
			switchList.add(newOne);
			intermediateSwitch.add(newOne);
			networkElements[0][2*i].getRouter().connection[0] = switchList.elementAt(i);
			networkElements[0][2*i+1].getRouter().connection[0] = switchList.elementAt(i);
		}
		return switchList;
	}
	/************************************************************************
     * Method Name  : connectOutputButterfly
     * Purpose      : connect the last column switches of omega network
     * Parameters   : matrix of cache banks, number of bank columns, list of switches
     *                number of levels of switches
     * Return       : void
     *************************************************************************/
	
	public void connectOutputButterfly(NocInterface[][] networkElements,int bankColumns, 
										Vector<Switch> switchList,int numberOfSwitchLevels)
	{
		int i;
		for(i=0;i<bankColumns/2;i++)
		{
			switchList.elementAt((numberOfSwitchLevels -1 )*bankColumns/2 + i).connection[2] = networkElements[0][2*i].getRouter();
			switchList.elementAt((numberOfSwitchLevels -1 )*bankColumns/2 + i).connection[3] = networkElements[0][2*i+1].getRouter();
			//switchList.elementAt((numberOfSwitchLevels -1 )*bankColumns/2 + i+1).connection[2] = cacheBank[0][i].getRouter();
			//switchList.elementAt((numberOfSwitchLevels -1 )*bankColumns/2 + i+1).connection[3] = cacheBank[0][i+1].getRouter();
		}
	}
	/************************************************************************
     * Method Name  : ConnectBanksButterfly
     * Purpose      : connect a matrix of cachebank in butterfly topology.
     * 				  Row number should be 1. Column should be 2^n.
     * 				  Connection consisting of input connection part, output
     * 				  connection part and connection of rest of the parts. 
     * 				  First for loop creates a column of switches and next 
     * 				  for loop connect those switches.   
     * Parameters   : matrix of cache banks,number of bank columns, noc configuration
     * Return       : void
     *************************************************************************/
	
	public void ConnectBanksButterfly(NocInterface[][] networkElements, int bankColumns,NocConfig nocConfig)
	{
		Vector<Switch> switchList;
		int i,j,k;
		Switch newOne;
		switchList = connectInputButterfly(networkElements,bankColumns,nocConfig);
		int numberOfSwitchLevels = (int)(Math.log(bankColumns)/Math.log(2));
		for(i=0;i<numberOfSwitchLevels-1;i++) //middle connections... 
		{
			for(j=0;j<bankColumns/2;j++)                    //creating one column on new switches
			{
				newOne = new Switch(nocConfig,i+1);
				switchList.add(newOne);
				intermediateSwitch.add(newOne);
			}
			for(j=0;j<bankColumns/2;j++)
			{												//shuffled connections
				for(k=0;k<bankColumns/Math.pow(2, i+2);k++)
				{
					switchList.elementAt(i*bankColumns/2 + j + k).connection[2] = switchList.elementAt((i+1)*bankColumns/2 + j + k);
					switchList.elementAt(i*bankColumns/2 + j + k).connection[3] = switchList.elementAt((i+1)*bankColumns/2 + j + k + (int)(bankColumns/Math.pow(2, i+2)));
					switchList.elementAt(i*bankColumns/2 + j + k + (int)(bankColumns/Math.pow(2, i+2))).connection[2] = switchList.elementAt((i+1)*bankColumns/2 + j + k);
					switchList.elementAt(i*bankColumns/2 + j + k + (int)(bankColumns/Math.pow(2, i+2))).connection[3] = switchList.elementAt((i+1)*bankColumns/2 + j + k + (int)(bankColumns/Math.pow(2, i+2)));
				}
				j = j + 2*k-1;
			}
		}
		connectOutputButterfly(networkElements,bankColumns,switchList,numberOfSwitchLevels);
	}
}
