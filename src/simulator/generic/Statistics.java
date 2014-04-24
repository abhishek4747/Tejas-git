package generic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import net.Router;
import net.Switch;

import main.ArchitecturalComponent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.directory.CentralizedDirectoryCache;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;

import config.BranchPredictorConfig;
import config.CoreConfig;
import config.EmulatorConfig;
import config.Interconnect;
import config.PowerConfigNew;
import config.SimulationConfig;
import config.SystemConfig;
import config.BranchPredictorConfig.BP;
import config.XMLParser;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;

public class Statistics {
	
	
	static FileWriter outputFileWriter,traceWriter;
	
	static String benchmark;
	public static void printSystemConfig()
	{
		//read config.xml and write to output file
		try
		{
			outputFileWriter.write("[Configuration]\n");
			outputFileWriter.write("\n");
			
			if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
				outputFileWriter.write("EmulatorType: Pin\n");
			} else if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
				outputFileWriter.write("EmulatorType: Qemu\n");
			}
			
			
			outputFileWriter.write("Benchmark: "+benchmark+"\n");
			outputFileWriter.write("Schedule: " + (new Date()).toString() + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private static CoreMemorySystem coreMemSys[];
	private static Core cores[];
	
	//Translator Statistics
	
	static long dataRead[];
	static long numHandledCISCInsn[][];
	static long numCISCInsn[][];
	static long noOfMicroOps[][];
	static double staticCoverage;
	static double dynamicCoverage;
	
	public static void printTranslatorStatistics()
	{
		for(int i = 0; i < SystemConfig.maxNumJavaThreads; i++)
		{
			for (int j=0; j<IpcBase.getEmuThreadsPerJavaThread(); j++) {
				if(SimulationConfig.pinpointsSimulation == false)
				{
					totalNumMicroOps += noOfMicroOps[i][j];
				}
//				totalNumMicroOps += numCoreInstructions[i];
				totalHandledCISCInsn += numHandledCISCInsn[i][j];
				totalPINCISCInsn += numCISCInsn[i][j];
			}
		}
		
		dynamicCoverage = ((double)totalHandledCISCInsn/(double)totalPINCISCInsn)*(double)100.0;
		
		if(SimulationConfig.pinpointsSimulation == true)
		{
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				totalNumMicroOps += cores[i].getNoOfInstructionsExecuted();
			}
			totalHandledCISCInsn = 3000000;
		}
		
		//for each java thread, print number of instructions provided by PIN and number of instructions forwarded to the pipeline
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Translator Statistics]\n");
			outputFileWriter.write("\n");
			
			for(int i = 0; i < SystemConfig.maxNumJavaThreads; i++)
			{
				outputFileWriter.write("Java thread\t=\t" + i + "\n");
				outputFileWriter.write("Data Read\t=\t" + dataRead[i] + " bytes\n");
//				outputFileWriter.write("Number of instructions provided by emulator\t=\t" + numHandledCISCInsn[i] + "\n");
//				outputFileWriter.write("Number of Micro-Ops\t=\t" + noOfMicroOps[i] + " \n");
//				outputFileWriter.write("MicroOps/CISC = " + 
//						((double)(numInstructions[i]))/((double)(noOfMicroOps[i])) + "\n");
//				outputFileWriter.write("\n");
			}
			outputFileWriter.write("Number of micro-ops\t\t=\t" + totalNumMicroOps + "\n");
			outputFileWriter.write("Number of handled CISC instructions\t=\t" + totalHandledCISCInsn + "\n");
			outputFileWriter.write("Number of PIN CISC instructions\t=\t" + totalPINCISCInsn + "\n");
			
			outputFileWriter.write("Static coverage\t\t=\t" + formatDouble(staticCoverage) + " %\n");
			outputFileWriter.write("Dynamic Coverage\t=\t" + formatDouble(dynamicCoverage) + " %\n");
			outputFileWriter.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//Timing Statistics	
	static long totalNumMicroOps = 0;
	static long totalHandledCISCInsn = 0;
	static long totalPINCISCInsn = 0;
	public static long maxCoreCycles = 0;
	
		
	public static void printTimingStatistics()
	{
		long coreCyclesTaken[] = new long[SystemConfig.NoOfCores];
		for (int i =0; i < SystemConfig.NoOfCores; i++)
		{
			coreCyclesTaken[i] = cores[i].getCoreCyclesTaken();
			if (maxCoreCycles < coreCyclesTaken[i])
				maxCoreCycles = coreCyclesTaken[i];
		}
	
		
		//for each core, print number of cycles taken by pipeline to reach completion
		
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Timing Statistics]\n");
			outputFileWriter.write("\n");
			outputFileWriter.write("Total Cycles taken\t\t=\t" + maxCoreCycles + "\n\n");
			outputFileWriter.write("Total IPC\t\t=\t" + formatDouble((double)totalNumMicroOps/maxCoreCycles) + "\t\tin terms of micro-ops\n");
			outputFileWriter.write("Total IPC\t\t=\t" + formatDouble((double)totalHandledCISCInsn/maxCoreCycles) + "\t\tin terms of CISC instructions\n\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				if(cores[i].getNoOfInstructionsExecuted()==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				
				CoreConfig coreConfig = SystemConfig.core[i];
				
				outputFileWriter.write("Pipeline: " + coreConfig.pipelineType + "\n");
								
				outputFileWriter.write("instructions executed\t=\t" + cores[i].getNoOfInstructionsExecuted() + "\n");
				outputFileWriter.write("cycles taken\t=\t" + coreCyclesTaken[i] + " cycles\n");
				//FIXME will work only if java thread is 1
				if(SimulationConfig.pinpointsSimulation == false)
				{
					outputFileWriter.write("IPC\t\t=\t" + formatDouble((double)cores[i].getNoOfInstructionsExecuted()/coreCyclesTaken[i]) + "\t\tin terms of micro-ops\n");
					outputFileWriter.write("IPC\t\t=\t" + formatDouble((double)numHandledCISCInsn[0][i]/coreCyclesTaken[i]) + "\t\tin terms of CISC instructions\n");
				}
				else
				{
					outputFileWriter.write("IPC\t\t=\t" + formatDouble((double)cores[i].getNoOfInstructionsExecuted()/coreCyclesTaken[i]) + "\t\tin terms of micro-ops\n");
					outputFileWriter.write("IPC\t\t=\t" + formatDouble((double)3000000/coreCyclesTaken[i]) + "\t\tin terms of CISC instructions\n");
				}
				
				outputFileWriter.write("core frequency\t=\t" + cores[i].getFrequency() + " MHz\n");
				outputFileWriter.write("time taken\t=\t" + formatDouble((double)coreCyclesTaken[i]/cores[i].getFrequency()) + " microseconds\n");
				outputFileWriter.write("\n");
				
				outputFileWriter.write("number of branches\t=\t" + cores[i].getExecEngine().getNumberOfBranches() + "\n");
				outputFileWriter.write("number of mispredicted branches\t=\t" + cores[i].getExecEngine().getNumberOfMispredictedBranches() + "\n");
				outputFileWriter.write("branch predictor accuracy\t=\t" + formatDouble((double)((double)(1.0 - (double)cores[i].getExecEngine().getNumberOfMispredictedBranches()/(double)cores[i].getExecEngine().getNumberOfBranches())*100.0)) + " %\n");
				outputFileWriter.write("\n");
				
				outputFileWriter.write("predictor type = " + coreConfig.branchPredictor.predictorMode + "\n");
				outputFileWriter.write("PC bits = " + coreConfig.branchPredictor.PCBits + "\n");
				outputFileWriter.write("BHR size = " + coreConfig.branchPredictor.BHRsize + "\n");
				outputFileWriter.write("Saturating bits = " + coreConfig.branchPredictor.saturating_bits + "\n");
				outputFileWriter.write("\n");
				
			}
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static void printPowerStatistics()
	{
		try {
			// Cores
			int i = 0;

			PowerConfigNew corePower = new PowerConfigNew(0, 0);
			i = 0;
			for(Core core : cores) {
				corePower.add(core.calculateAndPrintPower(outputFileWriter, "core[" + (i++) + "]"));
			}
			
			outputFileWriter.write("\n\n");
			corePower.printPowerStats(outputFileWriter, "corePower.total");
			
			outputFileWriter.write("\n\n");
			
			// LLC
			PowerConfigNew cachePower = new PowerConfigNew(0, 0);
			for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); )
			{
				String cacheName = cacheNameSet.nextElement();
				Cache cache = MemorySystem.getCacheList().get(cacheName);
				cachePower.add(cache.calculateAndPrintPower(outputFileWriter, cache.toString()));
			}
						
			// Main Memory
			PowerConfigNew mainMemoryPower=null;
			PowerConfigNew[] mainMemoryPowers = null;
			double totalDynamicPower=0;
			int totalAccesses=0;
			outputFileWriter.write("\n\n");
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				mainMemoryPower = MemorySystem.mainMemoryController.calculateAndPrintPower(outputFileWriter, "MainMemoryController");
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				int j=0;
				mainMemoryPowers = new PowerConfigNew[SystemConfig.nocConfig.nocElements.noOfMemoryControllers];
				for(MainMemoryController controller:SystemConfig.nocConfig.nocElements.memoryControllers)
				{
					mainMemoryPowers[j] = controller.calculatePower(outputFileWriter);
					totalDynamicPower += mainMemoryPowers[j].dynamicPower;
					totalAccesses += mainMemoryPowers[j].numAccesses;
					j++;
				}
				outputFileWriter.write("MainMemoryController\t\t" + mainMemoryPowers[0].leakagePower + "\t" + totalDynamicPower 
										+ "\t" + ( mainMemoryPowers[0].leakagePower + totalDynamicPower) + "\t" + totalAccesses);
			}
			outputFileWriter.write("\n\n");
			
			// Directory
			PowerConfigNew directoryPower=null;
			PowerConfigNew[] directoriesPower = null;
			double totalDirectoryDynamicPower=0, totalDirectoryLeakagePower=0;
			int totalDirectoryNumAccesses=0;
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				directoryPower = MemorySystem.getDirectoryCache().calculateAndPrintPower(outputFileWriter, "Directory");
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				int j=0;
				directoriesPower = new PowerConfigNew[SystemConfig.nocConfig.nocElements.noOfL1Directories];
				for(CentralizedDirectoryCache directory:SystemConfig.nocConfig.nocElements.l1Directories)
				{
					directoriesPower[j] = directory.calculateAndPrintPower(outputFileWriter, "Directory Bank " + j);
					totalDirectoryLeakagePower += directoriesPower[j].leakagePower;
					totalDirectoryDynamicPower += directoriesPower[j].dynamicPower;
					totalDirectoryNumAccesses += directoriesPower[j].numAccesses;
					outputFileWriter.write("\n");
					j++;
				}
				outputFileWriter.write("\n");
				outputFileWriter.write("Total Directory Power \t\t" + totalDirectoryLeakagePower + "\t" + totalDirectoryDynamicPower 
						+ "\t" + ( totalDirectoryLeakagePower + totalDirectoryDynamicPower) + "\t" + totalDirectoryNumAccesses);
			}
			outputFileWriter.write("\n\n");
			// NOC
			PowerConfigNew nocRouterPower = new PowerConfigNew(0, 0);
			i = 0;
			for(Router router : ArchitecturalComponent.getNOCRouterList()) {
				nocRouterPower.add(router.calculateAndPrintPower(outputFileWriter, "NOCRouter[" + (i++) + "]"));
			}
			
			outputFileWriter.write("\n\n");
			nocRouterPower.printPowerStats(outputFileWriter, "nocRouter.total");
			
			outputFileWriter.write("\n\n");
						
			// Clock
			//PowerConfigNew clockPower = GlobalClock.calculateAndPrintPower(outputFileWriter, "GlobalClock");
			
			outputFileWriter.write("\n\n");
			
			PowerConfigNew totalPower = new PowerConfigNew(0, 0);
			totalPower.add(corePower);
			totalPower.add(cachePower);
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				totalPower.add(mainMemoryPower);
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				for(PowerConfigNew m:mainMemoryPowers)
					totalPower.add(m);
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				totalPower.add(directoryPower);
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				for(PowerConfigNew p:directoriesPower)
					totalPower.add(p);
			}
			totalPower.add(nocRouterPower);
			//totalPower.add(clockPower);
			
			totalPower.printPowerStats(outputFileWriter, "TotalPower");
			
		} catch (Exception e) {
			System.err.println("error in printing stats + \nexception = " + e);
			e.printStackTrace();
		}
	}
	
	
	//Memory System Statistics
	static long totalNucaBankAccesses;
	
	public static String nocTopology;
	public static String nocRoutingAlgo;
	public static int hopcount=0;
	static long noOfDirHits;
	static long noOfDirMisses;
	static long noOfDirDataForwards;
	static long noOfDirInvalidations;
	static long noOfDirWritebacks;
	static long noOfDirReadMiss;
	static long noOfDirWriteMiss;
	static long noOfDirWriteHits;
	static long numOfDirEntries;
	
	static float averageHopLength;
	static int maxHopLength;
	static int minHopLength;
	
	static long numInsWorkingSetHits[];
	static long numInsWorkingSetMisses[];
	static long maxInsWorkingSetSize[];
	static long minInsWorkingSetSize[];
	static long totalInsWorkingSetSize[];
	static long numInsWorkingSetsNoted[];
	
	static long numDataWorkingSetHits[];
	static long numDataWorkingSetMisses[];
	static long maxDataWorkingSetSize[];
	static long minDataWorkingSetSize[];
	static long totalDataWorkingSetSize[];
	static long numDataWorkingSetsNoted[];
	
	
	public static void printMemorySystemStatistics()
	{
		//for each core, print memory system statistics
		
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Memory System Statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("[Per core statistics]\n");
			outputFileWriter.write("\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				if(cores[i].getCoreCyclesTaken()==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("Memory Requests\t=\t" + coreMemSys[i].getNumberOfMemoryRequests() + "\n");
				outputFileWriter.write("Loads\t\t=\t" + coreMemSys[i].getNumberOfLoads() + "\n");
				outputFileWriter.write("Stores\t\t=\t" + coreMemSys[i].getNumberOfStores() + "\n");
				outputFileWriter.write("LSQ forwardings\t=\t" + coreMemSys[i].getNumberOfValueForwardings() + "\n");
				
				/*printCacheStatistics("iCache[" + i + "]", coreMemSys[i].getiCache().hits, coreMemSys[i].getiCache().misses);
				printCacheStatistics("l1Cache[" + i + "]", coreMemSys[i].getL1Cache().hits, coreMemSys[i].getL1Cache().misses);*/
				
				printCacheStatistics("iTLB[" + i + "]", coreMemSys[i].getiTLB().getTlbHits(), coreMemSys[i].getiTLB().getTlbMisses());
				printCacheStatistics("dTLB[" + i + "]", coreMemSys[i].getdTLB().getTlbHits(), coreMemSys[i].getdTLB().getTlbMisses());
				
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("\n");
			
			outputFileWriter.write("\n\nCache Statistics\n\n");
			
			for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
			{
				String cacheName = cacheNameSet.nextElement();
				Cache cache = MemorySystem.getCacheList().get(cacheName);
				printCacheStatistics(cache);
			}
			
			/*outputFileWriter.write("[Other cache statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("L2 Requests\t=\t" + noOfL2Requests + "\n");
			outputFileWriter.write("L2 Hits\t\t=\t" + noOfL2Hits + "\n");
			outputFileWriter.write("L2 Misses\t=\t" + noOfL2Misses + "\n");
			

			if (noOfL2Requests != 0)
			{
				outputFileWriter.write("L2 Hit-Rate\t=\t" + formatDouble((double)(noOfL2Hits)/noOfL2Requests) + "\n");
				outputFileWriter.write("L2 Miss-Rate\t=\t" + formatDouble((double)(noOfL2Misses)/noOfL2Requests) + "\n");
			}*/
			
			
			
			outputFileWriter.write("NUCA Type\t=\t" + SimulationConfig.nucaType + "\n");
			if (nocRoutingAlgo != null)
			{
				outputFileWriter.write("L2 noc Topology\t=\t" + nocTopology + "\n");
				outputFileWriter.write("L2 noc Routing Algorithm\t=\t" + nocRoutingAlgo + "\n");
			}
			if(SimulationConfig.nucaType!=NucaType.NONE)
			{
				outputFileWriter.write("Total Nuca Bank Accesses\t=\t" + totalNucaBankAccesses +"\n");
				
				/* TODO anuj
				double totalNucaBankPower = (totalNucaBankAccesses*PowerConfig.dcache2Power)/executionTime;
				outputFileWriter.write("Total Nuca Bank Accesses Power\t=\t" + totalNucaBankPower + "\n");
				double totalRouterPower = ((hopcount*(PowerConfig.linkEnergy+PowerConfig.totalRouterEnergy))/executionTime);
				
				
				if (hopcount != 0)
				{
					outputFileWriter.write("Router Hops\t=\t" + hopcount + "\n");
					outputFileWriter.write("Total Router Power\t=\t" + totalRouterPower + "\n");
				}*/
				outputFileWriter.write("Minimum Hop Length\t=\t" + minHopLength + "\n");
				outputFileWriter.write("Maximum Hop Length\t=\t" + maxHopLength + "\n");
				outputFileWriter.write("Average Hop Length\t=\t" + averageHopLength + "\n");
				
				/* TODO anuj
				double totalBufferPower = (Switch.totalBufferAccesses*PowerConfig.bufferEnergy)/executionTime;
				outputFileWriter.write("Total Buffer Accesses\t=\t" + Switch.totalBufferAccesses + "\n");
				if(totalBufferPower!=0)
					outputFileWriter.write("Total Buffer Power\t=\t" + totalBufferPower + "\n");
				outputFileWriter.write("Total NUCA Dynamic Power\t=\t" 
								+ (totalNucaBankPower
								+  totalRouterPower
								+ totalBufferPower) + "\n");
				*/
			}
			if(SystemConfig.interconnect == Interconnect.Bus)
			{
				outputFileWriter.write("Directory Access due to Read-Miss\t=\t" + noOfDirReadMiss + "\n");
				outputFileWriter.write("Directory Access due to Write-Miss\t=\t" + noOfDirWriteMiss + "\n");
				outputFileWriter.write("Directory Access due to Write-Hit\t=\t" + noOfDirWriteHits + "\n");
				outputFileWriter.write("Directory Hits\t=\t" + noOfDirHits + "\n");
				outputFileWriter.write("Directory Misses\t=\t" + noOfDirMisses + "\n");
				outputFileWriter.write("Directory Invalidations\t=\t" + noOfDirInvalidations + "\n");
				outputFileWriter.write("Directory DataForwards\t=\t" + noOfDirDataForwards + "\n");
				outputFileWriter.write("Directory Writebacks\t=\t" + noOfDirWritebacks + "\n");
				outputFileWriter.write("Directory Entries\t=\t" + numOfDirEntries + "\n");
				if (noOfDirHits+noOfDirMisses != 0)
				{
					outputFileWriter.write("Directory Hit-Rate\t=\t" + formatDouble((double)(noOfDirHits)/(noOfDirHits+noOfDirMisses)) + "\n");
					outputFileWriter.write("Directory Miss-Rate\t=\t" + formatDouble((double)(noOfDirMisses)/(noOfDirHits+noOfDirMisses)) + "\n");
				
				}
			}
			else if(SystemConfig.interconnect == Interconnect.Noc)
			{
				outputFileWriter.write("\n\n");
				int j=0;
				int totalDirHits=0;
				int totalDirMisses=0;
				for(CentralizedDirectoryCache directory : SystemConfig.nocConfig.nocElements.l1Directories)
				{
					outputFileWriter.write("Directory Bank " + j + "\n");
					outputFileWriter.write("Directory Access due to Read-Miss\t=\t" + directory.getNumReadMiss() + "\n");
					outputFileWriter.write("Directory Access due to Write-Miss\t=\t" + directory.getNumWriteMiss() + "\n");
					outputFileWriter.write("Directory Access due to Write-Hit\t=\t" + directory.getNumWriteHit() + "\n");
					outputFileWriter.write("Directory Hits\t=\t" + directory.getDirectoryHits() + "\n");
					outputFileWriter.write("Directory Misses\t=\t" + directory.getDirectoryMisses() + "\n");
					outputFileWriter.write("Directory Invalidations\t=\t" + directory.getInvalidations() + "\n");
					outputFileWriter.write("Directory DataForwards\t=\t" + directory.getDataForwards() + "\n");
					outputFileWriter.write("Directory Writebacks\t=\t" + directory.getWritebacks() + "\n");
					outputFileWriter.write("Directory Entries\t=\t" + directory.getNumberOfDirectoryEntries() + "\n");
					if (noOfDirHits+noOfDirMisses != 0)
					{
						outputFileWriter.write("Directory Hit-Rate\t=\t" + formatDouble((double)(directory.getDirectoryHits())/(directory.getDirectoryHits()
																+directory.getDirectoryMisses())) + "\n");
						outputFileWriter.write("Directory Miss-Rate\t=\t" + formatDouble((double)(directory.getDirectoryMisses())/(directory.getDirectoryHits()
																+directory.getDirectoryMisses())) + "\n");
					
					}
					totalDirHits += directory.getDirectoryHits();
					totalDirMisses += directory.getDirectoryMisses();
					j++;
					outputFileWriter.write("\n\n");
				}
				outputFileWriter.write("Total Directory Hits\t=\t" + totalDirHits + "\n");
				outputFileWriter.write("Total Directory Misses\t=\t" + totalDirMisses + "\n");
				outputFileWriter.write("Total Directory Hit-Rate\t=\t" + formatDouble((double)(totalDirHits)/(totalDirHits+totalDirMisses)) + "\n");
				outputFileWriter.write("Total Directory Miss-Rate\t=\t" + formatDouble((double)(totalDirMisses)/(totalDirHits+totalDirMisses)) + "\n");

			}
				
			outputFileWriter.write("\n\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static void printInsWorkingSetStats() throws IOException {
		long insMaxWorkingSet = Long.MIN_VALUE; 
		long insMinWorkingSet = Long.MAX_VALUE;
		long insTotalWorkingSet = 0, insNumWorkingSetNoted = 0;
		long insWorkingSetHits = 0, insWorkingSetMisses = 0;
		
		outputFileWriter.write("\n\nPer Core Ins Working Set Stats : \n");
		for(int i=0; i<SystemConfig.NoOfCores; i++) {
			if(numInsWorkingSetHits[i]==0 && numInsWorkingSetMisses[i]==0) {
				continue;
			} else {
				// Min, Avg, Max working set of the core
				// Hitrate for the working set
				outputFileWriter.write("\nMinInsWorkingSet[" + i + "]\t=\t" + 
						minInsWorkingSetSize[i]);
				
				if(minInsWorkingSetSize[i]<insMinWorkingSet) {
					insMinWorkingSet = minInsWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nAvgInsWorkingSet[" + i + "]\t=\t" + 
						(float)totalInsWorkingSetSize[i]/(float)numInsWorkingSetsNoted[i]);
				
				insTotalWorkingSet += totalInsWorkingSetSize[i];
				insNumWorkingSetNoted += numInsWorkingSetsNoted[i];
				
				outputFileWriter.write("\nMaxInsWorkingSet[" + i + "]\t=\t" + 
						maxInsWorkingSetSize[i]);
				
				if(maxInsWorkingSetSize[i]>insMaxWorkingSet) {
					insMaxWorkingSet = maxInsWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nInsWorkingSetHitrate[" + i + "]\t=\t" + 
						(float)numInsWorkingSetHits[i]/(float)(numInsWorkingSetHits[i] + numInsWorkingSetMisses[i]));
				
				insWorkingSetHits += numInsWorkingSetHits[i];
				insWorkingSetMisses += numInsWorkingSetMisses[i];
			}
			outputFileWriter.write("\n");
		}
		
		outputFileWriter.write("\n\nTotal Ins Working Set Stats : \n");
		outputFileWriter.write("\nMinInsWorkingSet\t=\t" + insMinWorkingSet);
		outputFileWriter.write("\nAvgInsWorkingSet\t=\t" + formatDouble((float)insTotalWorkingSet/(float)insNumWorkingSetNoted));
		outputFileWriter.write("\nMaxInsWorkingSet\t=\t" + insMaxWorkingSet);
		
		float hitrate = (float)insWorkingSetHits/(float)(insWorkingSetHits+insWorkingSetMisses);
		outputFileWriter.write("\nInsWorkingSetHitrate\t=\t" + formatDouble(hitrate));
		
		outputFileWriter.write("\n\n");
	}
	
	
	static void printDataWorkingSetStats() throws IOException {
		long dataMaxWorkingSet = Long.MIN_VALUE; 
		long dataMinWorkingSet = Long.MAX_VALUE;
		long dataTotalWorkingSet = 0, dataNumWorkingSetNoted = 0;
		long dataWorkingSetHits = 0, dataWorkingSetMisses = 0;
		
		outputFileWriter.write("\n\nPer Core Data Working Set Stats : \n");
		for(int i=0; i<SystemConfig.NoOfCores; i++) {
			if(numDataWorkingSetHits[i]==0 && numDataWorkingSetMisses[i]==0) {
				continue;
			} else {
				// Min, Avg, Max working set of the core
				// Hitrate for the working set
				outputFileWriter.write("\nMinDataWorkingSet[" + i + "]\t=\t" + 
						minDataWorkingSetSize[i]);
				
				if(minDataWorkingSetSize[i]<dataMinWorkingSet) {
					dataMinWorkingSet = minDataWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nAvgDataWorkingSet[" + i + "]\t=\t" + 
						formatDouble((float)totalDataWorkingSetSize[i]/(float)numDataWorkingSetsNoted[i]));
				
				dataTotalWorkingSet += totalDataWorkingSetSize[i];
				dataNumWorkingSetNoted += numDataWorkingSetsNoted[i];
				
				outputFileWriter.write("\nMaxDataWorkingSet[" + i + "]\t=\t" + 
						maxDataWorkingSetSize[i]);
				
				if(maxDataWorkingSetSize[i]>dataMaxWorkingSet) {
					dataMaxWorkingSet = maxDataWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nDataWorkingSetHitrate[" + i + "]\t=\t" + 
						formatDouble((float)numDataWorkingSetHits[i]/(float)(numDataWorkingSetHits[i] + numDataWorkingSetMisses[i])));
				
				dataWorkingSetHits += numDataWorkingSetHits[i];
				dataWorkingSetMisses += numDataWorkingSetMisses[i];
			}
			outputFileWriter.write("\n");
		}
		
		outputFileWriter.write("\n\nTotal Data Working Set Stats : \n");
		outputFileWriter.write("\nMinDataWorkingSet\t=\t" + dataMinWorkingSet);
		outputFileWriter.write("\nAvgDataWorkingSet\t=\t" + (float)dataTotalWorkingSet/(float)dataNumWorkingSetNoted);
		outputFileWriter.write("\nMaxDataWorkingSet\t=\t" + dataMaxWorkingSet);
		
		float hitrate = (float)dataWorkingSetHits/(float)(dataWorkingSetHits+dataWorkingSetMisses);
		outputFileWriter.write("\nDataWorkingSetHitrate\t=\t" + formatDouble(hitrate));
		
		outputFileWriter.write("\n\n");
	}
	
	static void printCacheStatistics(String cacheStr,
			long hits, long misses) throws IOException
	{
		outputFileWriter.write("\n\n" + cacheStr + " Hits\t=\t" + hits);
		outputFileWriter.write("\n" + cacheStr + " Misses\t=\t" + misses);
		outputFileWriter.write("\n" + cacheStr + " Hit-Rate\t=\t" + formatDouble((double)hits/(double)(hits+misses)));
		outputFileWriter.write("\n" + cacheStr + " Miss-Rate\t=\t" + formatDouble((double)misses/(double)(hits+misses)));
	}
	
	static void printCacheStatistics(Cache cache) throws IOException
	{
		outputFileWriter.write("\n\nCache : ");
		/*if(cache.getName() != null)
		{
			outputFileWriter.write(cache.getName());
		}
		if(cache.getContainingMemSys().length == 1)
		{
			outputFileWriter("\nPrivate to core " + cache.getContainingMemSys()[0].getCore().getCore_number());
		}
		else
		{
			outputFileWriter("\nShared between cores ");
			for(int i = 0; i < cache.getContainingMemSys().length; i++)
			{
				outputFileWriter.write(cache.getContainingMemSys()[i].getCore().getCore_number() + " ");
			}
		}*/
		outputFileWriter.write("\nRequests\t=\t" + cache.hits+cache.misses);
		outputFileWriter.write("\nHits\t=\t" + cache.hits);
		outputFileWriter.write("\nMisses\t=\t" + cache.misses);
		outputFileWriter.write("\nHit-Rate\t=\t" + formatDouble((double)cache.hits/(double)(cache.hits+cache.misses)));
		outputFileWriter.write("\nMiss-Rate\t=\t" + formatDouble((double)cache.misses/(double)(cache.hits+cache.misses)));
	}
	
	//Simulation time
	//static long time;
	//static long subsetTime;
	private static long simulationTime;

	public static void printPowerTraceHeader(String delimiter){
		try {
			for (int i =0; i < SystemConfig.NoOfCores; i++)
			{

			traceWriter.write("Core"+delimiter);

			traceWriter.write("Simple"+delimiter);
			
			traceWriter.write("Total"+delimiter);
			
			traceWriter.write("AggresiveIdeal"+delimiter);

			traceWriter.write("Total"+delimiter);
			
			traceWriter.write("AggresiveNonIdeal"+delimiter);
			
			traceWriter.write("Total"+delimiter);
		}
			traceWriter.write("\n\n");


		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}


	public static void printSimulationTime()
	{
		//print time taken by simulator
		long seconds = simulationTime/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Simulator Time]\n");
			
			outputFileWriter.write("Time Taken\t\t=\t" + minutes + " : " + seconds + " minutes\n");
			
			outputFileWriter.write("Instructions per Second\t=\t" + 
					formatDouble((double)totalNumMicroOps/simulationTime) + " KIPS\t\tin terms of micro-ops\n");
			outputFileWriter.write("Instructions per Second\t=\t" + 
					formatDouble((double)totalHandledCISCInsn/simulationTime) + " KIPS\t\tin terms of CISC instructions\n");
			
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void initStatistics()
	{		
		dataRead = new long[SystemConfig.maxNumJavaThreads];
		numHandledCISCInsn = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
		numCISCInsn = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
		noOfMicroOps = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
		
		if(SimulationConfig.collectInsnWorkingSetInfo==true) {
			numInsWorkingSetHits = new long[SystemConfig.NoOfCores];
			numInsWorkingSetMisses = new long[SystemConfig.NoOfCores];
			maxInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			minInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			totalInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			numInsWorkingSetsNoted = new long[SystemConfig.NoOfCores];
		}
		
		if(SimulationConfig.collectDataWorkingSetInfo==true) {
			numDataWorkingSetHits = new long[SystemConfig.NoOfCores];
			numDataWorkingSetMisses = new long[SystemConfig.NoOfCores];
			maxDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			minDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			totalDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			numDataWorkingSetsNoted = new long[SystemConfig.NoOfCores];
		}
		
		if(SimulationConfig.pinpointsSimulation == true)
		{		
			
		}
		
	}	
	public static void openTraceStream()
	{
		try
		{
			traceWriter = new FileWriter(SimulationConfig.outputFileName+"Trace.csv");
		}
		catch (IOException e)
		{
			misc.Error.showErrorAndExit("error in opening trace file !!");
		}
	}	
	public static void openStream()
	{
		if(SimulationConfig.outputFileName == null)
		{
			SimulationConfig.outputFileName = "default";
		}
		
		try {
			File outputFile = new File(SimulationConfig.outputFileName);
			
			if(outputFile.exists()) {
				
				// rename the previous output file
				Date lastModifiedDate = new Date(outputFile.lastModified());
				File backupFile = new File(SimulationConfig.outputFileName + "_" + lastModifiedDate.toString());
				if(!outputFile.renameTo(backupFile)) {
					System.err.println("error in creating a backup of your previous output file !!\n");
				}
				
				// again point to the new file
				outputFile = new File(SimulationConfig.outputFileName);
			}
			
			outputFileWriter = new FileWriter(outputFile);
			
			
		} catch (IOException e) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("DEFAULT_");
		    Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			sb.append(sdf.format(cal.getTime()));
			try
			{
				outputFileWriter = new FileWriter(sb.toString());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			System.out.println("unable to create specified output file");
			System.out.println("statistics written to " + sb.toString());
		}
	}
	
	public static void closeStream()
	{
		try
		{
			outputFileWriter.close();
			traceWriter.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void setDataRead(long dataRead, int thread) {
		Statistics.dataRead[thread] = dataRead;
	}

	public static long getNumHandledCISCInsn(int javaThread, int emuThread) {
		return Statistics.numHandledCISCInsn[javaThread][emuThread];
	}

	public static void setNumHandledCISCInsn(long numInstructions, int javaThread, int emuThread) {
		Statistics.numHandledCISCInsn[javaThread][emuThread] = numInstructions;
		PinPointsProcessing.toProcessEndOfSlice(numHandledCISCInsn[javaThread][emuThread]);
	}
	
	public static void setNumCISCInsn(long numInstructions, int javaThread, int emuThread) {
		Statistics.numCISCInsn[javaThread][emuThread] = numInstructions;
	}

	public static void setNoOfMicroOps(long noOfMicroOps[], int thread) {
		Statistics.noOfMicroOps[thread] = noOfMicroOps;
	}
	
	public static void setStaticCoverage(double staticCoverage) {
		Statistics.staticCoverage = staticCoverage;
	}
	
	// Ins working set
	public static void setMaxInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.maxInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setMinInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.minInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setTotalInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.totalInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setNumInsWorkingSetNoted(long workingSetNoted, int core) {
		Statistics.numInsWorkingSetsNoted[core] = workingSetNoted;
	}
	
	public static void setNumInsWorkingSetHits(long workingSetHits, int core) {
		Statistics.numInsWorkingSetHits[core] = workingSetHits;
	}
	
	public static void setNumInsWorkingSetMisses(long workingSetMisses, int core) {
		Statistics.numInsWorkingSetMisses[core] = workingSetMisses;
	}
	
	
	// Data working set
	public static void setMaxDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.maxDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setMinDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.minDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setTotalDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.totalDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setNumDataWorkingSetNoted(long workingSetNoted, int core) {
		Statistics.numDataWorkingSetsNoted[core] = workingSetNoted;
	}
	
	public static void setNumDataWorkingSetHits(long workingSetHits, int core) {
		Statistics.numDataWorkingSetHits[core] = workingSetHits;
	}
	
	public static void setNumDataWorkingSetMisses(long workingSetMisses, int core) {
		Statistics.numDataWorkingSetMisses[core] = workingSetMisses;
	}
	
	public static void setSimulationTime(long simulationTime) {
		Statistics.simulationTime = simulationTime;
	}

	public static void setExecutable(String executableFile) {
		Statistics.benchmark = executableFile;
	}
	public static long getNoOfDirHits() {
		return noOfDirHits;
	}
	public static void setNoOfDirHits(long noOfDirHits) {
		Statistics.noOfDirHits = noOfDirHits;
	}
	public static long getNoOfDirMisses() {
		return noOfDirMisses;
	}
	public static void setNoOfDirMisses(long noOfDirMisses) {
		Statistics.noOfDirMisses = noOfDirMisses;
	}
	public static long getNoOfDirDataForwards() {
		return noOfDirDataForwards;
	}
	public static void setNoOfDirDataForwards(long noOfDirDataForwards) {
		Statistics.noOfDirDataForwards = noOfDirDataForwards;
	}
	public static long getNoOfDirInvalidations() {
		return noOfDirInvalidations;
	}
	public static void setNoOfDirInvalidations(long noOfDirInvalidations) {
		Statistics.noOfDirInvalidations = noOfDirInvalidations;
	}

	public static void setNoOfDirReadMiss(long noOfDirInvalidations) {
		Statistics.noOfDirReadMiss = noOfDirInvalidations;
	}
	public static void setNoOfDirWriteMiss(long noOfDirInvalidations) {
		Statistics.noOfDirWriteMiss = noOfDirInvalidations;
	}
	public static void setNoOfDirWriteHits(long noOfDirInvalidations) {
		Statistics.noOfDirWriteHits = noOfDirInvalidations;
	}
	
	public static void setNoOfDirEntries(long dirEntries)
	{
		Statistics.numOfDirEntries = dirEntries;
		
	}
	public static long getNoOfDirWritebacks() {
		return noOfDirWritebacks;
	}
	
	public static void setNoOfDirWritebacks(long noOfDirWritebacks) {
		Statistics.noOfDirWritebacks = noOfDirWritebacks;
	}

	
	
	public static void printAllStatistics(String benchmarkName, 
			long startTime, long endTime) {
		//set up statistics module
		//Statistics.initStatistics();
		// Statistics.initStatistics();
		cores = ArchitecturalComponent.getCores();
		coreMemSys = ArchitecturalComponent.getCoreMemSysArray();

		Statistics.setExecutable(benchmarkName);
		if(SystemConfig.interconnect == Interconnect.Bus)
		{
			Statistics.setNoOfDirHits(MemorySystem.getDirectoryCache().getDirectoryHits());
			Statistics.setNoOfDirMisses(MemorySystem.getDirectoryCache().getDirectoryMisses());
			Statistics.setNoOfDirInvalidations(MemorySystem.getDirectoryCache().getInvalidations());
			Statistics.setNoOfDirDataForwards(MemorySystem.getDirectoryCache().getDataForwards());
			Statistics.setNoOfDirReadMiss(MemorySystem.getDirectoryCache().getNumReadMiss());
			Statistics.setNoOfDirWriteMiss(MemorySystem.getDirectoryCache().getNumWriteMiss());
			Statistics.setNoOfDirWriteHits(MemorySystem.getDirectoryCache().getNumWriteHit());
			Statistics.setNoOfDirEntries(MemorySystem.getDirectoryCache().getNumberOfDirectoryEntries());
		}
		//set memory statistics for levels L2 and below
		for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cache = MemorySystem.getCacheList().get(cacheName);
			if(cache.getClass()!=Cache.class)
			{
				if (((NucaCache)cache).nucaType != NucaType.NONE )
				{
					averageHopLength = ((NucaCache)cache).getAverageHoplength(); 
					maxHopLength = ((NucaCache)cache).getMaxHopLength();
					minHopLength = ((NucaCache)cache).getMinHopLength();
					Statistics.nocTopology = ((NucaCache)cache).cacheBank.get(0).getRouter().topology.name();
					Statistics.nocRoutingAlgo = ((NucaCache)cache).cacheBank.get(0).getRouter().rAlgo.name();
					for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
					{
						Statistics.hopcount += ((NucaCache)cache).cacheBank.get(i).getRouter().hopCounters; 
					}
					if(Statistics.nocTopology.equals("FATTREE") ||
							Statistics.nocTopology.equals("OMEGA") ||
							Statistics.nocTopology.equals("BUTTERFLY")) {
						for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
							Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
						}
					}
				}
				Statistics.totalNucaBankAccesses = ((NucaCache)cache).getTotalNucaBankAcesses();
			}
			
		}
			
		Statistics.setSimulationTime(endTime - startTime);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		
		try {
			if(SimulationConfig.collectInsnWorkingSetInfo) {
				Statistics.printInsWorkingSetStats();
			}
			
			if(SimulationConfig.collectDataWorkingSetInfo) {
				Statistics.printDataWorkingSetStats();
			}
		} catch (IOException e) {
			
		}
		
		Statistics.printSimulationTime();
		Statistics.printPowerStatistics();
		
		// Qemu translation cache stats
		if(TranslatedInstructionCache.getHitRate()!=-1) {
			try {
				outputFileWriter.write("[Qemu translation cache]\n");
				outputFileWriter.write("Hit-rate = " + (TranslatedInstructionCache.getHitRate() * 100 ) + " %");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		Statistics.closeStream();
	}
	public static long getNumCISCInsn(int javaTid, int tidEmu) {
		return numCISCInsn[javaTid][tidEmu];
	}
	
	public static String formatFloat(float f)
	{
		return String.format("%.4f", f);
	}
	
	public static String formatDouble(double d)
	{
		return String.format("%.4f", d);
	}
	
}