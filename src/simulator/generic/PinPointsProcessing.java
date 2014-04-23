package generic;

import java.io.BufferedReader;
import java.io.FileReader;

import main.ArchitecturalComponent;
import memorysystem.MemorySystem;

import config.Interconnect;
import config.SimulationConfig;
import config.SystemConfig;

public class PinPointsProcessing {
	
	static float[] weightsArray;
	static int currentSlice;
	
	static long coreCyclesTaken[];
	static long coreFrequencies[];				//in MHz
	static long numCoreInstructions[];
	static long branchCount[];
	static long mispredictedBranchCount[];	
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
	static long noOfDirHits;
	static long noOfDirMisses;
	static long noOfDirDataForwards;
	static long noOfDirInvalidations;
	static long noOfDirWritebacks;
	
	static long tempcoreCyclesTaken[];
	static long tempnumCoreInstructions[];
	static long tempbranchCount[];
	static long tempmispredictedBranchCount[];
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
	
	static void initialize()
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

		tempcoreCyclesTaken = new long[SystemConfig.NoOfCores];
		tempnumCoreInstructions = new long[SystemConfig.NoOfCores];
		tempbranchCount = new long[SystemConfig.NoOfCores];
		tempmispredictedBranchCount = new long[SystemConfig.NoOfCores];

		tempnoOfMemRequests = new long[SystemConfig.NoOfCores];
		tempnoOfLoads = new long[SystemConfig.NoOfCores];
		tempnoOfStores = new long[SystemConfig.NoOfCores];
		tempnoOfValueForwards = new long[SystemConfig.NoOfCores];
		tempnoOfTLBRequests = new long[SystemConfig.NoOfCores];
		tempnoOfTLBHits = new long[SystemConfig.NoOfCores];
		tempnoOfTLBMisses = new long[SystemConfig.NoOfCores];
		tempnoOfL1Requests = new long[SystemConfig.NoOfCores];
		tempnoOfL1Hits = new long[SystemConfig.NoOfCores];
		tempnoOfL1Misses = new long[SystemConfig.NoOfCores];
		tempnoOfIRequests = new long[SystemConfig.NoOfCores];
		tempnoOfIHits = new long[SystemConfig.NoOfCores];
		tempnoOfIMisses = new long[SystemConfig.NoOfCores];
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
				//TODO split into iTLB and dTLB
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
				
				if(SystemConfig.interconnect == Interconnect.Bus)
				{
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
		}
		
		currentSlice++;
	}
	
	public static void toProcessEndOfSlice(long numHandledCISCInsn) 
	{
		if(SimulationConfig.pinpointsSimulation == true)
		{
			if(numHandledCISCInsn > 3000000 * (currentSlice + 1))
			{
				processEndOfSlice();
			}
		}
	}

}
