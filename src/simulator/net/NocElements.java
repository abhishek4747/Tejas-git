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

				Contributor: Anuj Arora
*****************************************************************************/
package net;

import generic.Core;
import generic.PortType;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import config.SimulationConfig;
import config.SystemConfig;
import main.ArchitecturalComponent;
import memorysystem.MainMemoryController;
import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;
import net.NOC;
import net.NOC.CONNECTIONTYPE;
import net.NocInterface;
import net.optical.OpticalNOC;
import net.optical.TopLevelTokenBus;

public class NocElements 
{
	public NOC noc;
	public int rows;
	public int columns;
	public int noOfCores;
	public int noOfCacheBanks;
	public int noOfL1Directories;
	public int noOfMemoryControllers;
	public NocInterface[][] nocInterface;
	public Vector<Core> cores;
	public Vector<CentralizedDirectoryCache> l1Directories;
	public Vector<MainMemoryController> memoryControllers;
	public Vector<Vector<String>> nocElementsLocations;
	NucaType nucaType;
	public NocElements(int r, int c)
	{
		nocInterface = new NocInterface[r][c]; 
		nocElementsLocations = new Vector<Vector<String>>();
		rows = r;
		columns = c;
		this.nucaType = SimulationConfig.nucaType;
		cores = new Vector<Core>();
		l1Directories = new Vector<CentralizedDirectoryCache>();
		memoryControllers = new Vector<MainMemoryController>();
	}
	public void makeNocElements(TopLevelTokenBus tokenbus, NucaCache nucaCache)
	{
        noc = new NOC();
		int coreNumber = 0;
		int cacheNumber =0;
		for(int i=0;i<rows;i++)
		{
			nocInterface[i] = new NocInterface[columns];
			for(int j=0;j<columns;j++)
			{
				if(nocElementsLocations.get(i).get(j).equals("1"))
				{
					nocInterface[i][j] = (NocInterface) ArchitecturalComponent.getCores()[coreNumber].comInterface;
					cores.add(ArchitecturalComponent.getCores()[coreNumber]);
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					(nocInterface[i][j]).setId(id);
					coreNumber++;
				}
				else if(nocElementsLocations.get(i).get(j).equals("0"))
				{
					nocInterface[i][j] = (NocInterface) nucaCache.cacheBank.get(cacheNumber).comInterface;
					cacheNumber++;
				}
				else if(nocElementsLocations.get(i).get(j).equals("D"))
				{
					int dirId = i*columns+j;
					CentralizedDirectoryCache directory = new CentralizedDirectoryCache("Directory", dirId, SystemConfig.directoryConfig, null, noOfCores, SystemConfig.dirNetworkDelay);
					l1Directories.add(directory);
					nocInterface[i][j] = (NocInterface)directory.comInterface;
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					(nocInterface[i][j]).setId(id);
				}
				else if(nocElementsLocations.get(i).get(j).equals("M"))
				{
					MainMemoryController mainMemoryController = new MainMemoryController(nucaType);
					memoryControllers.add(mainMemoryController);
					nocInterface[i][j] =  (NocInterface) mainMemoryController.comInterface;
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					nocInterface[i][j].setId(id);
				}
				else if(nocElementsLocations.get(i).get(j).equals("-"))
				{
					// dummy values in constructor
					nocInterface[i][j] = (NocInterface)((new NocElementDummy(PortType.Unlimited, 1, 1, null, 1, 1)).comInterface);
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
				    nocInterface[i][j].setId(id);
				}
			}
		}
		noc.ConnectBanks(nocInterface,rows,columns,SystemConfig.nocConfig,tokenbus);
	}
	public Vector<Integer> getMemoryControllerId(Vector<Integer> currBankId)//nearest Memory Controller
    {
    	double distance = Double.MAX_VALUE;
    	Vector<Integer> memControllerId = ((NocInterface) (SystemConfig.nocConfig.nocElements.memoryControllers.get(0).comInterface)).getId();
    	int x1 = currBankId.get(0);//bankid/cacheColumns;
    	int y1 = currBankId.get(1);//bankid%cacheColumns;
   
    	for(MainMemoryController memController:SystemConfig.nocConfig.nocElements.memoryControllers)
    	{
    		int x2 = ((NocInterface)memController.comInterface).getId().get(0);
    		int y2 = ((NocInterface)memController.comInterface).getId().get(1);
    		double localdistance = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    		if(localdistance < distance) 
    		{
    			distance = localdistance;
    			memControllerId = ((NocInterface)memController.comInterface).getId();
    		}
    	}
    	return memControllerId;
    }
}
