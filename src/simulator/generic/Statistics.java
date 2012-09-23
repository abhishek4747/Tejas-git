package generic;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

import memorysystem.Cache;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;

import power.Counters;
import config.SimulationConfig;
import config.SystemConfig;
import emulatorinterface.communication.IpcBase;

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
			outputFileWriter.write("ToolName: "+SimulationConfig.PinInstrumentor+"\n");
			outputFileWriter.write("Benchmark: "+benchmark+"\n");
			outputFileWriter.write("Pipeline: ");
			if (SimulationConfig.isPipelineInorder)
				outputFileWriter.write("Inorder Pipeline\n");
			else if (SimulationConfig.isPipelineMultiIssueInorder)
				outputFileWriter.write("MultiIssueInorder Pipeline\n");
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
	static long numHandledCISCInsn[][];
	static long numPINCISCInsn[][];
	static long noOfMicroOps[][];
	static double staticCoverage;
	static double dynamicCoverage;
	
	public static void printTranslatorStatistics()
	{
		for(int i = 0; i < IpcBase.MaxNumJavaThreads; i++)
		{
			for (int j=0; j<IpcBase.getEmuThreadsPerJavaThread(); j++) {
				totalNumMicroOps += noOfMicroOps[i][j];
//				totalNumMicroOps += numCoreInstructions[i];
				totalHandledCISCInsn += numHandledCISCInsn[i][j];
				totalPINCISCInsn += numPINCISCInsn[i][j];
			}
		}
		
		dynamicCoverage = ((double)totalHandledCISCInsn/(double)totalPINCISCInsn)*(double)100.0;
		
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
	
	public static void printTimingStatistics()
	{
		long maxCoreCycles = 0;
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
			
			//outputFileWriter.write("global clock\t=\t" + GlobalClock.getCurrentTime() + " cycles\n");
			//outputFileWriter.write("\n");
			outputFileWriter.write("Total Cycles taken\t\t=\t" + maxCoreCycles + "\n\n");
			outputFileWriter.write("Total IPC\t\t=\t" + (double)totalNumMicroOps/maxCoreCycles + "\t\tin terms of micro-ops\n");
			outputFileWriter.write("Total IPC\t\t=\t" + (double)totalHandledCISCInsn/maxCoreCycles + "\t\tin terms of CISC instructions\n\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				if(coreCyclesTaken[i]==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				outputFileWriter.write("core\t\t=\t" + i + "\n");
				outputFileWriter.write("instructions executed\t=\t" + numCoreInstructions[i] + "\n");
				outputFileWriter.write("cycles taken\t=\t" + coreCyclesTaken[i] + " cycles\n");
				//FIXME will work only if java thread is 1
				outputFileWriter.write("IPC\t\t=\t" + (double)noOfMicroOps[0][i]/coreCyclesTaken[i] + "\t\tin terms of micro-ops\n");
				outputFileWriter.write("IPC\t\t=\t" + (double)numHandledCISCInsn[0][i]/coreCyclesTaken[i] + "\t\tin terms of CISC instructions\n");
				
				outputFileWriter.write("core frequency\t=\t" + coreFrequencies[i] + " MHz\n");
				outputFileWriter.write("time taken\t=\t" + (double)coreCyclesTaken[i]/coreFrequencies[i] + " microseconds\n");
				outputFileWriter.write("\n");
				
				outputFileWriter.write("number of branches\t=\t" + branchCount[i] + "\n");
				outputFileWriter.write("number of mispredicted branches\t=\t" + mispredictedBranchCount[i] + "\n");
				outputFileWriter.write("branch predictor accuracy\t=\t" + (double)((double)(branchCount[i]-mispredictedBranchCount[i])*100/branchCount[i]) + " %\n");
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
	public static String nocTopology;
	public static String nocRoutingAlgo;
	public static int hopcount=0;
	static long noOfDirHits;
	static long noOfDirMisses;
	static long noOfDirDataForwards;
	static long noOfDirInvalidations;
	static long noOfDirWritebacks;
	
	
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
				outputFileWriter.write("TLB Requests\t=\t" + noOfTLBRequests[i] + "\n");
				outputFileWriter.write("TLB Hits\t=\t" + noOfTLBHits[i] + "\n");
				outputFileWriter.write("TLB Misses\t=\t" + noOfTLBMisses[i] + "\n");
				if (noOfTLBRequests[i] != 0)
				{
					outputFileWriter.write("TLB Hit-rate\t=\t" + (float)(noOfTLBHits[i])/noOfTLBRequests[i] + "\n");
					outputFileWriter.write("TLB Miss-rate\t=\t" + (float)(noOfTLBMisses[i])/noOfTLBRequests[i] + "\n");
				}
				outputFileWriter.write("L1 Requests\t=\t" + noOfMemRequests[i]  + "\n");
				outputFileWriter.write("L1 Hits\t\t=\t" + noOfL1Hits[i] + "\n");
				outputFileWriter.write("L1 Misses\t=\t" +(noOfMemRequests[i] - noOfL1Hits[i]) + "\n");
				outputFileWriter.write("I Requests\t=\t" + noOfIRequests[i] + "\n");
				outputFileWriter.write("I Hits\t\t=\t" + noOfIHits[i] + "\n");
				outputFileWriter.write("I Misses\t=\t" + (noOfIRequests[i] - noOfIHits[i]) + "\n");
				if (noOfL1Requests[i] != 0)
				{
					outputFileWriter.write("L1 Hit-Rate\t=\t" + (float)(noOfL1Hits[i])/noOfMemRequests[i] + "\n");
					outputFileWriter.write("L1 Miss-Rate\t=\t" + (float)(noOfMemRequests[i] - noOfL1Hits[i])/noOfMemRequests[i] + "\n");
				}
				if (noOfIRequests[i] != 0)
				{
					outputFileWriter.write("I Hit-Rate\t=\t" + (float)(noOfIHits[i])/noOfIRequests[i] + "\n");
					outputFileWriter.write("I Miss-Rate\t=\t" + (float) (noOfIRequests[i] - noOfIHits[i])/noOfIRequests[i] + "\n");
				
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
			if (nocRoutingAlgo != null)
			{
				outputFileWriter.write("L2 noc Topology\t=\t" + nocTopology + "\n");
				outputFileWriter.write("L2 noc Routing Algorithm\t=\t" + nocRoutingAlgo + "\n");
			}
			if (hopcount != 0)
			{
				outputFileWriter.write("Router Hops\t=\t" + hopcount + "\n");
			}
			
			outputFileWriter.write("Directory Hits\t=\t" + noOfDirHits + "\n");
			outputFileWriter.write("Directory Misses\t=\t" + noOfDirMisses + "\n");
			outputFileWriter.write("Directory Invalidations\t=\t" + noOfDirInvalidations + "\n");
			outputFileWriter.write("Directory DataForwards\t=\t" + noOfDirDataForwards + "\n");
			outputFileWriter.write("Directory Writebacks\t=\t" + noOfDirWritebacks + "\n");
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
	
	
	
	
	//Simulation time
	static long time;
	static long subsetTime;

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


		} catch (IOException e) {
			// TODO Auto-generated catch block
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

/*			traceWriter.write(String.valueOf(powerCounters[i].getIcachePower()/executionTime+powerCounters[i].getBpredPower()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getRenamePower()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
									+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime+powerCounters[i].getLsqPower()/executionTime
									+powerCounters[i].getWindowPower()/executionTime));
			traceWriter.write(delimiter);
*/			traceWriter.write(String.valueOf(powerCounters[i].getRenamePower()/executionTime + powerCounters[i].getBpredPower()/executionTime
					+powerCounters[i].getRegfilePower()/executionTime+powerCounters[i].getIcachePower()/executionTime
					+powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
					+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime
					+powerCounters[i].getLsqPower()/executionTime+powerCounters[i].getClockPower()/executionTime
					+powerCounters[i].getWindowPower()/executionTime));
			traceWriter.write(delimiter);
			
			traceWriter.write("AggressiveIdeal"+delimiter);
/*			traceWriter.write(String.valueOf(powerCounters[i].getIcachePowerCC2()/executionTime+powerCounters[i].getBpredPowerCC2()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getRenamePowerCC2()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getResultbusPowerCC2()/executionTime+powerCounters[i].getAluPowerCC2()/executionTime
									+powerCounters[i].getDcachePowerCC2()/executionTime+powerCounters[i].getDcache2PowerCC2()/executionTime+powerCounters[i].getLsqPowerCC2()/executionTime
									+powerCounters[i].getWindowPowerCC2()/executionTime));
			traceWriter.write(delimiter);
*//*			traceWriter.write(String.valueOf(powerCounters[i].getIcachePowerCC3()/executionTime+powerCounters[i].getBpredPowerCC3()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getRenamePowerCC3()/executionTime));
			traceWriter.write(delimiter);
			traceWriter.write(String.valueOf(powerCounters[i].getResultbusPowerCC3()/executionTime+powerCounters[i].getAluPowerCC3()/executionTime
									+powerCounters[i].getDcachePowerCC3()/executionTime+powerCounters[i].getDcache2PowerCC3()/executionTime+powerCounters[i].getLsqPowerCC3()/executionTime
									+powerCounters[i].getWindowPowerCC3()/executionTime));
			traceWriter.write(delimiter);
*/
		}


		} catch (IOException e) {
			// TODO Auto-generated catch block
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
System.out.println("execution time = "+executionTime);
			outputFileWriter.write("\n\nCore: "+i+"\n\n");
			
			outputFileWriter.write("\n\nSimple conditional clocking \n\n");
			
			outputFileWriter.write("Bpred Power\t=\t"+powerCounters[i].getTotalBpredPower()/executionTime+"\n");
			outputFileWriter.write("Reg file Power\t=\t"+powerCounters[i].getTotalRegfilePower()/executionTime+"\n");
			outputFileWriter.write("I L1 Cache Power\t=\t"+powerCounters[i].getTotalIcachePower()/executionTime+"\n");
			outputFileWriter.write("D L1 Cache Power\t=\t"+powerCounters[i].getTotalDcachePower()/executionTime+"\n");
			outputFileWriter.write("L2 Cache Power\t=\t"+powerCounters[i].getTotalDcache2Power()/executionTime+"\n");
			outputFileWriter.write("ALU power\t=\t"+powerCounters[i].getTotalAluPower()/executionTime+"\n");
			outputFileWriter.write("Clock Power\t=\t"+powerCounters[i].getTotalClockPower()/executionTime+"\n");
			outputFileWriter.write("Rename Power\t=\t"+powerCounters[i].getTotalRenamePower()/executionTime+"\n");
			outputFileWriter.write("Window Power\t=\t"+powerCounters[i].getTotalWindowPower()/executionTime+"\n");
			outputFileWriter.write("LSQ Power\t=\t"+powerCounters[i].getTotalLsqPower()/executionTime+"\n");
			outputFileWriter.write("Result bus power\t=\t"+powerCounters[i].getTotalResultbusPower()/executionTime+"\n");

/*			
			outputFileWriter.write("Avg Bpred Power\t=\t"+powerCounters[i].getBpredPower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg Clock Power\t=\t"+powerCounters[i].getClockPower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg Reg file Power\t=\t"+powerCounters[i].getRegfilePower()/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg I L1 Cache Power\t=\t"+powerCounters[i].getIcachePower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg D L1 Cache Power\t=\t"+powerCounters[i].getDcachePower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg L2 Cache Power\t=\t"+powerCounters[i].getDcache2Power()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg ALU power\t=\t"+powerCounters[i].getAluPower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg Rename Power\t=\t"+powerCounters[i].getRenamePower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg Window Power\t=\t"+powerCounters[i].getWindowPower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg LSQ Power\t=\t"+powerCounters[i].getLsqPower()/executionTime/coreCyclesTaken[i]+"\n");
			outputFileWriter.write("Avg Result bus power\t=\t"+powerCounters[i].getResultbusPower()/executionTime/coreCyclesTaken[i]+"\n");
*/
			
			outputFileWriter.write("Fetch Stage Power\t=\t"+(powerCounters[i].getTotalIcachePower()/executionTime+powerCounters[i].getTotalBpredPower()/executionTime)+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Dispatch Stage Power\t=\t"+powerCounters[i].getTotalRenamePower()/executionTime+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Issue Stage Power\t=\t"+(powerCounters[i].getTotalResultbusPower()/executionTime+powerCounters[i].getTotalAluPower()/executionTime
									+powerCounters[i].getTotalDcachePower()/executionTime+powerCounters[i].getTotalDcache2Power()/executionTime+powerCounters[i].getTotalLsqPower()/executionTime
									+powerCounters[i].getTotalWindowPower()/executionTime)+"\n");
			
/*			outputFileWriter.write("Avg Fetch Stage Power\t=\t"+(powerCounters[i].getIcachePower()/executionTime+powerCounters[i].getBpredPower()/executionTime)/coreCyclesTaken[i]+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Avg Dispatch Stage Power\t=\t"+powerCounters[i].getRenamePower()/executionTime/coreCyclesTaken[i]+"\n");
			//TODO only for out of order ?
			outputFileWriter.write("Avg Issue Stage Power\t=\t"+(powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
									+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime+powerCounters[i].getLsqPower()/executionTime
									+powerCounters[i].getWindowPower()/executionTime)/coreCyclesTaken[i]+"\n");
			
*/	
			outputFileWriter.write("Total Power \t=\t"+(powerCounters[i].getTotalRenamePower()/executionTime + powerCounters[i].getTotalBpredPower()/executionTime
					+powerCounters[i].getTotalRegfilePower()/executionTime+powerCounters[i].getTotalIcachePower()/executionTime
					+powerCounters[i].getTotalResultbusPower()/executionTime+powerCounters[i].getTotalAluPower()/executionTime
					+powerCounters[i].getTotalDcachePower()/executionTime+powerCounters[i].getTotalDcache2Power()/executionTime
					+powerCounters[i].getTotalLsqPower()/executionTime+powerCounters[i].getTotalClockPower()/executionTime
					+powerCounters[i].getTotalWindowPower()/executionTime)+"\n");
			
/*			outputFileWriter.write("Avg Total Power per cycle \t=\t"+(powerCounters[i].getRenamePower()/executionTime + powerCounters[i].getBpredPower()/executionTime
					+powerCounters[i].getRegfilePower()/executionTime+powerCounters[i].getIcachePower()/executionTime
					+powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
					+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime
					+powerCounters[i].getLsqPower()/executionTime+powerCounters[i].getClockPower()/executionTime
					+powerCounters[i].getWindowPower()/executionTime)/coreCyclesTaken[i]+"\n");

			outputFileWriter.write("Avg Total Power per instruction \t=\t"+(powerCounters[i].getRenamePower()/executionTime + powerCounters[i].getBpredPower()/executionTime
					+powerCounters[i].getRegfilePower()/executionTime+powerCounters[i].getIcachePower()/executionTime
					+powerCounters[i].getResultbusPower()/executionTime+powerCounters[i].getAluPower()/executionTime
					+powerCounters[i].getDcachePower()/executionTime+powerCounters[i].getDcache2Power()/executionTime
					+powerCounters[i].getLsqPower()/executionTime+powerCounters[i].getClockPower()/executionTime
					+powerCounters[i].getWindowPower()/executionTime)/numCoreInstructions[i]+"\n");
			
*/

			
			
			/*********************************************************************/
			
			
			outputFileWriter.write(powerCounters[i].getTotalRegfilePower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalIcachePower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalDcachePower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalDcache2Power()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalAluPower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalClockPower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalRenamePower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalWindowPower()/executionTime+",");
			outputFileWriter.write(+powerCounters[i].getTotalLsqPower()/executionTime+",");
			outputFileWriter.write(powerCounters[i].getTotalResultbusPower()/executionTime+"\n");
			
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


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
			
			
			if(subsetTime != 0)
			{
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumMicroOps/subsetTime + " KIPS\t\tin terms of micro-ops\n");
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalHandledCISCInsn/subsetTime + " KIPS\t\tin terms of CISC instructions\n");
			}
			else
			{
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalNumMicroOps/time + " KIPS\t\tin terms of micro-ops\n");
				outputFileWriter.write("Instructions per Second\t=\t" + (double)totalHandledCISCInsn/time + " KIPS\t\tin terms of CISC instructions\n");
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
		numHandledCISCInsn = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		numPINCISCInsn = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		noOfMicroOps = new long[IpcBase.MaxNumJavaThreads][IpcBase.getEmuThreadsPerJavaThread()];
		
		coreCyclesTaken = new long[SystemConfig.NoOfCores];
		coreFrequencies = new long[SystemConfig.NoOfCores];
		numCoreInstructions = new long[SystemConfig.NoOfCores];
		branchCount = new long[SystemConfig.NoOfCores];
		mispredictedBranchCount = new long[SystemConfig.NoOfCores];
		
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
		
		powerCounters = new Counters[SystemConfig.NoOfCores];
		for(int i=0;i<SystemConfig.NoOfCores;i++){
			powerCounters[i] = new Counters();
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
			e.printStackTrace();
		}
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
			System.out.println("power trace written to " + SimulationConfig.outputFileName+"Trace.csv");
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
	}
	
	public static void setNumPINCISCInsn(long numInstructions, int javaThread, int emuThread) {
		Statistics.numPINCISCInsn[javaThread][emuThread] = numInstructions;
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
	public static long getNoOfDirWritebacks() {
		return noOfDirWritebacks;
	}
	
	public static void setNoOfDirWritebacks(long noOfDirWritebacks) {
		Statistics.noOfDirWritebacks = noOfDirWritebacks;
	}
	
	public static void printAllStatistics(String benchmarkName, 
			long startTime, long endTime) {
		//set up statistics module
		// Statistics.initStatistics();
		
		Statistics.setExecutable(benchmarkName);
		
		//set memory statistics for levels L2 and below
		for (Enumeration<String> cacheNameSet = MemorySystem.getCacheList().keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
		{
			String cacheName = cacheNameSet.nextElement();
			Cache cache = MemorySystem.getCacheList().get(cacheName);
			
			if (cache.nucaType != NucaType.NONE )
			{
				Statistics.nocTopology = ((NucaCache)cache).cacheBank[0][0].getRouter().topology.name();
				Statistics.nocRoutingAlgo = ((NucaCache)cache).cacheBank[0][0].getRouter().rAlgo.name();
				for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
				{
					for(int j=0; j< ((NucaCache)cache).cacheColumns;j++)
					{
						Statistics.hopcount += ((NucaCache)cache).cacheBank[i][j].getRouter().hopCounters; 
					}
				}
				if(Statistics.nocTopology.equals("FATTREE") ||
						Statistics.nocTopology.equals("OMEGA") ||
						Statistics.nocTopology.equals("BUTTERFLY")) {
					for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
						Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
					}
				}
			}
			Statistics.setNoOfL2Requests(cache.noOfRequests);
			Statistics.setNoOfL2Hits(cache.hits);
			Statistics.setNoOfL2Misses(cache.misses);
			
		}
			
		Statistics.setTime(endTime - startTime);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printTranslatorStatistics();
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.printSimulationTime();
		
		if(SimulationConfig.powerStats)
			Statistics.printPowerStats();
		
		Statistics.closeStream();
	}	
}