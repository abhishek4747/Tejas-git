package generic;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import config.SimulationConfig;
import config.SystemConfig;
import emulatorinterface.Newmain;
import emulatorinterface.communication.IPCBase;

public class Statistics {
	
	static FileWriter outputFileWriter;
	
	
	
	
	public static void printSystemConfig()
	{
		//read config.xml and write to output file
	}
	
	
	
	
	//Translator Statistics
	
	static long dataRead[];
	static long numInstructions[];
	static long noOfMicroOps[];
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
			
			for(int i = 0; i < IPCBase.MAXNUMTHREADS; i++)
			{
				outputFileWriter.write("Java thread\t=\t" + i + "\n");
				outputFileWriter.write("Data Read\t=\t" + dataRead[i] + " bytes\n");
				outputFileWriter.write("Number of instructions provided by emulator\t=\t" + numInstructions[i] + "\n");
				outputFileWriter.write("Number of Micro-Ops\t=\t" + noOfMicroOps[i] + " \n");
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("Static coverage\t\t=\t" + staticCoverage + "\n");
			outputFileWriter.write("Dynamic Coverage\t=\t" + dynamicCoverage);
			outputFileWriter.write("\n\n");
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
				outputFileWriter.write("cycles taken\t=\t" + coreCyclesTaken[i] + " cycles\n");
				outputFileWriter.write("IPC\t\t=\t" + (double)numCoreInstructions[i]/coreCyclesTaken[i] + "\n");
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
	
	static long noOfLoads[];
	static long noOfStores[];
	static long noOfValueForwards[];
	static long noOfTLBHits[];
	static long noOfTLBMisses[];
	static long noOfL1Hits[];
	static long noOfL1Misses[];
	static long noOfL2Hits;
	static long noOfL2Misses;
	
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
				outputFileWriter.write("Loads\t\t=\t" + noOfLoads[i] + "\n");
				outputFileWriter.write("Stores\t\t=\t" + noOfStores[i] + "\n");
				outputFileWriter.write("Value Forwards\t=\t" + noOfValueForwards[i] + "\n");
				outputFileWriter.write("L1 Hits\t\t=\t" + noOfL1Hits[i] + "\n");
				outputFileWriter.write("L1 Misses\t=\t" + noOfL1Misses[i] + "\n");
				outputFileWriter.write("\n");
			}
			outputFileWriter.write("\n");
			
			outputFileWriter.write("[Other cache statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("L2 Hits\t\t=\t" + noOfL2Hits + "\n");
			outputFileWriter.write("L2 Misses\t=\t" + noOfL2Misses + "\n");
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	//Simulation time
	
	public static void printSimulationTime(long time)
	{
		//print time taken by simulator
		long seconds = time/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Simulator Time]\n");
			
			outputFileWriter.write("Time Taken\t=\t" + minutes + " : " + seconds + " minutes");
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void initStatistics()
	{		
		dataRead = new long[IPCBase.MAXNUMTHREADS];
		numInstructions = new long[IPCBase.MAXNUMTHREADS];
		noOfMicroOps = new long[IPCBase.MAXNUMTHREADS];
		
		coreCyclesTaken = new long[SystemConfig.NoOfCores];
		coreFrequencies = new long[SystemConfig.NoOfCores];
		numCoreInstructions = new long[SystemConfig.NoOfCores];
		
		noOfLoads = new long[SystemConfig.NoOfCores];
		noOfStores = new long[SystemConfig.NoOfCores];
		noOfValueForwards = new long[SystemConfig.NoOfCores];
		noOfTLBHits = new long[SystemConfig.NoOfCores];
		noOfTLBMisses = new long[SystemConfig.NoOfCores];
		noOfL1Hits = new long[SystemConfig.NoOfCores];
		noOfL1Misses = new long[SystemConfig.NoOfCores];
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
				sb.append(Newmain.executableFile + "_");
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
			sb.append(Newmain.executableFile + "_");
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

	public static void setNumInstructions(long numInstructions, int thread) {
		Statistics.numInstructions[thread] = numInstructions;
	}

	public static void setNoOfMicroOps(long noOfMicroOps, int thread) {
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
}
