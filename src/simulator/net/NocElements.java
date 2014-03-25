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
	public NocInterface[][] nocElements;
	public Vector<Core> cores;
	public Vector<CentralizedDirectoryCache> l1Directories;
	public Vector<MainMemoryController> memoryControllers;
	public Vector<Vector<String>> nocElementsLocations;
	NucaType nucaType;
	public NocElements(int r, int c)
	{
		nocElements = new NocInterface[r][c]; 
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
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
            noc = new NOC();
        else
            noc = new OpticalNOC();
		int coreNumber = 0;
		int cacheNumber =0;
		for(int i=0;i<rows;i++)
		{
			nocElements[i] = new NocInterface[columns];
			for(int j=0;j<columns;j++)
			{
				if(nocElementsLocations.get(i).get(j).equals("1"))
				{
					nocElements[i][j] = (NocInterface) ArchitecturalComponent.getCores()[coreNumber];
					cores.add(ArchitecturalComponent.getCores()[coreNumber]);
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					((Core)nocElements[i][j]).setId(id);
					coreNumber++;
				}
				else if(nocElementsLocations.get(i).get(j).equals("0"))
				{
					nocElements[i][j] = nucaCache.cacheBank.get(cacheNumber);
					cacheNumber++;
				}
				else if(nocElementsLocations.get(i).get(j).equals("D"))
				{
					CentralizedDirectoryCache directory = new CentralizedDirectoryCache(SystemConfig.directoryConfig, null, noOfCores, SystemConfig.dirNetworkDelay);
					l1Directories.add(directory);
					nocElements[i][j] = directory;
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					((CentralizedDirectoryCache)nocElements[i][j]).setId(id);
				}
				else if(nocElementsLocations.get(i).get(j).equals("M"))
				{
					MainMemoryController mainMemoryController = new MainMemoryController(nucaType);
					memoryControllers.add(mainMemoryController);
					nocElements[i][j] =  mainMemoryController;
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					((MainMemoryController)nocElements[i][j]).setId(id);
				}
				else if(nocElementsLocations.get(i).get(j).equals("-"))
				{
					nocElements[i][j] = new NocElementDummy(PortType.Unlimited, 1, 1, null, 1, 1);// dummy values in constructor
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					((NocElementDummy)nocElements[i][j]).setId(id);
				}
			}
		}
		noc.ConnectBanks(nocElements,rows,columns,SystemConfig.nocConfig,tokenbus);
	}
	public Vector<Integer> getMemoryControllerId(Vector<Integer> currBankId)//nearest Memory Controller
    {
    	double distance = Double.MAX_VALUE;
    	Vector<Integer> memControllerId = SystemConfig.nocConfig.nocElements.memoryControllers.get(0).getId();
    	int x1 = currBankId.get(0);//bankid/cacheColumns;
    	int y1 = currBankId.get(1);//bankid%cacheColumns;
   
    	for(MainMemoryController memController:SystemConfig.nocConfig.nocElements.memoryControllers)
    	{
    		int x2 = memController.getId().get(0);
    		int y2 = memController.getId().get(1);
    		double localdistance = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    		if(localdistance < distance) 
    		{
    			distance = localdistance;
    			memControllerId = memController.getId();
    		}
    	}
    	return memControllerId;
    }
}
