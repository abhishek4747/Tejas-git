package main;

import net.optical.TopLevelTokenBus;
import emulatorinterface.communication.IpcBase;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import pipeline.outoforder.ICacheBuffer;
import pipeline.outoforder.OutOrderExecutionEngine;
import generic.Core;
import generic.CoreBcastBus;
import generic.EventQueue;
import generic.GlobalClock;

public class ArchitecturalComponent {

	private static Core[] cores;
	private static TopLevelTokenBus tokenBus;
	private static CoreBcastBus coreBcastBus;
	
	public static TopLevelTokenBus initTokenBus() 
	{
		return new TopLevelTokenBus();
	}
	
	//TODO read a config file
	//create specified number of cores
	//map threads to cores
	public static Core[] initCores(CoreBcastBus coreBBus)
	{
		System.out.println("initializing cores...");
		System.out.println("Initializing core broadcast bus...");
		
		Core[] cores = new Core[IpcBase.getEmuThreadsPerJavaThread()];
		for (int i=0; i<IpcBase.getEmuThreadsPerJavaThread(); i++) {
			cores[i] = new Core(i,
							1,
							1,
							null,
							new int[]{0});
			cores[i].setCoreBcastBus(coreBBus);
		}
		
		coreBBus.setEventQueue(cores[0].eventQueue);
		GlobalClock.systemTimingSetUp(cores);
		for(int i=0 ; i < cores.length ; i++){
			coreBBus.addToCoreList(cores[i]);
		}
		
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

	public static Core[] getCores() {
		return cores;
	}

	public static void setCores(Core[] cores) {
		ArchitecturalComponent.cores = cores;
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
	
	public static TopLevelTokenBus getTokenBus() {
		return tokenBus;
	}

	public static void setTokenBus(TopLevelTokenBus inTokenBus) {
		tokenBus = inTokenBus;
	}
	
	private static CoreMemorySystem coreMemSysArray[];
	public static CoreMemorySystem[] getCoreMemSysArray()
	{
		return coreMemSysArray;
	}

	public static void initMemorySystem(Core[] cores2,
			TopLevelTokenBus tokenBus2) {
		
		 //TODO mem sys need not know eventQ during initialisation
		coreMemSysArray = MemorySystem.initializeMemSys(ArchitecturalComponent.getCores(),
				ArchitecturalComponent.getTokenBus());		
	}

	public static CoreBcastBus initCoreBcastBus() {
		return new CoreBcastBus();
	}

	public static CoreBcastBus getCoreBcastBus() {
		return coreBcastBus;
	}

	public static void setCoreBcastBus(CoreBcastBus coreBcastBus) {
		ArchitecturalComponent.coreBcastBus = coreBcastBus;
	}
}
