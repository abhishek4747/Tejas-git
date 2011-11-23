/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Logger;
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
/* MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class RunnableThread implements Runnable, Encoding {

    private static final Logger logger = Logger.getLogger(RunnableThread.class);
	int tid;
	long sum = 0; // checksum
	int EMUTHREADS = IpcBase.EmuThreadsPerJavaThread;

	ThreadParams[] threadParams = new ThreadParams[EMUTHREADS];

	//inputToPipeline should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	InstructionLinkedList[] inputToPipeline;
	IpcBase ipcType;

	// QQQ re-arrange packets for use by translate instruction.
	DynamicInstructionBuffer dynamicInstructionBuffer;

	int[] decodeWidth;
	int[] stepSize;
	PipelineInterface[] pipelineInterfaces;

	public RunnableThread() {
	}

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int tid1, IpcBase ipcType, Core[] cores) {

		dynamicInstructionBuffer = new DynamicInstructionBuffer();
		inputToPipeline = new InstructionLinkedList[EMUTHREADS];
		decodeWidth = new int[EMUTHREADS];
		stepSize = new int[EMUTHREADS];
		pipelineInterfaces = new PipelineInterface[EMUTHREADS];
		for(int i = 0; i < EMUTHREADS; i++)
		{
			int id = tid1*IpcBase.EmuThreadsPerJavaThread+i;
			IpcBase.glTable.getStateTable().put(id, new ThreadState(id));
			threadParams[i] = new ThreadParams();

			//TODO pipelineinterfaces & inputToPipeline should also be in the IpcBase
			pipelineInterfaces[i] = cores[i].getPipelineInterface();
			inputToPipeline[i] = new InstructionLinkedList();
			cores[i].setInputToPipeline(new InstructionLinkedList[]{inputToPipeline[i]});

			if(cores[i].isPipelineInorder)
				decodeWidth[i] = 1;
			else
				decodeWidth[i] = cores[i].getDecodeWidth();

			stepSize[i] = cores[i].getStepSize();
		}

		this.tid = tid1;
		this.ipcType = ipcType;
		(new Thread(this, threadName)).start();
		logger.info("--  starting java thread"+this.tid);
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

		logger.info("-- in runnable thread run "+this.tid);

		Packet pnew = new Packet();
		boolean allover = false;
		boolean emulatorStarted = false;

		// start gets reinitialized when the program actually starts
		long start = System.currentTimeMillis();
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
					start = System.currentTimeMillis();
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

				int n = inputToPipeline[tidEmu].getListSize()/decodeWidth[tidEmu] * pipelineInterfaces[tidEmu].getCoreStepSize();
				for (int i1=0; i1< n; i1++)	{
					pipelineInterfaces[tidEmu].oneCycleOperation();
					if(!SimulationConfig.isPipelineInorder)
						GlobalClock.incrementClock();
				}

				thread.updateReaderLocation(numReads);
				queue_size = ipcType.update(tidApp, numReads);
				errorCheck(tidApp, tidEmu, queue_size, numReads, v);

				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					logger.info(tidApp+" pin thread got -1");
					thread.finished = true;
				}

				if (ipcType.termination[tid] == true) {
					allover = true;
					break;
				}
			}

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

		//this.inputToPipeline[0].appendInstruction(TestInstructionLists.testList2());

		boolean queueComplete;    //queueComplete is true when all cores have completed

		while(true)
		{
			//System.out.println("Pin completed ");
			queueComplete = true;        
			for(int i = 0; i < EMUTHREADS; i++)
			{

				queueComplete = queueComplete && pipelineInterfaces[i].isExecutionComplete();
			}
			if(queueComplete == true)
			{
				break;
			}

			for (int i=0; i<EMUTHREADS; i++) {
				pipelineInterfaces[i].oneCycleOperation();
			}

			GlobalClock.incrementClock();
		}


		long dataRead = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
			dataRead += threadParams[i].totalRead;
		}
		long timeTaken = System.currentTimeMillis() - start;
		System.out.println("\nThread" + tid + " Bytes-" + dataRead * 20
				+ " instructions-" + ipcType.numInstructions[tid] + " time-"
				+ timeTaken + " MBPS-" + (double) (dataRead * 24)
				/ (double) timeTaken / 1000.0 + " KIPS-"
				+ (double) ipcType.numInstructions[tid] / (double) timeTaken
				+ "checksum " + sum + "\n");

		IpcBase.free.release();
	}

	private void errorCheck(int tidApp, int emuid, int queue_size,
			int numReads, int v) {
		// some error checking
		long totalRead = threadParams[emuid].totalRead;
		totalRead += numReads;
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
		int tidApp = tid * EMUTHREADS + tidEmu;
		sum += pnew.value;
		//if (pnew.value >= SYNCHSTART || pnew.value <= SYNCHEND) return;
		if (pnew.value == TIMER) {
			tryResumeOnWaitingPipelines(tidApp, pnew.ip); 
			return;
		}
		if (thread.isFirstPacket) {
			thread.pold = pnew;
			thread.isFirstPacket=false;
		}
		if (pnew.ip == thread.pold.ip) {
			thread.packets.add(pnew);
		} else {
			if (thread.pold.value<=SYNCHSTART || thread.pold.value>=SYNCHEND) {
				(ipcType.numInstructions[tid])++;
				this.dynamicInstructionBuffer.configurePackets(thread.packets);
				InstructionLinkedList tempList = ObjParser.translateInstruction(thread.packets.get(0).ip, 
						dynamicInstructionBuffer);

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

				this.inputToPipeline[tidEmu].appendInstruction(tempList);

			}
			if (pnew.value>SYNCHSTART && pnew.value<SYNCHEND) {
				int threadToResume = IpcBase.glTable.update(pnew.tgt, tidApp, pnew.ip, pnew.value);
				if (threadToResume == -2) {
					//Means nobody sleeps, nobody resumes.
				}
				else {
					if (threadToResume != -1) {
						resumePipeline(threadToResume,pnew.value);
					}
					else {
						sleepPipeline(tidApp,threadToResume,pnew.value);
					}
				}
			}
			thread.pold = pnew;
			thread.packets.clear();
			thread.packets.add(thread.pold);
		}

	}

	private void tryResumeOnWaitingPipelines(int signaller, long time) {
		Hashtable<Integer, ThreadState> stateTable = IpcBase.glTable.getStateTable();
		ThreadState signallingThread = stateTable.get(signaller);
		signallingThread.lastTimerseen = time;

		for (PerAddressInfo pai : signallingThread.addressMap.values()) {
			for (Iterator<Integer> iter = pai.probableInteractors.iterator(); iter.hasNext();) {
				int waiter = (Integer)iter.next();
				ThreadState waiterThread = stateTable.get(waiter);
				if (waiterThread.timedWait) {
					//					synchronized (IpcBase.glTable.getStateTable()) { //TODO if multiple RunnableThreads
					if (time>=waiterThread.timeSlept(pai.address)) {
						//Remove dependencies from both sides.
						iter.remove();
						waiterThread.removeDep(signaller);
						// removeDep updates timedWait if needed
						if (!waiterThread.timedWait) resumePipeline(waiter,TIMER);
					}
					//					}
				}
			}
		}
	}

	private void sleepPipeline(int tidApp, int threadToResume, int encoding){
		if (encoding ==LOCK+1 || encoding==CONDWAIT+1) {
			// do not sleep at exits as they are already sleeping at the enters.
			// TODO add more here. as you handle more and more cases
			return;
		}
		else {
			System.out.println(tidApp+" pipeline is sleeping");
			this.inputToPipeline[tidApp].appendInstruction(new Instruction(OperationType.sync,null, null, null));
		}
	}
	private void resumePipeline(int tidApp, int encoding){
		// TODO remove entries from synchTable
		System.out.println(tidApp+" pipeline is resuming");
		this.pipelineInterfaces[tidApp].resumePipeline();
	}
}
