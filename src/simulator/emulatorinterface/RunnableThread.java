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
import java.util.Hashtable;
import java.util.Iterator;
import pipeline.PipelineInterface;
import config.SimulationConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Core;
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

	private static final int INSTRUCTION_THRESHOLD = 10000;
	private static final int PACKET_THRESHOLD = 10000;
	
	
	boolean doNotProcess = false;
	boolean writeToFile = SimulationConfig.writeToFile;
	long numInsToWrite = SimulationConfig.numInstructionsToBeWritten;
	String fileName = SimulationConfig.InstructionsFilename; //TODO take from config 

	OutputStream fileOut;
	OutputStream bufferOut;
	ObjectOutput output;

	int tid;
	long sum = 0; // checksum
	int EMUTHREADS = IpcBase.EmuThreadsPerJavaThread;
	int currentEMUTHREADS = 0;
	
	ThreadParams[] threadParams = new ThreadParams[EMUTHREADS];

	InstructionLinkedList[] inputToPipeline;

	// QQQ re-arrange packets for use by translate instruction.
	DynamicInstructionBuffer[] dynamicInstructionBuffer;

	int[] decodeWidth;
	int[] stepSize;
	static long[] noOfMicroOps;
	long[] numInstructions;
	//FIXME PipelineInterface should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	PipelineInterface[] pipelineInterfaces;

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int tid1, Core[] cores) {

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
		dynamicInstructionBuffer = new DynamicInstructionBuffer[EMUTHREADS];
		inputToPipeline = new InstructionLinkedList[EMUTHREADS];
		
		decodeWidth = new int[EMUTHREADS];
		stepSize = new int[EMUTHREADS];
		noOfMicroOps = new long[EMUTHREADS];
		numInstructions = new long[EMUTHREADS];
		pipelineInterfaces = new PipelineInterface[EMUTHREADS];
		for(int i = 0; i < EMUTHREADS; i++)
		{
			int id = tid1*IpcBase.EmuThreadsPerJavaThread+i;
			IpcBase.glTable.getStateTable().put(id, new ThreadState(id));
			threadParams[i] = new ThreadParams();

			//TODO pipelineinterfaces & inputToPipeline should also be in the IpcBase
			pipelineInterfaces[i] = cores[i].getPipelineInterface();
			inputToPipeline[i] = new InstructionLinkedList();
			
			dynamicInstructionBuffer[i] = new DynamicInstructionBuffer();
			cores[i].setInputToPipeline(new InstructionLinkedList[]{inputToPipeline[i]});

			if(cores[i].isPipelineInorder)
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
	}

	protected void runPipelines() {
		int minN = Integer.MAX_VALUE;
		for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
			ThreadParams th = threadParams[tidEmu];
			if ( th.halted  && !(this.inputToPipeline[tidEmu].getListSize() > INSTRUCTION_THRESHOLD)) {
					//|| th.packets.size() > PACKET_THRESHOLD)){
				th.halted = false;
			//	System.out.println("Halting over..!! "+tidEmu);
			}
			int n = inputToPipeline[tidEmu].getListSize() / decodeWidth[tidEmu]
					* pipelineInterfaces[tidEmu].getCoreStepSize();
			if (n < minN && n != 0)
				minN = n;
		}
		// System.out.println("minN ="+minN);
		// System.out.println();
		minN = (minN == Integer.MAX_VALUE) ? 0 : minN;
		// if (currentEMUTHREADS>1)
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
		for (int i1 = 0; i1 < minN; i1++) {
			for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
				pipelineInterfaces[tidEmu].oneCycleOperation();
			}
			GlobalClock.incrementClock();
		}
	}

	public void finishAllPipelines() {


		for (int i=0; i<currentEMUTHREADS; i++)
			this.inputToPipeline[i].appendInstruction(new Instruction(OperationType.inValid,null, null, null));

		for (int i=0; i<currentEMUTHREADS; i++) {
			if (!pipelineInterfaces[i].isExecutionComplete() && pipelineInterfaces[i].isSleeping()) { 
				System.out.println("not completed for "+i);
				resumeSleep(IpcBase.glTable.resumePipelineTimer(i));
			}
		}

		boolean queueComplete;    //queueComplete is true when all cores have completed
		while(true)
		{
			//System.out.println("Pin completed ");
			queueComplete = true;        
			for(int i = 0; i < currentEMUTHREADS; i++)
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
				for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
						pipelineInterfaces[tidEmu].oneCycleOperation();
				}
				GlobalClock.incrementClock();
//			}
			//System.out.println("maxN is "+maxN);
		}
		
		Core core;
		for (int tidEmu = 0; tidEmu < currentEMUTHREADS; tidEmu++) {
			core = pipelineInterfaces[tidEmu].getCore();
			if(core.isPipelineInorder && core.getExecutionEngineIn().getExecutionComplete()){
				//System.out.println("Setting statistics for core number = "+core.getCore_number()+"with step size= "+core.getStepSize());
				pipelineInterfaces[tidEmu].setTimingStatistics();			
				pipelineInterfaces[tidEmu].setPerCoreMemorySystemStatistics();
			}
		}
		long dataRead = 0;
		long totNumIns = 0;
		long totMicroOps = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
			totMicroOps += noOfMicroOps[i];
			dataRead += threadParams[i].totalRead;
			totNumIns += numInstructions[i];
		}
		long timeTaken = System.currentTimeMillis() - Newmain.start;
		System.out.println("\nThread" + tid + " Bytes-" + dataRead * 20
				+ " instructions-" + numInstructions[tid] 
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
		Statistics.setNumInstructions(numInstructions, tid);
		Statistics.setNoOfMicroOps(noOfMicroOps, tid);

		IpcBase.free.release();
	}


	// returns true if all the emulator threads from which I was reading have
	// finished
	protected boolean emuThreadsFinished() {
		boolean ret = true;
		for (int i = 0; i < EMUTHREADS; i++) {
			ThreadParams thread = threadParams[i];
			if (thread.started == true
					&& thread.finished == false) {
				return false;
			}
		}
		return ret;
	}
	
	protected void processPacket(ThreadParams thread, Packet pnew, int tidEmu) {
		if (doNotProcess) return;
		int tidApp = tid * EMUTHREADS + tidEmu;
		sum += pnew.value;
		if (pnew.value == TIMER) {
			resumeSleep(IpcBase.glTable.tryResumeOnWaitingPipelines(tidApp, pnew.ip)); 
			return;
		}
		if (pnew.value>SYNCHSTART && pnew.value<SYNCHEND) {
			resumeSleep(IpcBase.glTable.update(pnew.tgt, tidApp, pnew.ip, pnew.value));
			return;
		}
		if (thread.isFirstPacket) {
			currentEMUTHREADS++;
			thread.pold = pnew;
			thread.isFirstPacket=false;
		}
		if (pnew.ip == thread.pold.ip) {
			thread.packets.add(pnew);
		} else {
			(numInstructions[tidEmu])++;
			this.dynamicInstructionBuffer[tidEmu].configurePackets(thread.packets);
			InstructionLinkedList tempList = ObjParser.translateInstruction(thread.packets.get(0).ip, 
					dynamicInstructionBuffer[tidEmu]);

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
				if (noOfMicroOps[0]>numInsToWrite) doNotProcess=true;
				if (noOfMicroOps[0]>numInsToWrite && noOfMicroOps[0]< 20000005)
					System.out.println("Done writing to file");
				for (Instruction ins : tempList.instructionLinkedList) {
					try {
						this.output.writeObject(ins);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				
				int temmm = tempList.getListSize();
				for (int i = 0; i < temmm; i++) {
					//Newmain.instructionPool.returnObject(tempList.pollFirst());
					tempList.peekInstructionAt(i).setSerialNo(noOfMicroOps[0]+i);
				}
				
				this.inputToPipeline[tidEmu].appendInstruction(tempList);

				if (!thread.halted && (this.inputToPipeline[tidEmu].getListSize() > INSTRUCTION_THRESHOLD)) {
						//|| thread.packets.size() > PACKET_THRESHOLD)) {
					thread.halted = true;
					//System.out.println("Halting "+tidEmu);
				}
			}
			
/*			if (currentEMUTHREADS>1)
			System.out.print("len["+tidEmu+"]="+this.inputToPipeline[tidEmu].length()+"\n");
*/				
			noOfMicroOps[tidEmu] += tempList.length();
			long temp=noOfMicroOps[tidEmu] % 1000000;
			if(temp < 5  && tempList.getListSize() > 0) {
				//System.out.println("number of micro-ops = " + noOfMicroOps[tidEmu]+" on core "+tidApp);
			}


			thread.pold = pnew;
			thread.packets.clear();
			thread.packets.add(thread.pold);
		}

	}

	protected boolean poolExhausted() {
		return (Newmain.instructionPool.getNumIdle() < 2000);
	}

	private void resumeSleep(ResumeSleep update) {
		for (int i=0; i<update.getNumResumers(); i++) {
			System.out.println("Resuming "+update.resume.get(i));
			this.pipelineInterfaces[update.resume.get(i)].resumePipeline();
		}
		for (int i=0; i<update.getNumSleepers(); i++) {
			System.out.println("Sleeping "+update.sleep.get(i));
			this.inputToPipeline[update.sleep.get(i)].appendInstruction(new Instruction(OperationType.sync,null, null, null));
		}
	}


	protected void signalFinish(int tidApp) {
		// TODO Auto-generated method stub
		this.inputToPipeline[tidApp].appendInstruction(new Instruction(OperationType.inValid,null, null, null));
		IpcBase.glTable.getStateTable().get((Integer)tidApp).lastTimerseen = Long.MAX_VALUE;//(long)-1>>>1;
		//					System.out.println(tidApp+" pin thread got -1");
		
		//	FIXME threadParams should be on tidApp. Currently it is on tidEmu
		threadParams[tidApp].finished = true;

	}
}