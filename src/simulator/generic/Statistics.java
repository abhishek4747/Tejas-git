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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import net.Switch;

import main.ArchitecturalComponent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;

import power.Counters;
import power.PowerConfig;
import config.BranchPredictorConfig;
import config.EmulatorConfig;
import config.SimulationConfig;
import config.SystemConfig;
import config.BranchPredictorConfig.BP;
import config.XMLParser;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;

public class Statistics {
	
	
	static FileWriter outputFileWriter,traceWriter;
	
	static float[] weightsArray;
	static int currentSlice;
	
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
			outputFileWriter.write("Pipeline: ");
			if (SimulationConfig.isPipelineInorder)
				outputFileWriter.write("Inorder Pipeline\n");
			else if (SimulationConfig.isPipelineStatistical)
				outputFileWriter.write("Statistical Pipeline\n");
			else outputFileWriter.write("OutOrder Pipeline\n");
			
			outputFileWriter.write("Schedule: " + (new Date()).toString() + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private static CoreMemorySystem coreMemSys[];
	private static Core cores[];
	public static void setCoreMemorySystem(CoreMemorySystem coreMemSys[])
	{
		Statistics.coreMemSys = coreMemSys;
	}
	
	//Translator Statistics
	
	static long dataRead[];
	static long numHandledCISCInsn[][];
	static long numCISCInsn[][];
	static long noOfMicroOps[][];
	static double staticCoverage;
	static double dynamicCoverage;
	
	public static void printTranslatorStatistics()
	{
		for(int i = 0; i < IpcBase.MaxNumJavaThreads; i++)
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
				totalNumMicroOps += numCoreInstructions[i];
			}
			totalHandledCISCInsn = 3000000;
		}
		
		//for each java thread, print number of instructions provided by PIN and number of instructions forwarded to the pipeline
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Translator Statistics]\n");
			outputFileWriter.write("\n");
			
			for(int i = 0; i < IpcBase.MaxNumJavaThreads; i++)
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
			
			outputFileWriter.write("Static coverage\t\t=\t" + staticCoverage + " %\n");
			outputFileWriter.write("Dynamic Coverage\t=\t" + dynamicCoverage + " %\n");
			outputFileWriter.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//Timing Statistics
	
	static long coreCyclesTaken[];
	static long coreFrequencies[];				//in MHz
	static long numCoreInstructions[];
	static long branchCount[];
	static long mispredictedBranchCount[];
	static long totalNumMicroOps = 0;
	static long totalHandledCISCInsn = 0;
	static long totalPINCISCInsn = 0;
	static Counters powerCounters[];
	public static long maxCoreCycles = 0;
	
	//for pinpoints
	static long tempcoreCyclesTaken[];
	static long tempnumCoreInstructions[];
	static long tempbranchCount[];
	static long tempmispredictedBranchCount[];
	
	public static void printTimingStatistics()
	{
		for (int i =0; i < SystemConfig.NoOfCores; i++)
		{
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
			outputFileWriter.write("Total IPC\t\t=\t" + (double)totalNumMicroOps/maxCoreCycles + "\t\tin terms of micro-ops\n");
			outputFileWriter.write("Total IPC\t\t=\t" + (double)totalHandledCISCInsn/maxCoreCycles + "\t\tin terms of CISC instructions\n\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				if(numCoreInstructions[i]==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("instructions executed\t=\t" + numCoreInstructions[i] + "\n");
				outputFileWriter.write("cycles taken\t=\t" + coreCyclesTaken[i] + " cycles\n");
				//FIXME will work only if java thread is 1
				if(SimulationConfig.pinpointsSimulation == false)
				{
					outputFileWriter.write("IPC\t\t=\t" + (double)noOfMicroOps[0][i]/coreCyclesTaken[i] + "\t\tin terms of micro-ops\n");
					outputFileWriter.write("IPC\t\t=\t" + (double)numHandledCISCInsn[0][i]/coreCyclesTaken[i] + "\t\tin terms of CISC instructions\n");
				}
				else
				{
					outputFileWriter.write("IPC\t\t=\t" + (double)numCoreInstructions[i]/coreCyclesTaken[i] + "\t\tin terms of micro-ops\n");
					outputFileWriter.write("IPC\t\t=\t" + (double)3000000/coreCyclesTaken[i] + "\t\tin terms of CISC instructions\n");
				}
				
				outputFileWriter.write("core frequency\t=\t" + coreFrequencies[i] + " MHz\n");
				outputFileWriter.write("time taken\t=\t" + (double)coreCyclesTaken[i]/coreFrequencies[i] + " microseconds\n");
				outputFileWriter.write("\n");
				
				outputFileWriter.write("number of branches\t=\t" + branchCount[i] + "\n");
				outputFileWriter.write("number of mispredicted branches\t=\t" + mispredictedBranchCount[i] + "\n");
				outputFileWriter.write("branch predictor accuracy\t=\t" + (double)((double)(branchCount[i]-mispredictedBranchCount[i])*100/branchCount[i]) + " %\n");
				outputFileWriter.write("\n");
				
				outputFileWriter.write("predictor type = " + SystemConfig.branchPredictor.predictorMode + "\n");
				outputFileWriter.write("PC bits = " + SystemConfig.branchPredictor.PCBits + "\n");
				outputFileWriter.write("BHR size = " + SystemConfig.branchPredictor.BHRsize + "\n");
				outputFileWriter.write("Saturating bits = " + SystemConfig.branchPredictor.saturating_bits + "\n");
				outputFileWriter.write("\n");
				
			}
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	void printPowerStatistics() throws IOException
	{
		// Cores
		double corePower = 0;
		for(Core core : cores) {
			corePower += core.calculateAndPrintPower(outputFileWriter, "core" + core.toString());
		}
		
		// LLC
		//MemorySystem.
		
		// Memory
		
		// Directory
		
		// NOC
		
		// Clock
		
	}
	
	
	//Memory System Statistics
	static long noOfMemRequests[];
	static long noOfLoads[];
	static long noOfStores[];
	static long noOfValueForwards[];
	static long noOfTLBRequests[];
	static long noOfTLBHits[];
	static long noOfTLBMisses[];
	static long noOfL1Requests[];
	static long noOfL1Hits[];
	static long noOfL1Misses[];
	static long noOfL2Requests;
	static long noOfL2Hits;
	static long noOfL2Misses;
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
	//for pinpoints
	static long tempnoOfMemRequests[];
	static long tempnoOfLoads[];
	static long tempnoOfStores[];
	static long tempnoOfValueForwards[];
	static long tempnoOfTLBRequests[];
	static long tempnoOfTLBHits[];
	static long tempnoOfTLBMisses[];
	static long tempnoOfL1Requests[];
	static long tempnoOfL1Hits[];
	static long tempnoOfL1Misses[];
	static long tempnoOfL2Requests;
	static long tempnoOfL2Hits;
	static long tempnoOfL2Misses;
	static long tempnoOfIRequests[];
	static long tempnoOfIHits[];
	static long tempnoOfIMisses[];
	static long tempnoOfDirHits;
	static long tempnoOfDirMisses;
	static long tempnoOfDirDataForwards;
	static long tempnoOfDirInvalidations;
	static long tempnoOfDirWritebacks;
	static float averageHopLength;
	static int maxHopLength;
	static int minHopLength;
	
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
				if(coreCyclesTaken[i]==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("Memory Requests\t=\t" + noOfMemRequests[i] + "\n");
				outputFileWriter.write("Loads\t\t=\t" + noOfLoads[i] + "\n");
				outputFileWriter.write("Stores\t\t=\t" + noOfStores[i] + "\n");
				outputFileWriter.write("LSQ forwardings\t=\t" + noOfValueForwards[i] + "\n");
				
				printCacheStatistics("iCache[" + i + "]", coreMemSys[i].getiCache().hits, coreMemSys[i].getiCache().misses);
				printCacheStatistics("l1Cache[" + i + "]", coreMemSys[i].getL1Cache().hits, coreMemSys[i].getL1Cache().misses);
				
				printCacheStatistics("iTLB[" + i + "]", coreMemSys[i].getiTLB().getTlbHits(), coreMemSys[i].getiTLB().getTlbMisses());
				printCacheStatistics("dTLB[" + i + "]", coreMemSys[i].getdTLB().getTlbHits(), coreMemSys[i].getdTLB().getTlbMisses());
				
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("\n");
			
			outputFileWriter.write("[Other cache statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("L2 Requests\t=\t" + noOfL2Requests + "\n");
			outputFileWriter.write("L2 Hits\t\t=\t" + noOfL2Hits + "\n");
			outputFileWriter.write("L2 Misses\t=\t" + noOfL2Misses + "\n");
			

			if (noOfL2Requests != 0)
			{
				outputFileWriter.write("L2 Hit-Rate\t=\t" + (float)(noOfL2Hits)/noOfL2Requests + "\n");
				outputFileWriter.write("L2 Miss-Rate\t=\t" + (float)(noOfL2Misses)/noOfL2Requests + "\n");
			}
			
			printCacheStatistics(MemorySystem.getL2Cache(), hits, misses)
			
			outputFileWriter.write("NUCA Type\t=\t" + SimulationConfig.nucaType + "\n");
			if (nocRoutingAlgo != null)
			{
				outputFileWriter.write("L2 noc Topology\t=\t" + nocTopology + "\n");
				outputFileWriter.write("L2 noc Routing Algorithm\t=\t" + nocRoutingAlgo + "\n");
			}
			if(SimulationConfig.nucaType!=NucaType.NONE)
			{
				outputFileWriter.write("Total Nuca Bank Accesses\t=\t" + totalNucaBankAccesses +"\n");
				long maxCoreCycles = 0;
				for (int i =0; i < SystemConfig.NoOfCores; i++)
				{
					if (maxCoreCycles < coreCyclesTaken[i])
						maxCoreCycles = coreCyclesTaken[i];
				}
				double executionTime = ((double)maxCoreCycles/coreFrequencies[0])*1000;
				
				double totalNucaBankPower = (totalNucaBankAccesses*PowerConfig.dcache2Power)/executionTime;
				outputFileWriter.write("Total Nuca Bank Accesses Power\t=\t" + totalNucaBankPower + "\n");
				double totalRouterPower = ((hopcount*(PowerConfig.linkEnergy+PowerConfig.totalRouterEnergy))/executionTime);
				
				
				if (hopcount != 0)
				{
					outputFileWriter.write("Router Hops\t=\t" + hopcount + "\n");
					outputFileWriter.write("Total Router Power\t=\t" + totalRouterPower + "\n");
				}
				outputFileWriter.write("Minimum Hop Length\t=\t" + minHopLength + "\n");
				outputFileWriter.write("Maximum Hop Length\t=\t" + maxHopLength + "\n");
				outputFileWriter.write("Average Hop Length\t=\t" + averageHopLength + "\n");
				
				double totalBufferPower = (Switch.totalBufferAccesses*PowerConfig.bufferEnergy)/executionTime;
				outputFileWriter.write("Total Buffer Accesses\t=\t" + Switch.totalBufferAccesses + "\n");
				if(totalBufferPower!=0)
					outputFileWriter.write("Total Buffer Power\t=\t" + totalBufferPower + "\n");
				outputFileWriter.write("Total NUCA Dynamic Power\t=\t" 
								+ (totalNucaBankPower
								+  totalRouterPower
								+ totalBufferPower) + "\n");
			}
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
				outputFileWriter.write("Directory Hit-Rate\t=\t" + (float)(noOfDirHits)/(noOfDirHits+noOfDirMisses) + "\n");
				outputFileWriter.write("Directory Miss-Rate\t=\t" + (float)(noOfDirMisses)/(noOfDirHits+noOfDirMisses) + "\n");
			
			}
			outputFileWriter.write("\n");
			
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static void printCacheStatistics(String cacheStr,
			long hits, long misses) throws IOException
	{
		outputFileWriter.write("\n\n" + cacheStr + " Hits\t=\t" + hits);
		outputFileWriter.write("\n" + cacheStr + " Misses\t=\t" + misses);
		outputFileWriter.write("\n" + cacheStr + " Hit-Rate\t=\t" + (double)hits/(double)(hits+misses));
		outputFileWriter.write("\n" + cacheStr + " Miss-Rate\t=\t" + (double)misses/(double)(hits+misses));
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
	public static void printPowerTrace(String delimiter, long[] cyclesTaken, int emuThreads){
		long executionTime=0;
		try {
			traceWriter.write("\n");
			for (int i =0; i < emuThreads; i++)
			{
				if(cyclesTaken[i]==0)
					continue;
			executionTime = (long)((double)cyclesTaken[i]*1000/Statistics.coreFrequencies[i]); //In nano seconds
			traceWriter.write(i);
			traceWriter.write(delimiter);
			traceWriter.write("Simple"+delimiter);

			traceWriter.write(String.valueOf(powerCounters[i].getRenamePower()/executionTime + powerCounters[i].getBpredPower()/executionTime
					+powerCounters[i].getRegfilePower()/executionTime+powerCounters[i].getIcachePower()/executionTime
					+powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
					+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime
					+powerCounters[i].getLsqPower()/executionTime+powerCounters[i].getClockPower()/executionTime
					+powerCounters[i].getWindowPower()/executionTime));
			traceWriter.write(delimiter);
			
			traceWriter.write("AggressiveIdeal"+delimiter);
		}


		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}

	public static void printPowerStats(){
		long executionTime=0;
		try {
			for (int i =0; i < SystemConfig.NoOfCores; i++)
			{
				if(Statistics.coreCyclesTaken[i]==0)
					continue;
			executionTime = (Statistics.coreCyclesTaken[i]*1000/Statistics.coreFrequencies[i]); //In nano seconds
			outputFileWriter.write("\n\nCore: "+i+"\n\n");
			
			outputFileWriter.write("\n\nSimple conditional clocking \n\n");
			
			outputFileWriter.write("Bpred Power\t=\t"+powerCounters[i].getTotalBpredPower()/executionTime+"\n");
			outputFileWriter.write("Reg file Power\t=\t"+powerCounters[i].getTotalRegfilePower()/executionTime+"\n");
			outputFileWriter.write("I L1 Cache Power\t=\t"+powerCounters[i].getTotalIcachePower()/executionTime+"\n");
			outputFileWriter.write("D L1 Cache Power\t=\t"+powerCounters[i].getTotalDcachePower()/executionTime+"\n");
			if(SimulationConfig.nucaType==NucaType.NONE)
				outputFileWriter.write("L2 Cache Power\t=\t"+powerCounters[i].getTotalDcache2Power()/executionTime+"\n");
			outputFileWriter.write("ALU power\t=\t"+powerCounters[i].getTotalAluPower()/executionTime+"\n");
			outputFileWriter.write("Clock Power\t=\t"+powerCounters[i].getTotalClockPower()/executionTime+"\n");
			outputFileWriter.write("Rename Power\t=\t"+powerCounters[i].getTotalRenamePower()/executionTime+"\n");
			outputFileWriter.write("Window Power\t=\t"+powerCounters[i].getTotalWindowPower()/executionTime+"\n");
			outputFileWriter.write("LSQ Power\t=\t"+powerCounters[i].getTotalLsqPower()/executionTime+"\n");
			outputFileWriter.write("Result bus power\t=\t"+powerCounters[i].getTotalResultbusPower()/executionTime+"\n");


			outputFileWriter.write("Fetch Stage Power\t=\t"+(powerCounters[i].getTotalIcachePower()/executionTime+powerCounters[i].getTotalBpredPower()/executionTime)+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Dispatch Stage Power\t=\t"+powerCounters[i].getTotalRenamePower()/executionTime+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Issue Stage Power\t=\t"+(powerCounters[i].getTotalResultbusPower()/executionTime+powerCounters[i].getTotalAluPower()/executionTime
									+powerCounters[i].getTotalDcachePower()/executionTime+powerCounters[i].getTotalDcache2Power()/executionTime+powerCounters[i].getTotalLsqPower()/executionTime
									+powerCounters[i].getTotalWindowPower()/executionTime)+"\n");

			outputFileWriter.write("Total Power \t=\t"+(powerCounters[i].getTotalRenamePower()/executionTime + powerCounters[i].getTotalBpredPower()/executionTime
					+powerCounters[i].getTotalRegfilePower()/executionTime+powerCounters[i].getTotalIcachePower()/executionTime
					+powerCounters[i].getTotalResultbusPower()/executionTime+powerCounters[i].getTotalAluPower()/executionTime
					+powerCounters[i].getTotalDcachePower()/executionTime+powerCounters[i].getTotalDcache2Power()/executionTime
					+powerCounters[i].getTotalLsqPower()/executionTime+powerCounters[i].getTotalClockPower()/executionTime
					+powerCounters[i].getTotalWindowPower()/executionTime)+"\n");
			
			}

		}
		catch (IOException e) 
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
					(double)totalNumMicroOps/simulationTime + " KIPS\t\tin terms of micro-ops\n");
			outputFileWriter.write("Instructions per Second\t=\t" + 
					(double)totalHandledCISCInsn/simulationTime + " KIPS\t\tin terms of CISC instructions\n");
			
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void initStatistics()
	{		
		dataRead = new long[IpcBase.MaxNumJavaThreads];
		numHandledCISCInsn = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		numCISCInsn = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		noOfMicroOps = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		
		coreCyclesTaken = new long[SystemConfig.NoOfCores];
		coreFrequencies = new long[SystemConfig.NoOfCores];
		numCoreInstructions = new long[SystemConfig.NoOfCores];
		branchCount = new long[SystemConfig.NoOfCores];
		mispredictedBranchCount = new long[SystemConfig.NoOfCores];
		tempcoreCyclesTaken = new long[SystemConfig.NoOfCores];
		tempnumCoreInstructions = new long[SystemConfig.NoOfCores];
		tempbranchCount = new long[SystemConfig.NoOfCores];
		tempmispredictedBranchCount = new long[SystemConfig.NoOfCores];
		
		noOfMemRequests = new long[SystemConfig.NoOfCores];
		tempnoOfMemRequests = new long[SystemConfig.NoOfCores];
		noOfLoads = new long[SystemConfig.NoOfCores];
		tempnoOfLoads = new long[SystemConfig.NoOfCores];
		noOfStores = new long[SystemConfig.NoOfCores];
		tempnoOfStores = new long[SystemConfig.NoOfCores];
		noOfValueForwards = new long[SystemConfig.NoOfCores];
		tempnoOfValueForwards = new long[SystemConfig.NoOfCores];
		noOfTLBRequests = new long[SystemConfig.NoOfCores];
		tempnoOfTLBRequests = new long[SystemConfig.NoOfCores];
		noOfTLBHits = new long[SystemConfig.NoOfCores];
		tempnoOfTLBHits = new long[SystemConfig.NoOfCores];
		noOfTLBMisses = new long[SystemConfig.NoOfCores];
		tempnoOfTLBMisses = new long[SystemConfig.NoOfCores];
		noOfL1Requests = new long[SystemConfig.NoOfCores];
		tempnoOfL1Requests = new long[SystemConfig.NoOfCores];
		noOfL1Hits = new long[SystemConfig.NoOfCores];
		tempnoOfL1Hits = new long[SystemConfig.NoOfCores];
		noOfL1Misses = new long[SystemConfig.NoOfCores];
		tempnoOfL1Misses = new long[SystemConfig.NoOfCores];
		
		tempnoOfIRequests = new long[SystemConfig.NoOfCores];
		
		tempnoOfIHits = new long[SystemConfig.NoOfCores];
		
		tempnoOfIMisses = new long[SystemConfig.NoOfCores];
		
		powerCounters = new Counters[SystemConfig.NoOfCores];
		for(int i=0;i<SystemConfig.NoOfCores;i++){
			powerCounters[i] = new Counters();
		}
		
		if(SimulationConfig.pinpointsSimulation == true)
		{		
			FileReader pinpointsFileReader = null;
			BufferedReader pinpointsBufferedReader = null;
			int numberOfSlices = 0;
			String lineRead;
			
			try {
				pinpointsFileReader = new FileReader(SimulationConfig.pinpointsFile);
				pinpointsBufferedReader = new BufferedReader(pinpointsFileReader);
				
				while((lineRead = pinpointsBufferedReader.readLine()) != null)
				{
					numberOfSlices++;
				}
				
				weightsArray = new float[numberOfSlices];

				pinpointsBufferedReader.close();
				pinpointsFileReader.close();
				pinpointsFileReader = new FileReader(SimulationConfig.pinpointsFile);
				pinpointsBufferedReader = new BufferedReader(pinpointsFileReader);
				int index = 0;
				
				while((lineRead = pinpointsBufferedReader.readLine()) != null)
				{
					String subs[] = lineRead.split("[ \t]");
					weightsArray[index++] = Float.parseFloat(subs[2]);
				}			
				
			} catch (Exception e) {
				misc.Error.showErrorAndExit("pinpoints file not found");
				e.printStackTrace();
			}
			
			currentSlice = 0;
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
		
		if(SimulationConfig.pinpointsSimulation == true)
		{
			if(numHandledCISCInsn[javaThread][emuThread] > 3000000 * (currentSlice + 1))
			{
				processEndOfSlice();
			}
		}
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
	
	public static void setCoreCyclesTaken(long coreCyclesTaken, int core) {
		Statistics.coreCyclesTaken[core] = coreCyclesTaken;
	}

	public static void setCoreFrequencies(long coreFrequency, int core) {
		Statistics.coreFrequencies[core] = coreFrequency;
	}
	
	public static void setNumCoreInstructions(long numCoreInstructions, int core) {
		Statistics.numCoreInstructions[core] = numCoreInstructions;
	}

	public static void setBranchCount(long branchCount, int core) {
		Statistics.branchCount[core] = branchCount;
	}

	public static void setMispredictedBranchCount(long mispredictedBranchCount, int core) {
		Statistics.mispredictedBranchCount[core] = mispredictedBranchCount;
	}
	
	public static void setNoOfLoads(long noOfLoads, int core) {
		Statistics.noOfLoads[core] = noOfLoads;
	}

	public static void setNoOfStores(long noOfStores, int core) {
		Statistics.noOfStores[core] = noOfStores;
	}

	public static void setNoOfValueForwards(long noOfValueForwards, int core) {
		Statistics.noOfValueForwards[core] = noOfValueForwards;
	}
	
	public static void setNoOfTLBRequests(long noOfTLBRequests, int core) {
		Statistics.noOfTLBRequests[core] = noOfTLBRequests;
	}
	
	public static void setNoOfTLBHits(long noOfTLBHits, int core) {
		Statistics.noOfTLBHits[core] = noOfTLBHits;
	}

	public static void setNoOfTLBMisses(long noOfTLBMisses, int core) {
		Statistics.noOfTLBMisses[core] = noOfTLBMisses;
	}

	public static void setNoOfL1Hits(long noOfL1Hits, int core) {
		Statistics.noOfL1Hits[core] = noOfL1Hits;
	}

	public static void setNoOfL1Misses(long noOfL1Misses, int core) {
		Statistics.noOfL1Misses[core] = noOfL1Misses;
	}

	public static void setNoOfL2Hits(long noOfL2Hits) {
		Statistics.noOfL2Hits = noOfL2Hits;
	}

	public static void setNoOfL2Misses(long noOfL2Misses) {
		Statistics.noOfL2Misses = noOfL2Misses;
	}

	public static void setNoOfMemRequests(long noOfMemRequests, int core) {
		Statistics.noOfMemRequests[core] = noOfMemRequests;
	}

	public static void setNoOfL2Requests(long noOfL2Requests) {
		Statistics.noOfL2Requests = noOfL2Requests;
	}
	
	public static void setNoOfL1Requests(long noOfL1Requests, int core) {
		Statistics.noOfL1Requests[core] = noOfL1Requests;
	}
//	public static long getTime() {
//		return Statistics.time;
//	}
//	
//	public static void setTime(long time) {
//		Statistics.time = time;
//	}
//
//	public static long getSubsetTime() {
//		return subsetTime;
//	}
//
//	public static void setSubsetTime(long subsetTime) {
//		Statistics.subsetTime = subsetTime;
//	}
	
	public static void setSimulationTime(long simulationTime) {
		Statistics.simulationTime = simulationTime;
	}

	public static void setExecutable(String executableFile) {
		Statistics.benchmark = executableFile;
	}
	
	public static void setPerCorePowerStatistics(Counters powerCount, int core) {
//		System.out.println("Setting for coreid "+core + " "+SystemConfig.NoOfCores);
		Statistics.powerCounters[core]=powerCount;
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
	
	static int temp = 0;

	
	
	public static void printAllStatistics(String benchmarkName, 
			long startTime, long endTime) {
		//set up statistics module
		//Statistics.initStatistics();
		// Statistics.initStatistics();
		
		Statistics.setExecutable(benchmarkName);
		Statistics.setNoOfDirHits(MemorySystem.getDirectoryCache().getDirectoryHits());
		Statistics.setNoOfDirMisses(MemorySystem.getDirectoryCache().getDirectoryMisses());
		Statistics.setNoOfDirInvalidations(MemorySystem.getDirectoryCache().getInvalidations());
		Statistics.setNoOfDirDataForwards(MemorySystem.getDirectoryCache().getDataForwards());
		Statistics.setNoOfDirReadMiss(MemorySystem.getDirectoryCache().getNumReadMiss());
		Statistics.setNoOfDirWriteMiss(MemorySystem.getDirectoryCache().getNumWriteMiss());
		Statistics.setNoOfDirWriteHits(MemorySystem.getDirectoryCache().getNumWriteHit());
		Statistics.setNoOfDirEntries(MemorySystem.getDirectoryCache().getNumberOfDirectoryEntries());
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
			Statistics.setNoOfL2Requests(cache.noOfRequests);
			Statistics.setNoOfL2Hits(cache.hits);
			Statistics.setNoOfL2Misses(cache.misses);
			
		}
			
		Statistics.setSimulationTime(endTime - startTime);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.printSimulationTime();
		
		if(SimulationConfig.powerStats)
			Statistics.printPowerStats();
		
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
	
	public static void processEndOfSlice()
	{
		Core core;
		
		if(currentSlice < weightsArray.length)
		{
		
			for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
			{
				core = ArchitecturalComponent.getCores()[i];
				
				if(core.getNoOfInstructionsExecuted() == 0)
				{
					continue;
				}
				
				System.out.println("\n\n!!!!!!process end of slice!!!!!!!!\n\n");
				System.out.print(coreCyclesTaken[i] + " : " + GlobalClock.getCurrentTime()/core.getStepSize());
				System.out.println(" : " + tempcoreCyclesTaken[i]);
				
				coreCyclesTaken[i] += (long) (GlobalClock.getCurrentTime()/core.getStepSize() - tempcoreCyclesTaken[i]) * weightsArray[currentSlice];
				tempcoreCyclesTaken[i] = GlobalClock.getCurrentTime()/core.getStepSize();
				System.out.println(coreCyclesTaken[i] + " : " + tempcoreCyclesTaken[i]);
				System.out.println();
				coreFrequencies[i] = core.getFrequency();
				numCoreInstructions[i] += (long) (core.getNoOfInstructionsExecuted() - tempnumCoreInstructions[i]) * weightsArray[currentSlice];
				tempnumCoreInstructions[i] = core.getNoOfInstructionsExecuted();
				branchCount[i] += (long) (core.getPipelineInterface().getBranchCount() - tempbranchCount[i]) * weightsArray[currentSlice];
				tempbranchCount[i] = core.getPipelineInterface().getBranchCount();
				mispredictedBranchCount[i] += (long) (core.getPipelineInterface().getMispredCount() - tempmispredictedBranchCount[i]) * weightsArray[currentSlice];
				tempmispredictedBranchCount[i] = core.getPipelineInterface().getMispredCount();
				
				noOfMemRequests[i] += (long) (core.getPipelineInterface().getNoOfMemRequests() - tempnoOfMemRequests[i]) * weightsArray[currentSlice];
				tempnoOfMemRequests[i] = core.getPipelineInterface().getNoOfMemRequests();
				noOfLoads[i] += (long) (core.getPipelineInterface().getNoOfLoads() - tempnoOfLoads[i]) * weightsArray[currentSlice];
				tempnoOfLoads[i] = core.getPipelineInterface().getNoOfLoads();
				noOfStores[i] += (long) (core.getPipelineInterface().getNoOfStores() - tempnoOfStores[i]) * weightsArray[currentSlice];
				tempnoOfStores[i] = core.getPipelineInterface().getNoOfStores();
				noOfValueForwards[i] += (long) (core.getPipelineInterface().getNoOfValueForwards() - tempnoOfValueForwards[i]) * weightsArray[currentSlice];
				tempnoOfValueForwards[i] = core.getPipelineInterface().getNoOfValueForwards();
				noOfTLBRequests[i] += (long) (core.getPipelineInterface().getNoOfTLBRequests() - tempnoOfTLBRequests[i]) * weightsArray[currentSlice];
				tempnoOfTLBRequests[i] = core.getPipelineInterface().getNoOfTLBRequests();
				noOfTLBHits[i] += (long) (core.getPipelineInterface().getNoOfTLBHits() - tempnoOfTLBHits[i]) * weightsArray[currentSlice];
				tempnoOfTLBHits[i] = core.getPipelineInterface().getNoOfTLBHits();
				noOfTLBMisses[i] += (long) (core.getPipelineInterface().getNoOfTLBMisses() - tempnoOfTLBMisses[i]) * weightsArray[currentSlice];
				tempnoOfTLBMisses[i] = core.getPipelineInterface().getNoOfTLBMisses();
				noOfL1Requests[i] += (long) (core.getPipelineInterface().getNoOfL1Requests() - tempnoOfL1Requests[i]) * weightsArray[currentSlice];
				tempnoOfL1Requests[i] = core.getPipelineInterface().getNoOfL1Requests();
				noOfL1Hits[i] += (long) (core.getPipelineInterface().getNoOfL1Hits() - tempnoOfL1Hits[i]) * weightsArray[currentSlice];
				tempnoOfL1Hits[i] = core.getPipelineInterface().getNoOfL1Hits();
				noOfL1Misses[i] += (long) (core.getPipelineInterface().getNoOfL1Misses() - tempnoOfL1Misses[i]) * weightsArray[currentSlice];
				tempnoOfL1Misses[i] = core.getPipelineInterface().getNoOfL1Misses();
				
				noOfDirHits = (long) ((MemorySystem.getDirectoryCache().hits - tempnoOfDirHits) * weightsArray[currentSlice]);
				tempnoOfDirHits = MemorySystem.getDirectoryCache().hits;
				noOfDirMisses = (long) ((MemorySystem.getDirectoryCache().misses - tempnoOfDirMisses) * weightsArray[currentSlice]);
				tempnoOfDirMisses = MemorySystem.getDirectoryCache().misses;
				noOfDirInvalidations = (long) ((MemorySystem.getDirectoryCache().getInvalidations() - tempnoOfDirInvalidations) * weightsArray[currentSlice]);
				tempnoOfDirInvalidations = MemorySystem.getDirectoryCache().getInvalidations();
				noOfDirDataForwards = (long) ((MemorySystem.getDirectoryCache().getDataForwards() - tempnoOfDirDataForwards) * weightsArray[currentSlice]);
				tempnoOfDirDataForwards = MemorySystem.getDirectoryCache().getDataForwards();
				noOfDirWritebacks = (long) ((MemorySystem.getDirectoryCache().getWritebacks() - tempnoOfDirWritebacks) * weightsArray[currentSlice]);
				tempnoOfDirWritebacks = MemorySystem.getDirectoryCache().getWritebacks();
			}
		}
		
		currentSlice++;
	}
	public static long getNumCISCInsn(int javaTid, int tidEmu) {
		return numCISCInsn[javaTid][tidEmu];
	}
	
}