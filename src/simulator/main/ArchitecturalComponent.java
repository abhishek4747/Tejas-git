package main;

import emulatorinterface.communication.IpcBase;
import generic.CommunicationInterface;
import generic.Core;
import generic.CoreBcastBus;
import generic.EventQueue;
import generic.GlobalClock;
import generic.SimulationElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import config.CacheConfig;
import config.SystemConfig;

import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.coherence.Coherence;
import memorysystem.coherence.Directory;
import net.Bus;
import net.InterConnect;
import net.NOC;
import net.Router;
import pipeline.outoforder.ICacheBuffer;
import pipeline.outoforder.OutOrderExecutionEngine;

public class ArchitecturalComponent {
	
	public static Vector<Core> cores = new Vector<Core>();
	public static Vector<CoreMemorySystem> coreMemSysArray = new Vector<CoreMemorySystem>();
	public static Vector<Coherence> coherences = new Vector<Coherence>();	
	public static Vector<MainMemoryController> memoryControllers = new Vector<MainMemoryController>();
	public static Vector<Cache> sharedCaches = new Vector<Cache>();
	
	private static InterConnect interconnect;
	public static CoreBcastBus coreBroadcastBus;
		
	public static void createChip() {
		// Interconnect
			// Core
			// Coherence
			// Shared Cache
			// Main Memory Controller

		if(SystemConfig.interconnect ==  SystemConfig.Interconnect.Bus) {
			ArchitecturalComponent.setInterConnect(new Bus());
		} else if(SystemConfig.interconnect == SystemConfig.Interconnect.Noc) {
			ArchitecturalComponent.setInterConnect(new NOC(SystemConfig.nocConfig));
			createElementsOfNOC();			
			((NOC)interconnect).ConnectNOCElements();
		}
		
		MemorySystem.createLinkBetweenCaches();
		MemorySystem.setCoherenceOfCaches();
		initCoreBroadcastBus();
		GlobalClock.systemTimingSetUp(getCores());
	}
	
	private static void createElementsOfNOC() {
		//create elements mentioned as topology file
		BufferedReader readNocConfig = NOC.openTopologyFile(SystemConfig.nocConfig.NocTopologyFile);
		
		// Skip the first line. It contains numrows/cols information
		try {
			readNocConfig.readLine();
		} catch (IOException e1) {
			misc.Error.showErrorAndExit("Error in reading noc topology file !!");
		}
		
		int numRows = ((NOC)interconnect).getNumRows();
		int numColumns = ((NOC)interconnect).getNumColumns();
		for(int i=0;i<numRows;i++)
		{
			String str = null;
			try {
				str = readNocConfig.readLine();
			} catch (IOException e) {
				misc.Error.showErrorAndExit("Error in reading noc topology file !!");
			}
			
			StringTokenizer st = new StringTokenizer(str," ");
			
			for(int j=0;j<numColumns;j++)
			{
				String nextElementToken = (String)st.nextElement();
				
				System.out.println("NOC [" + i + "][" + j + "] = " + nextElementToken);
				
				CommunicationInterface comInterface = ((NOC)interconnect).getNetworkElements()[i][j];
				
				if(nextElementToken.equals("C")){
					Core core = createCore(cores.size());
					cores.add(core);
					core.setComInterface(comInterface);
				} else if(nextElementToken.equals("D")) {
					Directory directory = MemorySystem.createDirectory();
					coherences.add(directory);
					directory.setComInterface(comInterface);
					//TODO split and multiple directories
				} else if(nextElementToken.equals("M")) {
					MainMemoryController mainMemController = new MainMemoryController();
					memoryControllers.add(mainMemController);
					mainMemController.setComInterface(comInterface);
				} else if(nextElementToken.equals("-")) {
					//do nothing
				} else {
					Cache c = MemorySystem.createSharedCache(nextElementToken);
					sharedCaches.add(c);
					c.setComInterface(comInterface);
					//TODO split and multiple shared caches
				} 
			}
		}
	}	
	
	static Core createCore(int coreNumber) {
		return new Core(coreNumber, 1, 1, null, new int[]{0});
	}
		
	public static InterConnect getInterConnect() {
		return interconnect;
	}
	
	public static void setInterConnect(InterConnect i) {
		interconnect = i;
	}
		
	//TODO read a config file
	//create specified number of cores
	//map threads to cores
	public static Core[] initCores()
	{
		System.out.println("initializing cores...");
		
		Core[] cores = new Core[IpcBase.getEmuThreadsPerJavaThread()];
		for (int i=0; i<IpcBase.getEmuThreadsPerJavaThread(); i++) {
			cores[i] = new Core(i,
							1,
							1,
							null,
							new int[]{0});
		}
		
		GlobalClock.systemTimingSetUp(cores);
		
		//TODO wont work in case of multiple runnable threads
//			for(int i = 0; i<IpcBase.getEmuThreadsPerJavaThread(); i++)
//			{
//				if (SimulationConfig.isPipelineInorder)
//				{
//					((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//				else if (SimulationConfig.isPipelineMultiIssueInorder)
//				{
//					//TODO
//					((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//				else if(SimulationConfig.isPipelineOutOfOrder)
//				{	
//					((OutOrderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//			}
		return cores;
	}

	public static Core getCore(int i) {
		return cores.get(i);
	}
	
	public static Vector<Core> getCoresVector() {
		return cores;
	}
	
	public static Core[] getCores() {
		Core[] coreArray = new Core[cores.size()];
		int i=0;
		for(Core core : cores) {
			coreArray[i] = core;
			i++;
		}
		
		return coreArray;
	}

	public static long getNoOfInstsExecuted()
	{
		long noOfInstsExecuted = 0;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			noOfInstsExecuted += ArchitecturalComponent.getCores()[i].getNoOfInstructionsExecuted();
		}
		return noOfInstsExecuted;
	}

	public static void dumpAllICacheBuffers()
	{
		System.out.println("\n\nICache Buffer DUMP\n\n");
		ICacheBuffer buffer = null;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			buffer = ((OutOrderExecutionEngine)ArchitecturalComponent.getCores()[i].getExecEngine()).getiCacheBuffer();
			System.out.println("---------------------------------------------------------------------------");
			System.out.println("CORE " + i);
			buffer.dump();
		}
	}

	public static void dumpAllEventQueues()
	{
		System.out.println("\n\nEvent Queue DUMP\n\n");
		EventQueue eventQueue = null;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			eventQueue = ArchitecturalComponent.getCores()[i].getEventQueue();
			System.out.println("---------------------------------------------------------------------------");
			System.out.println("CORE " + i);
			eventQueue.dump();
		}
	}

	public static void dumpAllMSHRs()
	{
		CoreMemorySystem coreMemSys = null;
		System.out.println("\n\nMSHR DUMP\n\n");
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			coreMemSys = ArchitecturalComponent.getCores()[i].getExecEngine().getCoreMemorySystem();
			System.out.println("---------------------------------------------------------------------------");
			System.out.println("CORE " + i);
			System.out.println("coreMemSys");
			System.out.println("i - mshr");
			coreMemSys.getiCache().getMissStatusHoldingRegister().dump();
			System.out.println("l1-mshr");
			coreMemSys.getL1Cache().getMissStatusHoldingRegister().dump();
			System.out.println("iCache");
			coreMemSys.getiCache().getMissStatusHoldingRegister().dump();
			System.out.println("L1");
			coreMemSys.getL1Cache().getMissStatusHoldingRegister().dump();
		}
		
		System.out.println("---------------------------------------------------------------------------");
		System.out.println("L2");
		coreMemSys.getiCache().nextLevel.getMissStatusHoldingRegister().dump();
		
	}

	public static void dumpOutStandingLoads()
	{
		/*System.out.println("Outstanding loads on core ");
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			System.out.println( "outstanding loads on core "+i +"  = "+((InorderExecutionEngine)ArchitecturalComponent.getCores()[i].getExecEngine()).noOfOutstandingLoads);
		}*/
	}
	
	
	public static CoreMemorySystem[] getCoreMemSysArray()
	{
		CoreMemorySystem[] toBeReturned = new CoreMemorySystem[coreMemSysArray.size()];
		int i = 0;
		for(CoreMemorySystem c : coreMemSysArray) {
			toBeReturned[i] = c;
			i++;
		}
		return toBeReturned;
	}

	private static ArrayList<Router> nocRouterList = new ArrayList<Router>();
	
	public static void addNOCRouter(Router router) {
		nocRouterList.add(router);		
	}
	
	public static ArrayList<Router> getNOCRouterList() {
		return nocRouterList;
	}

	public static void initCoreBroadcastBus() {
		coreBroadcastBus = new CoreBcastBus();		
	}

	public static MainMemoryController getMainMemoryController(CommunicationInterface comInterface) {
		//TODO : return the nearest memory controller based on the location of the communication interface
		return memoryControllers.get(0);
	}
}
