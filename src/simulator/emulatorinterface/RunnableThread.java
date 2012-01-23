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
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.Encoding;
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
public class RunnableThread implements Runnable, Encoding {

	boolean doNotProcess = false;
	boolean writeToFile = true;
	long numInsToWrite = 20000000;
	String fileName = "outFile.ser"; //TODO take from config 

	OutputStream fileOut;
	OutputStream bufferOut;
	ObjectOutput output;

	int tid;
	long sum = 0; // checksum
	int EMUTHREADS = IpcBase.EmuThreadsPerJavaThread;

	ThreadParams[] threadParams = new ThreadParams[EMUTHREADS];

	InstructionLinkedList[] inputToPipeline;
	IpcBase ipcType;

	// QQQ re-arrange packets for use by translate instruction.
	DynamicInstructionBuffer[] dynamicInstructionBuffer;

	int[] decodeWidth;
	int[] stepSize;
	long[] noOfMicroOps;
	long[] numInstructions;
	//FIXME PipelineInterface should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	PipelineInterface[] pipelineInterfaces;

	//	MessageDigest md5;

	public RunnableThread() {
	}

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int tid1, IpcBase ipcType, Core[] cores) {

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
		this.ipcType = ipcType;
		(new Thread(this, threadName)).start();
		System.out.println("--  starting java thread"+this.tid);
	}


	// returns true if all the emulator threads from which I was reading have
	// finished
	boolean emuThreadsFinished() {
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

	/*
	 * This keeps on reading from the appropriate index in the shared memory
	 * till it gets a -1 after which it stops. NOTE this depends on each thread
	 * calling threadFini() which might not be the case. This function will
	 * break if the threads which started do not call threadfini in the PIN (in
	 * case of unclean termination). Although the problem is easily fixable.
	 */
	public void run() {

		//		System.out.println("-- in runnable thread run "+this.tid);
		int extraCycles=0;
		Packet pnew = new Packet();
		boolean allover = false;
		boolean emulatorStarted = false;

		// start gets reinitialized when the program actually starts
		Newmain.start = System.currentTimeMillis();
		ThreadParams thread;
		// keep on looping till there is something to read. iterates on the
		// emulator threads from which it has to read.
		// tid is java thread id
		// tidEmu is the local notion of pin threads for the current java thread
		// tidApp is the actual tid of a pin thread
		while (true) {
			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
				thread = threadParams[tidEmu];
				if (thread.halted || thread.finished)
					continue;
				int tidApp = tid * EMUTHREADS + tidEmu;
				int queue_size, numReads = 0, v = 0;

				// get the number of packets to read. 'continue' and read from
				// some other thread if there is nothing.

				/*** start, finish, isEmpty, fetchPacket, isTerminated ****/
				if ((numReads = ipcType.numPackets(tidApp)) == 0) {
					continue;
				}

				// If java thread itself is terminated then break out from this
				// for loop. also update the variable allover so that I can 
				// break from$the while loop also.
				if (ipcType.termination[tid] == true) {
					allover = true;
					break;
				}
				/**** END ***/

				// need to do this only the first time
				if (!emulatorStarted) {
					emulatorStarted = true;
					Newmain.start = System.currentTimeMillis();
					ipcType.started[tid] = true;
					for (int i = 0; i < EMUTHREADS; i++) {
						stepSize[i] = pipelineInterfaces[i].getCoreStepSize();
					}
				}

				thread.checkStarted();

				// Read the entries
				for (int i = 0; i < numReads; i++) {
					pnew = ipcType.fetchOnePacket(tidApp, thread.readerLocation+i);					
					v = pnew.value;
					processPacket(thread, pnew,tidEmu);
				}



				thread.updateReaderLocation(numReads);
				queue_size = ipcType.update(tidApp, numReads);
				errorCheck(tidApp, tidEmu, queue_size, numReads, v);

				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					this.inputToPipeline[tidApp].appendInstruction(new Instruction(OperationType.inValid,null, null, null));
					IpcBase.glTable.getStateTable().get((Integer)tidApp).lastTimerseen = Long.MAX_VALUE;//(long)-1>>>1;
					//					System.out.println(tidApp+" pin thread got -1");
					thread.finished = true;
				}

				if (ipcType.termination[tid] == true) {
					allover = true;
					break;
				}
			}
			int tempu=0;
			int minN=Integer.MAX_VALUE;
			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
				thread = threadParams[tidEmu];
				int n = inputToPipeline[tidEmu].getListSize()/decodeWidth[tidEmu] * pipelineInterfaces[tidEmu].getCoreStepSize();
				if(tidEmu==0)
					tempu=n;
				//FIXME what if core not started
				/*				if(tidEmu==0)
					System.out.println("n = "+n);
				 */				if(thread.started  &&  n<minN && n!=0)
					 minN=n;
				 //	System.out.print("  "+n);
			}
			//System.out.println("minN ="+minN);
			//System.out.println();
			minN = (minN==Integer.MAX_VALUE) ? 0 : minN;
			//System.out.println("min is"+minN + " pipeline size  : " + inputToPipeline[0].getListSize());
			if (minN==tempu &&extraCycles!=-1){ extraCycles+=minN;
			//			System.out.println("Extra cycles = "+extraCycles);
			}
			else 
				extraCycles = -1;
			/*boolean print =false;
			for (int tidEmu=0; tidEmu<EMUTHREADS; tidEmu++) {
				if (inputToPipeline[tidEmu].getListSize()!=0 && minN==0) {
					print = true;
				}
			}
			if (true) {
				System.out.println("minN is "+minN);
				for(int tidEmu=0; tidEmu<EMUTHREADS; tidEmu++) {
					System.out.println("numInstructions in pipeline"+tidEmu+"  "+inputToPipeline[tidEmu].getListSize()+" thread.started is"+threadParams[tidEmu].started);
				}
			}*/
			for (int i1=0; i1< minN; i1++)	{
				for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
					if(threadParams[tidEmu].started)
						pipelineInterfaces[tidEmu].oneCycleOperation();
				}
				GlobalClock.incrementClock();
			}
			//System.out.println("after execution n=  "+n+" Thread finished ? "+threadParams[1].finished);

			// this runnable thread can be stopped in two ways. Either the
			// emulator threads from which it was supposed to read never
			// started(none of them) so it has to be signalled by the main
			// thread. When this happens 'allover' becomes 'true' and it
			// breaks out from the loop. The second situation is that all the
			// emulator threads which started have now finished, so probably
			// this thread should now terminate.
			// The second condition handles this situation.
			// NOTE this ugly state management cannot be avoided unless we use
			// some kind of a signalling mechanism between the emulator and
			// simulator(TODO).
			// Although this should handle most of the cases.
			if (allover || (emulatorStarted && emuThreadsFinished())) {
				ipcType.termination[tid] = true;
				break;
			}
		}

		for (int i=0; i<EMUTHREADS; i++)
			this.inputToPipeline[i].appendInstruction(new Instruction(OperationType.inValid,null, null, null));

		for (int i=0; i<EMUTHREADS; i++) {
			if (!pipelineInterfaces[i].isExecutionComplete() && pipelineInterfaces[i].isSleeping()) { 
				System.out.println("not completed for "+i);
				resumePipelineTimer(i);
			}
		}

		boolean queueComplete;    //queueComplete is true when all cores have completed
		while(true)
		{
			//System.out.println("Pin completed ");
			queueComplete = true;        
			for(int i = 0; i < EMUTHREADS && threadParams[i].started; i++)
			{

				queueComplete = queueComplete && pipelineInterfaces[i].isExecutionComplete();
			}
			if(queueComplete == true)
			{
				break;
			}

			//System.out.println(pipelineInterfaces[0].isExecutionComplete()+"  "+pipelineInterfaces[1].isExecutionComplete());
			int maxN=0;
			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
				thread = threadParams[tidEmu];
				int n = inputToPipeline[tidEmu].getListSize()/decodeWidth[tidEmu] * pipelineInterfaces[tidEmu].getCoreStepSize();
				if( n>maxN)
					maxN=n;
			}	
			for (int i1=0; i1< maxN; i1++)	{
				for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
					if(threadParams[tidEmu].started)
						pipelineInterfaces[tidEmu].oneCycleOperation();
				}
				GlobalClock.incrementClock();
			}

		}
		Core core;
		for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
			core = pipelineInterfaces[tidEmu].getCore();
			if(core.getExecutionEngineIn().getExecutionComplete()){
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

	private void errorCheck(int tidApp, int emuid, int queue_size,
			int numReads, int v) {
		// some error checking
		threadParams[emuid].totalRead += numReads;
		long totalRead = threadParams[emuid].totalRead;
		int totalProduced = ipcType.totalProduced(tidApp);
		// System.out.println("tot_prod="+tot_prod+" tot_cons="+tot_cons[emuid]+" v="+v+" numReads"+numReads);
		if (totalRead > totalProduced) {
			System.out.println("numReads" + numReads + " queue_size"
					+ queue_size + " ip");
			System.out.println("tot_prod=" + totalProduced + " tot_cons="
					+ totalRead + " v=" + v + " emuid" + emuid);
			System.exit(1);
		}
		if (queue_size < 0) {
			System.out.println("queue less than 0");
			System.exit(1);
		}
	}

	private void processPacket(ThreadParams thread, Packet pnew, int tidEmu) {
		if (doNotProcess) return;
		int tidApp = tid * EMUTHREADS + tidEmu;
		sum += pnew.value;
		if (pnew.value == TIMER) {
			tryResumeOnWaitingPipelines(tidApp, pnew.ip); 
			return;
		}
		if (pnew.value>SYNCHSTART && pnew.value<SYNCHEND) {
			resumeSleep(IpcBase.glTable.update(pnew.tgt, tidApp, pnew.ip, pnew.value));
			return;
		}
		if (thread.isFirstPacket) {
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
			noOfMicroOps[tidEmu] += tempList.length();

			if(SimulationConfig.detachMemSys == true)	//TODO
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
				this.inputToPipeline[tidEmu].appendInstruction(tempList);
			}
			/*			//compute hash
			StringBuilder sb = new StringBuilder();				
			for(int i = 0; i < tempList.getListSize(); i++)
			{
				sb.append(tempList.peekInstructionAt(i).getProgramCounter());
				sb.append(tempList.peekInstructionAt(i).getOperationType());
				//System.out.println(tempList.peekInstructionAt(i).getProgramCounter()
				//		+ "\t;\t" + tempList.peekInstructionAt(i).getOperationType());
			}

  			if(tempList.getListSize() > 0)
			{
				//System.out.println(sb);
				md5.update(sb.toString().getBytes());
			}
			 */
			long temp=noOfMicroOps[tidEmu] % 1000000;
			if(temp < 5  && tempList.getListSize() > 0) {
				//System.out.println("number of micro-ops = " + noOfMicroOps[tidEmu]+" on core "+tidApp);
			}


			thread.pold = pnew;
			thread.packets.clear();
			thread.packets.add(thread.pold);
		}

	}

	/*	private String makeDigest()
	{
		byte messageDigest[] = md5.digest();
		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < messageDigest.length; i++)
		{
			String hex = Integer.toHexString(0xFF & messageDigest[i]);
			if (hex.length() == 1)
			{
				hexString.append('0');
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}
	 */
	private void tryResumeOnWaitingPipelines(int signaller, long time) {
		Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable.getStateTable();
		Hashtable<Long, SynchPrimitive> synchTable = IpcBase.glTable.getSynchTable();
		ThreadState signallingThread = stateTable.get(signaller);
		signallingThread.lastTimerseen = time;

		for (PerAddressInfo pai : signallingThread.addressMap.values()) {
			for (Iterator<Integer> iter = pai.probableInteractors.iterator(); iter.hasNext();) {
				int waiter = (Integer)iter.next();
				ThreadState waiterThread = stateTable.get(waiter);
				if (waiterThread.isOntimedWaitAt(pai.address)) {
					//TODO if multiple RunnableThreads then this should be synchronised
					if (time>=waiterThread.timeSlept(pai.address)) {
						//Remove dependencies from both sides.
						iter.remove();
						waiterThread.removeDep(signaller);
						if (!waiterThread.isOntimedWait()) {
							//TODOthis means waiter got released from a timedWait by a timer and not by synchPrimitive.
							//this means that in the synchTable somewhere there is a stale entry of their lockEnter/Exit
							// or unlockEnter. which needs to removed.
							// flushSynchTable();
							/*							System.out.println(waiter+" pipeline is resuming by timedWait from"+signaller
									+" num of Times"+stateTable.get(waiter).countTimedSleep);
							 */							resumePipelineTimer(waiter);
							 PerAddressInfo p = waiterThread.addressMap.get(pai.address);
							 if (p!=null) {
								 if (p.on_broadcast) {
									 resumeSleep(synchTable.get(pai.address).broadcastResume(p.broadcastTime,waiter));
									 p.on_broadcast = false;
									 p.broadcastTime = Long.MAX_VALUE;
								 }
								 else if (p.on_barrier) {
									 resumeSleep(synchTable.get(pai.address).barrierResume());
									 p.on_barrier = false;
								 }
							 }
						}
					}
				}
				else {
					// this means that the thread was not timedWait anymore as it got served by the synchronization
					// it was waiting for.
					iter.remove();
				}
			}
		}
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


	@SuppressWarnings("unused")
	private void sleepPipeline(int tidApp, int encoding) {
		this.inputToPipeline[tidApp].appendInstruction(new Instruction(OperationType.sync,null, null, null));
	}

	private void resumePipelineTimer(int tidToResume) {
		int numResumes=IpcBase.glTable.getStateTable().get(tidToResume).countTimedSleep;
		IpcBase.glTable.getStateTable().get(tidToResume).countTimedSleep=0;
		for (int i=0; i<numResumes; i++) {
			System.out.println("Resuming by timer"+tidToResume);
			this.pipelineInterfaces[tidToResume].resumePipeline();
		}
	}
}