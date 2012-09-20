/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;

import main.Main;
import main.CustomObjectPool;
import net.optical.TopLevelTokenBus;
import pipeline.PipelineInterface;
import pipeline.inorder.InorderExecutionEngine;
import config.SimulationConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.BarrierTable;
import generic.Core;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.OperationType;
import generic.Statistics;
/* MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class RunnableThread implements Encoding {
	
	TopLevelTokenBus tokenBus;

	public static final int INSTRUCTION_THRESHOLD = 2000;
	//private static final int PACKET_THRESHOLD = 000;
	
	
	boolean doNotProcess = false;
	boolean writeToFile = SimulationConfig.writeToFile;
	long numInsToWrite = SimulationConfig.numInstructionsToBeWritten;
	String fileName = SimulationConfig.InstructionsFilename; //TODO take from config 
//	public static Hashtable<Integer,Integer> threadCoreMaping;

	OutputStream fileOut;
	OutputStream bufferOut;
	ObjectOutput output;

	int tid;
	long sum = 0; // checksum
	static int EMUTHREADS = IpcBase.getEmuThreadsPerJavaThread();
	int currentEMUTHREADS = 0;  //total number of livethreads
	int maxCoreAssign = 0;      //the maximum core id assigned 
	
	static ThreadParams[] threadParams = new ThreadParams[EMUTHREADS];

	GenericCircularQueue<Instruction>[] inputToPipeline;
	static long ignoredInstructions = 0;

	// QQQ re-arrange packets for use by translate instruction.
	DynamicInstructionBuffer[] dynamicInstructionBuffer;

	int[] decodeWidth;
	int[] stepSize;
	static long[] noOfMicroOps;
	//long[] numInstructions;
	//FIXME PipelineInterface should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	PipelineInterface[] pipelineInterfaces;
	long prevTotalInstructions, currentTotalInstructions;
	long[] prevCycles;

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	@SuppressWarnings("unchecked")
	public RunnableThread(String threadName, int tid1, Core[] cores, TopLevelTokenBus tokenBus) {

		if (writeToFile) {
			try {
				fileOut = new FileOutputStream( fileName );
				bufferOut = new BufferedOutputStream( fileOut );
				output = new ObjectOutputStream( bufferOut );

				/*	    fileIn = new FileInputStream( "microOps.ser" );
		    bufferIn = new BufferedInputStream( fileIn );
		    input = new ObjectInputStream ( bufferIn );
				 */		}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.tokenBus = tokenBus;
		dynamicInstructionBuffer = new DynamicInstructionBuffer[EMUTHREADS];
		inputToPipeline = (GenericCircularQueue<Instruction> [])
								Array.newInstance(GenericCircularQueue.class, EMUTHREADS);
		
		decodeWidth = new int[EMUTHREADS];
		stepSize = new int[EMUTHREADS];
		noOfMicroOps = new long[EMUTHREADS];
		//numInstructions = new long[EMUTHREADS];
		pipelineInterfaces = new PipelineInterface[EMUTHREADS];
		for(int i = 0; i < EMUTHREADS; i++)
		{
			int id = tid1*EMUTHREADS+i;
			IpcBase.glTable.getStateTable().put(id, new ThreadState(id));
			threadParams[i] = new ThreadParams();

			//TODO pipelineinterfaces & inputToPipeline should also be in the IpcBase
			pipelineInterfaces[i] = cores[i].getPipelineInterface();
			inputToPipeline[i] = new GenericCircularQueue<Instruction>(
												Instruction.class, INSTRUCTION_THRESHOLD*10);
			
			dynamicInstructionBuffer[i] = new DynamicInstructionBuffer();
			
			GenericCircularQueue<Instruction>[] toBeSet =
													(GenericCircularQueue<Instruction>[])
													Array.newInstance(GenericCircularQueue.class, 1);
			toBeSet[0] = inputToPipeline[i];
			pipelineInterfaces[i].setInputToPipeline(toBeSet);

			if(cores[i].isPipelineInorder)
				decodeWidth[i] = 1;
			else if(cores[i].isPipelineMultiIssueInorder)
				decodeWidth[i] = 1;
			else
				decodeWidth[i] = cores[i].getDecodeWidth();

			stepSize[i] = cores[i].getStepSize();
		}

		/*		try
		{
			md5 = MessageDigest.getInstance("SHA");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		 */
		this.tid = tid1;
		System.out.println("--  starting java thread"+this.tid);
		prevTotalInstructions=-1;
		currentTotalInstructions=0;
//		threadCoreMaping = new Hashtable<Integer, Integer>();
		prevCycles=new long[EMUTHREADS];
	}

	protected void runPipelines() {
		int minN = Integer.MAX_VALUE;
		for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
			ThreadParams th = threadParams[tidEmu];
			if ( th.halted  && !(this.inputToPipeline[tidEmu].size() > INSTRUCTION_THRESHOLD)) {
					//|| th.packets.size() > PACKET_THRESHOLD)){
				th.halted = false;
			//	System.out.println("Halting over..!! "+tidEmu);
			}
			int n = inputToPipeline[tidEmu].size() / decodeWidth[tidEmu]
					* pipelineInterfaces[tidEmu].getCoreStepSize();
			if (n < minN && n != 0)
				minN = n;
		}
		// System.out.println("minN ="+minN);
		// System.out.println();
		minN = (minN == Integer.MAX_VALUE) ? 0 : minN;
		// if (currenmaxCoreAssign>1)
		//if (minN==0) System.out.println("min is"+minN + "0 pipeline size  : " +
		// inputToPipeline[0].getListSize());

		/*
		 * boolean print =false; for (int tidEmu=0; tidEmu<EMUTHREADS; tidEmu++)
		 * { if (inputToPipeline[tidEmu].getListSize()!=0 && minN==0) { print =
		 * true; } } if (true) { System.out.println("minN is "+minN); for(int
		 * tidEmu=0; tidEmu<EMUTHREADS; tidEmu++) {
		 * System.out.println("numInstructions in pipeline"
		 * +tidEmu+"  "+inputToPipeline
		 * [tidEmu].getListSize()+" thread.started is"
		 * +threadParams[tidEmu].started); } }
		 */
//		System.out.print("Pipe Size Start= "+inputToPipeline[0].getListSize()+ " " +minN/pipelineInterfaces[0].getCoreStepSize());
		for (int i1 = 0; i1 < minN; i1++) {
			for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
				pipelineInterfaces[tidEmu].oneCycleOperation();
			}
			if(tokenBus.getFrequency() > 0)
				tokenBus.eq.processEvents();
			GlobalClock.incrementClock();
		}
		if(prevTotalInstructions == -1){
			Statistics.openTraceStream();
			Statistics.printPowerTraceHeader(",");
			prevTotalInstructions=0;
		}
		//calculating power traces
		if(SimulationConfig.powerTrace==1){
			for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
				currentTotalInstructions += pipelineInterfaces[tidEmu].getCore().getNoOfInstructionsExecuted();
			}
			if(currentTotalInstructions - prevTotalInstructions > SimulationConfig.numInsForTrace){
				long[] cyclesElapsed = new long[maxCoreAssign];
				long currentCycles;
				Core currentCore;
				for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
					currentCore = pipelineInterfaces[tidEmu].getCore();
					currentCycles = GlobalClock.getCurrentTime()/currentCore.getStepSize();
					cyclesElapsed[tidEmu]=currentCycles-prevCycles[tidEmu];
					currentCore.powerCounters.updatePowerPeriodically(cyclesElapsed[tidEmu]);
					Statistics.setCoreFrequencies(currentCore.getFrequency(), currentCore.getCore_number());
					Statistics.setPerCorePowerStatistics(currentCore.powerCounters,currentCore.getCore_number());
				}
				Statistics.printPowerTrace(",", cyclesElapsed,maxCoreAssign);
				for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
					prevCycles[tidEmu]=GlobalClock.getCurrentTime()/pipelineInterfaces[tidEmu].getCoreStepSize();
					pipelineInterfaces[tidEmu].getCore().powerCounters.clearAccessStats();
				}
				prevTotalInstructions = currentTotalInstructions;
			}
			currentTotalInstructions=0;
		}
		else if(SimulationConfig.powerTrace==2){
			long cyclesTillNow = GlobalClock.getCurrentTime()/pipelineInterfaces[0].getCore().getStepSize();
			if(cyclesTillNow - prevCycles[0] > SimulationConfig.numCyclesForTrace){
				long[] cyclesElapsed = new long[maxCoreAssign];
				long currentCycles;
				Core currentCore;
					for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
						currentCore = pipelineInterfaces[tidEmu].getCore();
						currentCycles = GlobalClock.getCurrentTime()/currentCore.getStepSize();
						cyclesElapsed[tidEmu]=currentCycles-prevCycles[tidEmu];
						currentCore.powerCounters.updatePowerPeriodically(cyclesElapsed[tidEmu]);
						Statistics.setCoreFrequencies(currentCore.getFrequency(), currentCore.getCore_number());
						Statistics.setPerCorePowerStatistics(currentCore.powerCounters,currentCore.getCore_number());
				}
				Statistics.printPowerTrace(",", cyclesElapsed,maxCoreAssign);
				for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
					prevCycles[tidEmu]=GlobalClock.getCurrentTime()/pipelineInterfaces[tidEmu].getCoreStepSize();
					pipelineInterfaces[tidEmu].getCore().powerCounters.clearAccessStats();
				}
				prevCycles[0] = cyclesTillNow;
			}
		}
//		System.out.println("  Pipe Size end= "+inputToPipeline[0].getListSize());
		
	}

	public void finishAllPipelines() {

		for (int i=0; i<maxCoreAssign; i++){
			//finishing pipelines by adding invalid instruction to all pipeline
			//already finished pipeline will not be affected 
					this.inputToPipeline[i].enqueue(new Instruction(OperationType.inValid,null, null, null));
		}
		for (int i=0; i<maxCoreAssign; i++) {
			if (!pipelineInterfaces[i].isExecutionComplete() && pipelineInterfaces[i].isSleeping()) { 
				System.err.println("not completed for "+i);  //not supposed to be here 
				resumeSleep(IpcBase.glTable.resumePipelineTimer(i));
			}
		}

		boolean queueComplete;    //queueComplete is true when all cores have completed
		while(true)
		{
			//System.out.println("Pin completed ");
			queueComplete = true;        
			for(int i = 0; i < maxCoreAssign; i++)
			{
				queueComplete = queueComplete && pipelineInterfaces[i].isExecutionComplete();
			}
			if(queueComplete == true)
			{
				break;
			}

			//System.out.println(pipelineInterfaces[0].isExecutionComplete()+"  "+inputToPipeline[0].getListSize());
			/*double maxN=0;
			for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
				double n = (double)(inputToPipeline[tidEmu].getListSize() * pipelineInterfaces[tidEmu].getCoreStepSize()) / decodeWidth[tidEmu];
				if( n>maxN)
					maxN=n;
			}*/	
//			for (int i1=0; i1< maxN; i1++)	{
				for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
						pipelineInterfaces[tidEmu].oneCycleOperation();
				}
				if(tokenBus.getFrequency() > 0)
					tokenBus.eq.processEvents();
				GlobalClock.incrementClock();
				//Why it cant be change into a separate function
				if(SimulationConfig.powerTrace==1){
					for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
						currentTotalInstructions += pipelineInterfaces[tidEmu].getCore().getNoOfInstructionsExecuted();
					}
					if(currentTotalInstructions - prevTotalInstructions > SimulationConfig.numInsForTrace){
						long[] cyclesElapsed = new long[maxCoreAssign];
						long currentCycles;
						Core currentCore;
							for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
							currentCore = pipelineInterfaces[tidEmu].getCore();
							currentCycles = GlobalClock.getCurrentTime()/currentCore.getStepSize();
							cyclesElapsed[tidEmu]=currentCycles-prevCycles[tidEmu];
							currentCore.powerCounters.updatePowerPeriodically(cyclesElapsed[tidEmu]);
							Statistics.setCoreFrequencies(currentCore.getFrequency(), currentCore.getCore_number());
							Statistics.setPerCorePowerStatistics(currentCore.powerCounters,currentCore.getCore_number());
						}
						Statistics.printPowerTrace(",", cyclesElapsed,maxCoreAssign);
						for(int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++){
							prevCycles[tidEmu]=GlobalClock.getCurrentTime()/pipelineInterfaces[tidEmu].getCoreStepSize();
							pipelineInterfaces[tidEmu].getCore().powerCounters.clearAccessStats();
						}
						prevTotalInstructions = currentTotalInstructions;
					}
					currentTotalInstructions=0;
				}
				//change currentEMUTHREADS to maxcoreAssign if you use this
				else if(SimulationConfig.powerTrace==2){
					long cyclesTillNow = GlobalClock.getCurrentTime()/pipelineInterfaces[0].getCore().getStepSize();
					if(cyclesTillNow - prevCycles[0] > SimulationConfig.numCyclesForTrace){
						long[] cyclesElapsed = new long[currentEMUTHREADS];
						long currentCycles;
						Core currentCore;
							for(int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++){
								currentCore = pipelineInterfaces[tidEmu].getCore();
								currentCycles = GlobalClock.getCurrentTime()/currentCore.getStepSize();
								cyclesElapsed[tidEmu]=currentCycles-prevCycles[tidEmu];
								currentCore.powerCounters.updatePowerPeriodically(cyclesElapsed[tidEmu]);
								Statistics.setCoreFrequencies(currentCore.getFrequency(), currentCore.getCore_number());
								Statistics.setPerCorePowerStatistics(currentCore.powerCounters,currentCore.getCore_number());
						}
						Statistics.printPowerTrace(",", cyclesElapsed,currentEMUTHREADS);
						for(int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++){
							prevCycles[tidEmu]=GlobalClock.getCurrentTime()/pipelineInterfaces[tidEmu].getCoreStepSize();
							pipelineInterfaces[tidEmu].getCore().powerCounters.clearAccessStats();
						}
						prevCycles[0] = cyclesTillNow;
					}
				}
//			}
			//System.out.println("maxN is "+maxN);
		}
		
		//FIXME move inside the writeback stage
/*		Core core;
		for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
			core = pipelineInterfaces[tidEmu].getCore();
			if(core.isPipelineInorder && core.getExecutionEngineIn().getExecutionComplete()){
				//System.out.println("Setting statistics for core number = "+core.getCore_number()+"with step size= "+core.getStepSize());
				pipelineInterfaces[tidEmu].setTimingStatistics();			
				pipelineInterfaces[tidEmu].setPerCoreMemorySystemStatistics();
				pipelineInterfaces[tidEmu].setPerCorePowerStatistics();

			}
		}*/
		long dataRead = 0;
		long totNumIns = 0;
		long totMicroOps = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
			totMicroOps += noOfMicroOps[i];
			dataRead += threadParams[i].totalRead;
			//totNumIns += numInstructions[i];
		}
		long timeTaken = System.currentTimeMillis() - Main.startTime;
		System.out.println("\nThread" + tid + " Bytes-" + dataRead * 20
				//+ " instructions-" + numInstructions[tid] 
				                                     +" microOps  "+totMicroOps
				                                     +" MBPS-" + (double) (dataRead * 24)
				                                     / (double) timeTaken / 1000.0 +" time-"
				                                     + timeTaken +"\n microOp KIPS- "+ (double) totMicroOps / (double)timeTaken
				                                     +" KIPS-" + (double) totNumIns / (double) timeTaken
				                                     + "checksum " + sum + "\n");

		//		System.out.println("number of micro-ops = " + noOfMicroOps + "\t\t;\thash = " + makeDigest());
		if (writeToFile) {
			try {
				this.output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Statistics.setDataRead(dataRead, tid);
		//Statistics.setNumHandledCISCInsn(numInstructions, 0, tid);
		Statistics.setNoOfMicroOps(noOfMicroOps, tid);

		
		/*if (SimulationConfig.subsetSimulation)
		{
			Process process;
			String cmd[] = {"/bin/sh",
				      "-c",
				      "killall -9 " + Newmain.executableFile
			};

			try 
			{
				process = Runtime.getRuntime().exec(cmd);
				int ret = process.waitFor();
				System.out.println("ret : " + ret);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}*/
		
		IpcBase.free.release();
	}


	// returns true if all the emulator threads from which I was reading have
	// finished
	protected boolean emuThreadsFinished() {
		boolean ret = true;
		for (int i = 0; i < maxCoreAssign; i++) {
			ThreadParams thread = threadParams[i];
			if (thread.started == true
					&& thread.finished == false) {
				return false;
			}
		}
		return ret;
	}
	/*
	 * process each packet
	 * parameters - Thread information, packet, thread id
	 */
	protected void processPacket(ThreadParams thread, Packet pnew, int tidEmu) {
		if (doNotProcess) return;
		int tidApp = tid * EMUTHREADS + tidEmu;
		sum += pnew.value;
		if (pnew.value == TIMER) {//leaving timer packet now
			//resumeSleep(IpcBase.glTable.tryResumeOnWaitingPipelines(tidApp, pnew.ip)); 
			return;
		}
		if (pnew.value>SYNCHSTART && pnew.value<SYNCHEND) { //for barrier enter and barrier exit
			ResumeSleep ret = IpcBase.glTable.update(pnew.tgt, tidApp, pnew.ip, pnew.value);
			if(ret!=null){
				resumeSleep(ret);
			}
			return;
		}
		if(pnew.value == BARRIERINIT)  //for barrier initialization
		{
		
//			System.out.println("Packet is " + pnew.toString());
			BarrierTable.barrierListAdd(pnew);
			return;
		}
		if (thread.isFirstPacket) {
			this.pipelineInterfaces[tidApp].getCore().currentThreads++;  //current number of threads in this pipeline
			System.out.println("num of threads on core " + tidApp + " = " + this.pipelineInterfaces[tidApp].getCore().currentThreads);
			this.pipelineInterfaces[tidApp].getCore().getExecEngine().setExecutionComplete(false);
			currentEMUTHREADS ++;
			if(tidApp>=maxCoreAssign)
				maxCoreAssign = tidApp+1;
			
			thread.pold = pnew;
			thread.isFirstPacket=false;
		}
		if (pnew.ip == thread.pold.ip && !(pnew.value>6 && pnew.value<26)) {
			thread.packets.add(pnew);
		} else {
			//(numInstructions[tidEmu])++;
			this.dynamicInstructionBuffer[tidEmu].configurePackets(thread.packets);
			int oldLength = inputToPipeline[tidEmu].size();
			long numHandledInsn = ObjParser.fuseInstruction(thread.packets.get(0).ip, 
					dynamicInstructionBuffer[tidEmu], this.inputToPipeline[tidEmu]);
			Statistics.setNumHandledCISCInsn(
									Statistics.getNumHandledCISCInsn(0, tidEmu) + numHandledInsn,
									0,
									tidEmu);
			int newLength = inputToPipeline[tidEmu].size();
			
	//		if (ignoredInstructions < SimulationConfig.NumInsToIgnore)
	//			ignoredInstructions += newLength;
	//		else
				noOfMicroOps[tidEmu] += newLength - oldLength;
				
			
/*			if(SimulationConfig.detachMemSys == true)	//TODO
			{
				for(int i = 0; i < tempList.getListSize(); i++)
				{
					if(tempList.peekInstructionAt(i).getOperationType() == OperationType.load ||
							tempList.peekInstructionAt(i).getOperationType() == OperationType.store)
					{
						tempList.removeInstructionAt(i);
						i--;
					}
				}
			}
*/
			// Writing 20million instructions to a file
			if (writeToFile) {
				//use old length, new length
				if (noOfMicroOps[0]>numInsToWrite) doNotProcess=true;
				if (noOfMicroOps[0]>numInsToWrite && noOfMicroOps[0]< 20000005)
					System.out.println("Done writing to file");
				
				/*for(int i = oldLength; i < newLength; i++) {
					try {
						this.output.writeObject(this.inputToPipeline[tidEmu].peek(i));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}*/
				while(this.inputToPipeline[tidEmu].size() > 0)
				{
					Instruction toBeWritten = this.inputToPipeline[tidEmu].pollFirst();
					try {
						this.output.writeObject(toBeWritten);
						this.output.flush();// TODO if flush is being ignored, may have to close and open the stream
						CustomObjectPool.getInstructionPool().returnObject(toBeWritten);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				
//				int temmm = tempList.getListSize();
//				for (int i = 0; i < temmm; i++) {
//					//Newmain.instructionPool.returnObject(tempList.pollFirst());
//					tempList.peekInstructionAt(i).setSerialNo(noOfMicroOps[0]+i);
//				}
	//			if (ignoredInstructions >= SimulationConfig.NumInsToIgnore)
	//			{
//					int coreId = threadCoreMaping.get(tidEmu);
					if (!thread.halted && this.inputToPipeline[tidEmu].size() > INSTRUCTION_THRESHOLD) {
						thread.halted = true;
						//System.out.println("Halting "+tidEmu);
					}	
	//			}
	//			else
	//			{
	//				while(this.inputToPipeline[tidEmu].size() > 0)
	//				{
	//					Newmain.instructionPool.returnObject(this.inputToPipeline[tidEmu].pollFirst());
	//				}
	//			}
			
/*				if (currentEMUTHREADS>1)
				System.out.print("len["+tidEmu+"]="+this.inputToPipeline[tidEmu].length()+"\n");
*/					

				thread.pold = pnew;
				thread.packets.clear();
				thread.packets.add(thread.pold);
			}
			
			long temp=noOfMicroOps[tidEmu] % 1000000;
			if(temp < 5  && this.inputToPipeline[tidEmu].size() > 0) {
				System.out.println("number of micro-ops = " + noOfMicroOps[tidEmu]+" on core "+tidApp);
			}
		}

	}

	protected boolean poolExhausted() {
		return (CustomObjectPool.getInstructionPool().getNumIdle() < 2000);
	}

	private void resumeSleep(ResumeSleep update) {
		for (int i=0; i<update.getNumResumers(); i++) {
			//never used ... resuming handled within pipeline exec
//			System.out.println( "resuming "+threadCoreMaping.get(update.sleep.get(i)) + " -> " +update.sleep.get(i));
			this.pipelineInterfaces[update.resume.get(i)].resumePipeline();
		}
		for (int i=0; i<update.getNumSleepers(); i++) {
			Instruction ins = new Instruction(OperationType.sync,null, null, null);
			ins.setRISCProgramCounter(update.barrierAddress);
//			System.out.println( "sleeping "+threadCoreMaping.get(update.sleep.get(i)) + " -> " +update.sleep.get(i));
			this.inputToPipeline[update.sleep.get(i)].enqueue(ins);
			setThreadState(update.sleep.get(i), true);
		}
	}


	protected void signalFinish(int tidApp) {
		//finished pipline
		// TODO Auto-generated method stub
//		System.out.println("signalfinish thread " + tidApp + " mapping " + threadCoreMaping.get(tidApp));
		this.inputToPipeline[tidApp].enqueue(new Instruction(OperationType.inValid,null, null, null));
		IpcBase.glTable.getStateTable().get((Integer)tidApp).lastTimerseen = Long.MAX_VALUE;//(long)-1>>>1;
		//					System.out.println(tidApp+" pin thread got -1");
		
		//	FIXME threadParams should be on tidApp. Currently it is on tidEmu
		threadParams[tidApp].finished = true;

	}
	
	public static void setThreadState(int tid,boolean cond)
	{
//		System.out.println("set thread state halted" + tid + " to " + cond);
		threadParams[tid].halted = cond;
	}
}