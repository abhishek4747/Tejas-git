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
import java.util.ArrayList;

//import main.Main;
import main.CustomObjectPool;
import net.optical.TopLevelTokenBus;
import pipeline.PipelineInterface;
import pipeline.inorder.InorderExecutionEngine;
import config.SimulationConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.SharedMem;
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
public class RunnableThread implements Encoding, Runnable {
	
	TopLevelTokenBus tokenBus;

	public static final int INSTRUCTION_THRESHOLD = 2000;
	
	boolean doNotProcess = false;
	boolean writeToFile = SimulationConfig.writeToFile;
	long numInsToWrite = SimulationConfig.numInstructionsToBeWritten;
	String fileName = SimulationConfig.InstructionsFilename; //TODO take from config 
//	public static Hashtable<Integer,Integer> threadCoreMaping;

	OutputStream fileOut;
	OutputStream bufferOut;
	ObjectOutput output;

	int javaTid;
	long sum = 0; // checksum
	static int EMUTHREADS = IpcBase.getEmuThreadsPerJavaThread();
	int currentEMUTHREADS = 0;  //total number of livethreads
	int maxCoreAssign = 0;      //the maximum core id assigned 
	
	static EmulatorThreadState[] emulatorThreadState = new EmulatorThreadState[EMUTHREADS];

	GenericCircularQueue<Instruction>[] inputToPipeline;
	// static long ignoredInstructions = 0;

	// QQQ re-arrange packets for use by translate instruction.
	// DynamicInstructionBuffer[] dynamicInstructionBuffer;

	static long[] noOfMicroOps;
	//long[] numInstructions;
	//FIXME PipelineInterface should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	PipelineInterface[] pipelineInterfaces;
	long prevTotalInstructions, currentTotalInstructions;
	long[] prevCycles;
	
	IpcBase ipcBase;

	/*
	 * This keeps on reading from the appropriate index in the shared memory
	 * till it gets a -1 after which it stops. NOTE this depends on each thread
	 * calling threadFini() which might not be the case. This function will
	 * break if the threads which started do not call threadfini in the PIN (in
	 * case of unclean termination). Although the problem is easily fixable.
	 */
	public void run() {

		// create pool for emulator packets
		ArrayList<Packet> fromEmulator = new ArrayList<Packet>(SharedMem.COUNT);
		for (int i = 0; i < SharedMem.COUNT; i++) {
			fromEmulator.add(new Packet());
		}
		
		Packet pnew = new Packet();
		
		boolean allover = false;
		//boolean emulatorStarted = false;

		// start gets reinitialized when the program actually starts
		//Main.setStartTime(System.currentTimeMillis());
		EmulatorThreadState threadParam;
		// keep on looping till there is something to read. iterates on the
		// emulator threads from which it has to read.
		// tid is java thread id
		// tidEmu is the local notion of pin threads for the current java thread
		// tidApp is the actual tid of a pin thread
		while (true) {
			
			for (int tidEmulator = 0; tidEmulator < EMUTHREADS ; tidEmulator++) {

				threadParam = emulatorThreadState[tidEmulator];

				// Thread is halted on a barrier or a sleep
				if (threadParam.halted /*|| thread.finished*/) {
					continue;        //one bug need to be fixed to remove this comment
				}
				
				int tidApplication = javaTid * EMUTHREADS + tidEmulator;
				int numReads = 0;
				long v = 0;

				// get the number of packets to read. 'continue' and read from
				// some other thread if there is nothing.
				if ((numReads = ipcBase.fetchManyPackets(tidApplication, fromEmulator)) == 0) {
					continue;
				}
				
				// update the number of read packets
				threadParam.totalRead += numReads;
				
				// If java thread itself is terminated then break out from this
				// for loop. also update the variable all-over so that I can
				// break from the outer while loop also.
				if (ipcBase.javaThreadTermination[javaTid] == true) {
					allover = true;
					break;
				}

				// need to do this only the first time
				if (ipcBase.javaThreadStarted[javaTid]==false) {
					//emulatorStarted = true;
					//Main.setStartTime(System.currentTimeMillis());
					ipcBase.javaThreadStarted[javaTid] = true;
				}

				threadParam.checkStarted();

				// Do not process new packets till we have sufficient pool of available instructions
				while (poolExhausted()) {
					//System.out.println("infinte loop");
					runPipelines();
				}
				
				// Process all the packets read from the communication channel
				for (int i = 0; i < numReads; i++) {
					pnew = fromEmulator.get(i);
					v = pnew.value;
					processPacket(threadParam, pnew, tidEmulator);
				}
				
				// perform error check.
				ipcBase.errorCheck(tidApplication, threadParam.totalRead);

				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					System.out.println("runnableshm : last packet received for application-thread " + 
							tidApplication + " numCISC=" + pnew.ip);
					Statistics.setNumPINCISCInsn(pnew.ip, 0, tidEmulator);
					threadParam.isFirstPacket = true;  //preparing the thread for next packet in same pipeline
					signalFinish(tidApplication);
				}

				if (ipcBase.javaThreadTermination[javaTid] == true) {  //check if java thread is finished
					allover = true;
					break;
				}
			}
			
			runPipelines();
			// System.out.println("after execution n=  "+n+" Thread finished ? "+threadParams[1].finished);

			// this runnable thread can be stopped in two ways. Either the
			// emulator threads from which it was supposed to read never
			// started(none of them) so it has to be signalled by the main
			// thread. When this happens 'all over' becomes 'true' and it
			// breaks out from the loop. The second situation is that all the
			// emulator threads which started have now finished, so probably
			// this thread should now terminate.
			// The second condition handles this situation.
			// NOTE this ugly state management cannot be avoided unless we use
			// some kind of a signalling mechanism between the emulator and
			// simulator(TODO).
			// Although this should handle most of the cases.
			if (allover || (ipcBase.javaThreadStarted[javaTid]==true && emuThreadsFinished())) {
				ipcBase.javaThreadTermination[javaTid] = true;
				break;
			}
		}

		finishAllPipelines();
	}

//	void errorCheck(int tidApp, int emuid, int queue_size,
//			long numReads, long v) {
//		
//		// some error checking
//		// threadParams[emuid].totalRead += numReads;
//		long totalRead = threadParams[emuid].totalRead;
//		long totalProduced = ipcBase.totalProduced(tidApp);
//		
//		if (totalRead > totalProduced) {
//			System.out.println("numReads=" + numReads + " > totalProduced=" 
//					+ totalProduced + " !!");
//			
//			System.out.println("queue_size=" + queue_size);
//			System.exit(1);
//		}
//		
//		if (queue_size < 0) {
//			System.out.println("queue less than 0");
//			System.exit(1);
//		}
//	}


	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int javaTid, IpcBase ipcBase, 
			Core[] cores, TopLevelTokenBus tokenBus) {

		this.ipcBase = ipcBase;
		
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
		// dynamicInstructionBuffer = new DynamicInstructionBuffer[EMUTHREADS];
		inputToPipeline = (GenericCircularQueue<Instruction> [])
								Array.newInstance(GenericCircularQueue.class, EMUTHREADS);
		
		noOfMicroOps = new long[EMUTHREADS];
		//numInstructions = new long[EMUTHREADS];
		pipelineInterfaces = new PipelineInterface[EMUTHREADS];
		for(int i = 0; i < EMUTHREADS; i++)
		{
			int id = javaTid*EMUTHREADS+i;
			IpcBase.glTable.getStateTable().put(id, new ThreadState(id));
			emulatorThreadState[i] = new EmulatorThreadState();

			//TODO pipelineinterfaces & inputToPipeline should also be in the IpcBase
			pipelineInterfaces[i] = cores[i].getPipelineInterface();
			inputToPipeline[i] = new GenericCircularQueue<Instruction>(
												Instruction.class, INSTRUCTION_THRESHOLD*10);
			
			// dynamicInstructionBuffer[i] = new DynamicInstructionBuffer();
			
			GenericCircularQueue<Instruction>[] toBeSet =
													(GenericCircularQueue<Instruction>[])
													Array.newInstance(GenericCircularQueue.class, 1);
			toBeSet[0] = inputToPipeline[i];
			pipelineInterfaces[i].setInputToPipeline(toBeSet);
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
		this.javaTid = javaTid;
		System.out.println("--  starting java thread"+this.javaTid);
		prevTotalInstructions=-1;
		currentTotalInstructions=0;
//		threadCoreMaping = new Hashtable<Integer, Integer>();
		prevCycles=new long[EMUTHREADS];
		
		// Special case must be made for RunnableFromFile
		if(this.ipcBase != null) {
			(new Thread(this, threadName)).start();
		}
	}

	protected void runPipelines() {
		int minN = Integer.MAX_VALUE;
		for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
			EmulatorThreadState th = emulatorThreadState[tidEmu];
			if ( th.halted  && !(this.inputToPipeline[tidEmu].size() > INSTRUCTION_THRESHOLD)) {
					//|| th.packets.size() > PACKET_THRESHOLD)){
				th.halted = false;
			//	System.out.println("Halting over..!! "+tidEmu);
			}
			int n = inputToPipeline[tidEmu].size() / pipelineInterfaces[tidEmu].getCore().getDecodeWidth()
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
					this.inputToPipeline[i].enqueue(Instruction.getInvalidInstruction());
		}
		for (int i=0; i<maxCoreAssign; i++) {
			if (!pipelineInterfaces[i].isExecutionComplete() && pipelineInterfaces[i].isSleeping()) { 
				System.err.println("not completed for "+i);  //not supposed to be here 
				this.pipelineInterfaces[i].resumePipeline();
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
//		long totNuIns = 0;
//		long totMicroOps = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
//			totMicroOps += noOfMicroOps[i];
			dataRead += emulatorThreadState[i].totalRead;
			//totNumIns += numInstructions[i];
		}
		
//		long timeTaken = System.currentTimeMillis() - Main.getStartTime();
//		System.out.println("\nThread" + javaTid + " Bytes-" + dataRead * 20
//		   //+ " instructions-" + numInstructions[tid] 
//             +" microOps  "+ totMicroOps
//             +" MBPS-" + (double) (dataRead * 24)
//             / (double) timeTaken / 1000.0 +" time-"
//             + timeTaken +"\n microOp KIPS- "+ (double) totMicroOps / (double)timeTaken
//             +" KIPS-" + (double) totNumIns / (double) timeTaken
//             + "checksum " + sum + "\n");

		//		System.out.println("number of micro-ops = " + noOfMicroOps + "\t\t;\thash = " + makeDigest());
		if (writeToFile) {
			try {
				this.output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Statistics.setDataRead(dataRead, javaTid);
		//Statistics.setNumHandledCISCInsn(numInstructions, 0, tid);
		Statistics.setNoOfMicroOps(noOfMicroOps, javaTid);

		
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
			EmulatorThreadState thread = emulatorThreadState[i];
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
	protected void processPacket(EmulatorThreadState thread, Packet pnew, int tidEmu) {
		if (doNotProcess) return;
		int tidApp = javaTid * EMUTHREADS + tidEmu;
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
			// just append the packet to outstanding packets for current instruction pointer
			thread.packets.add(pnew);
		} else {
			//(numInstructions[tidEmu])++;
			//this.dynamicInstructionBuffer[tidEmu].configurePackets(thread.packets);
			
			int oldLength = inputToPipeline[tidEmu].size();
			
			long numHandledInsn = ObjParser.fuseInstruction(thread.packets.get(0).ip, 
					thread.packets, this.inputToPipeline[tidEmu]);
			
			Statistics.setNumHandledCISCInsn(
					Statistics.getNumHandledCISCInsn(javaTid, tidEmu) + numHandledInsn,
					javaTid, tidEmu);
			
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
/*		for (int i=0; i<update.getNumResumers(); i++) {
			//never used ... resuming handled within pipeline exec
//			System.out.println( "resuming "+threadCoreMaping.get(update.sleep.get(i)) + " -> " +update.sleep.get(i));
			this.pipelineInterfaces[update.resume.get(i)].resumePipeline();
		}
*/		for (int i=0; i<update.getNumSleepers(); i++) {
			Instruction ins = Instruction.getSyncInstruction();
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
		this.inputToPipeline[tidApp].enqueue(Instruction.getInvalidInstruction());
		IpcBase.glTable.getStateTable().get((Integer)tidApp).lastTimerseen = Long.MAX_VALUE;//(long)-1>>>1;
		//					System.out.println(tidApp+" pin thread got -1");
		
		//	FIXME threadParams should be on tidApp. Currently it is on tidEmu
		emulatorThreadState[tidApp].finished = true;

	}
	
	public static void setThreadState(int tid,boolean cond)
	{
//		System.out.println("set thread state halted" + tid + " to " + cond);
		emulatorThreadState[tid].halted = cond;
	}
}