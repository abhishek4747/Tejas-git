package generic;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import config.SimulationConfig;
import config.SystemConfig;
import emulatorinterface.Newmain;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.translator.x86.objparser.ObjParser;

public class Statistics {
	
	static FileWriter outputFileWriter;
	
	
	
	static String benchmark;
	public static void printSystemConfig()
	{
		//read config.xml and write to output file
		try
		{
			outputFileWriter.write("[Configuration]\n");
			outputFileWriter.write("\n");
			outputFileWriter.write("ToolName: "+SimulationConfig.PinInstrumentor+"\n");
			outputFileWriter.write("Benchmark: "+benchmark+"\n");
			outputFileWriter.write("Pipeline: ");
			if (SimulationConfig.isPipelineInorder)
				outputFileWriter.write("Inorder Pipeline\n");
			else if (SimulationConfig.isPipelineStatistical)
				outputFileWriter.write("Statistical Pipeline\n");
			else outputFileWriter.write("OutOrder Pipeline\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	//Translator Statistics
	
	static long dataRead[];
	static long numInstructions[][];
	static long noOfMicroOps[][];
	static double staticCoverage;
	static double dynamicCoverage;
	
	public static void printTranslatorStatistics()
	{
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
				outputFileWriter.write("Number of instructions provided by emulator\t=\t" + numInstructions[i] + "\n");
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("Static coverage\t\t=\t" + staticCoverage + " %\n");
			outputFileWriter.write("Dynamic Coverage\t=\t" + dynamicCoverage + " %\n");
			outputFileWriter.write("\n");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	//Timing Statistics
	
	static long coreCyclesTaken[];
	static long coreFrequencies[];				//in MHz
	static long numCoreInstructions[];

	public static void printTimingStatistics()
	{
		//for each core, print number of cycles taken by pipeline to reach completion
		
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Timing Statistics]\n");
			outputFileWriter.write("\n");
			
			//outputFileWriter.write("global clock\t=\t" + GlobalClock.getCurrentTime() + " cycles\n");
			//outputFileWriter.write("\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("instructions executed\t=\t" + numCoreInstructions[i] + "\n");
				outputFileWriter.write("cycles taken\t=\t" + coreCyclesTaken[i] + " cycles\n");
				//FIXME will work only if java thread is 1
				outputFileWriter.write("IPC\t\t=\t" + (double)noOfMicroOps[0][i]/coreCyclesTaken[i] + "\t\tin terms of micro-ops\n");
				outputFileWriter.write("IPC\t\t=\t" + (double)numInstructions[0][i]/coreCyclesTaken[i] + "\t\tin terms of CISC instructions\n");
				
				outputFileWriter.write("core frequency\t=\t" + coreFrequencies[i] + " MHz\n");
				outputFileWriter.write("time taken\t=\t" + (double)coreCyclesTaken[i]/coreFrequencies[i] + " microseconds\n");
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
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
	static long noOfIRequests[];
	static long noOfIHits[];
	static long noOfIMisses[];
	
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
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("Memory Requests\t=\t" + noOfMemRequests[i] + "\n");
				outputFileWriter.write("Loads\t\t=\t" + noOfLoads[i] + "\n");
				outputFileWriter.write("Stores\t\t=\t" + noOfStores[i] + "\n");
				outputFileWriter.write("LSQ forwardings\t=\t" + noOfValueForwards[i] + "\n");
				outputFileWriter.write("TLB Requests\t=\t" + noOfTLBRequests[i] + "\n");
				outputFileWriter.write("TLB Hits\t=\t" + noOfTLBHits[i] + "\n");
				outputFileWriter.write("TLB Misses\t=\t" + noOfTLBMisses[i] + "\n");
				if (noOfTLBRequests[i] != 0)
				{
					outputFileWriter.write("TLB Hit-rate\t=\t" + (float)(noOfTLBHits[i])/noOfTLBRequests[i] + "\n");
					outputFileWriter.write("TLB Miss-rate\t=\t" + (float)(noOfTLBMisses[i])/noOfTLBRequests[i] + "\n");
				}
				outputFileWriter.write("L1 Requests\t=\t" + noOfL1Requests[i] + "\n");
				outputFileWriter.write("L1 Hits\t\t=\t" + noOfL1Hits[i] + "\n");
				outputFileWriter.write("L1 Misses\t=\t" + noOfL1Misses[i] + "\n");
				outputFileWriter.write("I Requests\t=\t" + noOfIRequests[i] + "\n");
				outputFileWriter.write("I Hits\t\t=\t" + noOfIHits[i] + "\n");
				outputFileWriter.write("I Misses\t=\t" + noOfIMisses[i] + "\n");
				if (noOfL1Requests[i] != 0)
				{
					outputFileWriter.write("L1 Hit-Rate\t=\t" + (float)(noOfL1Hits[i])/noOfL1Requests[i] + "\n");
					outputFileWriter.write("L1 Miss-Rate\t=\t" + (float)(noOfL1Misses[i])/noOfL1Requests[i] + "\n");
					outputFileWriter.write("I Hit-Rate\t=\t" + (float)(noOfIHits[i])/noOfIRequests[i] + "\n");
					outputFileWriter.write("I Miss-Rate\t=\t" + (float)(noOfIMisses[i])/noOfIRequests[i] + "\n");
				
				}
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
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	//Simulation time
	static long time;
	static long subsetTime;




	public static void printSimulationTime()
	{
		//print time taken by simulator
		long seconds = time/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Simulator Time]\n");
			
			outputFileWriter.write("Time Taken\t\t=\t" + minutes + " : " + seconds + " minutes\n");
			
			long totalNumMicroOps = 0;
			long totalNumInstructions = 0;
			for(int i = 0; i < IpcBase.MaxNumJavaThreads; i++)
			{
				for (int j=0; j<IpcBase.EmuThreadsPerJavaThread; j++) {
					totalNumMicroOps += noOfMicroOps[i][j];
//					totalNumMicroOps += numCoreInstructions[i];
					totalNumInstructions += numInstructions[i][j];
				}
				
			}
			if(subsetTime != 0)
			{
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumMicroOps/subsetTime + " KIPS\t\tin terms of micro-ops\n");
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumInstructions/subsetTime + " KIPS\t\tin terms of CISC instructions\n");
			}
			else
			{
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumMicroOps/time + " KIPS\t\tin terms of micro-ops\n");
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumInstructions/time + " KIPS\t\tin terms of CISC instructions\n");
			}
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
		numInstructions = new long[IpcBase.MaxNumJavaThreads][IpcBase.EmuThreadsPerJavaThread];
		noOfMicroOps = new long[IpcBase.MaxNumJavaThreads][IpcBase.EmuThreadsPerJavaThread];
		
		coreCyclesTaken = new long[SystemConfig.NoOfCores];
		coreFrequencies = new long[SystemConfig.NoOfCores];
		numCoreInstructions = new long[SystemConfig.NoOfCores];
		
		noOfMemRequests = new long[SystemConfig.NoOfCores];
		noOfLoads = new long[SystemConfig.NoOfCores];
		noOfStores = new long[SystemConfig.NoOfCores];
		noOfValueForwards = new long[SystemConfig.NoOfCores];
		noOfTLBRequests = new long[SystemConfig.NoOfCores];
		noOfTLBHits = new long[SystemConfig.NoOfCores];
		noOfTLBMisses = new long[SystemConfig.NoOfCores];
		noOfL1Requests = new long[SystemConfig.NoOfCores];
		noOfL1Hits = new long[SystemConfig.NoOfCores];
		noOfL1Misses = new long[SystemConfig.NoOfCores];
		noOfIRequests = new long[SystemConfig.NoOfCores];
		noOfIHits = new long[SystemConfig.NoOfCores];
		noOfIMisses = new long[SystemConfig.NoOfCores];
		
	}	
	
	public static void openStream()
	{
		if(SimulationConfig.outputFileName != null && SimulationConfig.outputFileName.compareTo("default") != 0)
		{
			try
			{
				outputFileWriter = new FileWriter(SimulationConfig.outputFileName);
			}
			catch (IOException e)
			{
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
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("DEFAULT_");
		    Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			sb.append(sdf.format(cal.getTime()));
			try
			{
				outputFileWriter = new FileWriter(sb.toString());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			System.out.println("statistics written to " + sb.toString());
		}
	}	
	
	public static void closeStream()
	{
		try
		{
			outputFileWriter.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void setDataRead(long dataRead, int thread) {
		Statistics.dataRead[thread] = dataRead;
	}

	public static void setNumInstructions(long numInstructions[], int thread) {
		Statistics.numInstructions[thread] = numInstructions;
	}

	public static void setNoOfMicroOps(long noOfMicroOps[], int thread) {
		Statistics.noOfMicroOps[thread] = noOfMicroOps;
	}
	
	public static void setStaticCoverage(double staticCoverage) {
		Statistics.staticCoverage = staticCoverage;
	}
	
	public static void setDynamicCoverage(double dynamicCoverage) {
		Statistics.dynamicCoverage = dynamicCoverage;
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
	public static void setNoOfIHits(long noOfIHits, int core) {
		Statistics.noOfIHits[core] = noOfIHits;
	}
	public static void setNoOfIMisses(long noOfIMisses, int core) {
		Statistics.noOfIMisses[core] = noOfIMisses;
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
	public static void setNoOfIRequests(long noOfIRequests, int core) {
		Statistics.noOfIRequests[core] = noOfIRequests;
	}
	public static long getTime() {
		return Statistics.time;
	}
	
	public static void setTime(long time) {
		Statistics.time = time;
	}

	public static long getSubsetTime() {
		return subsetTime;
	}

	public static void setSubsetTime(long subsetTime) {
		Statistics.subsetTime = subsetTime;
	}

	public static void setExecutable(String executableFile) {
		Statistics.benchmark = executableFile;
	}
}