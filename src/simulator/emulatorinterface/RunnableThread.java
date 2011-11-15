/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface;

import static emulatorinterface.ApplicationThreads.applicationThreads;

import java.util.ArrayList;

import pipeline.PipelineInterface;

import config.SimulationConfig;

import emulatorinterface.ApplicationThreads.appThread;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.Encoding;
import emulatorinterface.translator.x86.objparser.ObjParser;
import emulatorinterface.translator.x86.objparser.TestInstructionLists;
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

	Thread runner;
	int tid;
	long sum = 0; // checksum
	int COUNT = IpcBase.COUNT; // COUNT of packets per thread
	int EMUTHREADS = IpcBase.EMUTHREADS;
	int[] readerLocation = new int[EMUTHREADS]; // consumer pointers
	long[] totalRead = new long[EMUTHREADS]; // total consumed data

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
			pipelineInterfaces[i] = cores[i].getPipelineInterface();
			inputToPipeline[i] = new InstructionLinkedList();
			cores[i].setInputToPipeline(new InstructionLinkedList[]{inputToPipeline[i]});
			
			if(cores[i].isPipelineInorder)
				decodeWidth[i] = 1;
			else
				decodeWidth[i] = cores[i].getDecodeWidth();
			
			stepSize[i] = cores[i].getStepSize();
		}

		for (int i = tid1 * EMUTHREADS; i < (tid1 + 1) * EMUTHREADS; i++) {
			applicationThreads.add(i,new appThread());
		}
		
		this.tid = tid1;
		this.ipcType = ipcType;
		runner = new Thread(this, threadName);
		if (SimulationConfig.debugMode) 
			System.out.println("--  starting java thread"+runner.getName());
		runner.start(); // Start the thread.
	}


	// returns true if all the emulator threads from which I was reading have
	// finished
	boolean emuThreadsFinished() {
		boolean ret = true;
		int start=tid*EMUTHREADS;
		appThread thread;
		for (int i = start; i < start+EMUTHREADS; i++) {
			thread = applicationThreads.get(i);
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
		
		if (SimulationConfig.debugMode) 
			System.out.println("-- in runnable thread run "+this.tid);
		
		ArrayList<ArrayList<Packet>> listPackets = new ArrayList<ArrayList<Packet>>();
		ArrayList<Packet> packets;
		Packet[] poldList = new Packet[EMUTHREADS];
		Packet pnew = new Packet();
		boolean allover = false;
		boolean emulatorStarted = false;

		boolean isFirstPacket[] = new boolean[EMUTHREADS];
		for (int i = 0; i < EMUTHREADS; i++) {
			isFirstPacket[i] = true;
			listPackets.add(i, new ArrayList<Packet>());
			poldList[i] = new Packet();
		}

		// start gets reinitialized when the program actually starts
		long start = System.currentTimeMillis();
		appThread thread;
		// keep on looping till there is something to read. iterates on the
		// emulator threads from which it has to read.
		// tid is java thread id
		// tidEmu is the local notion of pin threads for the current java thread
		// tid_emu/tidApp is the actual tid of a pin thread
		while (true) {
			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
				
				int tidApp = tid * EMUTHREADS + tidEmu;
				thread = applicationThreads.get(tidApp);
				if (thread.halted || thread.finished)
					continue;
				packets = listPackets.get(tidEmu);

				int queue_size, numReads = 0, v = 0;

				// get the number of packets to read. 'continue' and read from
				// some other thread if there is nothing.

				/*** ALL THAT YOU NEED ARE FIVE FUNCTIONS ***/
				/*** start, finish, isEmpty, fetchPacket, isTerminated ****/
				numReads = ipcType.numPackets(tidApp);
				if (numReads == 0) {
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
					stepSize[0] = pipelineInterfaces[0].getCoreStepSize();//TODO all cores
				}

				if (isFirstPacket[tidEmu]) {
					thread.started = true;
				}

				// Read the entries
				for (int i = 0; i < numReads; i++) {
					pnew = ipcType.fetchOnePacket(tidApp, readerLocation[tidEmu]+i);
					if (handleSynch(pnew, tidApp))
						continue;
					v = pnew.value;
					processPacket(packets, poldList, pnew, tidEmu,
							isFirstPacket);
					if (isFirstPacket[tidEmu])
						isFirstPacket[tidEmu] = false;
				}

				int n = inputToPipeline[tidEmu].getListSize()/decodeWidth[tidEmu] * pipelineInterfaces[tidEmu].getCoreStepSize();
				for (int i1=0; i1< n; i1++)

				{
					pipelineInterfaces[tidEmu].oneCycleOperation();
					GlobalClock.incrementClock();

				}
				
				// update the consumer pointer, queue_size.
				// add a function : update queue_size
				readerLocation[tidEmu] = (readerLocation[tidEmu] + numReads) % COUNT;

				queue_size = ipcType.update(tidApp, numReads);

				errorCheck(tidApp, tidEmu, queue_size, numReads, v);
				
				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					System.out.println(tidApp+" pin thread got -1");
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
			dataRead += totalRead[i];
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
		totalRead[emuid] += numReads;
		int tot_prod = ipcType.totalProduced(tidApp);
		// System.out.println("tot_prod="+tot_prod+" tot_cons="+tot_cons[emuid]+" v="+v+" numReads"+numReads);
		if (totalRead[emuid] > tot_prod) {
			System.out.println("numReads" + numReads + " queue_size"
					+ queue_size + " ip");
			System.out.println("tot_prod=" + tot_prod + " tot_cons="
					+ totalRead[emuid] + " v=" + v + " emuid" + emuid);
			System.exit(1);
		}
		if (queue_size < 0) {
			System.out.println("queue less than 0");
			System.exit(1);
		}
	}
	
	private void processPacket(ArrayList<Packet> listPackets, Packet[] poldList,
			Packet pnew, int tidEmu, boolean[] isFirstPacket) {
		sum += pnew.value;
		if (pnew.ip == poldList[tidEmu].ip || isFirstPacket[tidEmu]) {
			if (isFirstPacket[tidEmu])
				poldList[tidEmu] = pnew;
			listPackets.add(pnew);
		} else {
			(ipcType.numInstructions[tid])++;
			
			// QQQ Using local buffer to store packets
			this.dynamicInstructionBuffer.configurePackets(listPackets);
			
			// QQQ translate instructions

			InstructionLinkedList tempList = ObjParser.translateInstruction(listPackets.get(0).ip, 
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
			
			this.inputToPipeline[0].appendInstruction(tempList);

			poldList[tidEmu] = pnew;

			listPackets.clear();
			listPackets.add(poldList[tidEmu]);
		}
		
	}


	// false if the packet is not for synchronization. else true
	private boolean handleSynch(Packet p, int appTid) {
		if (p.value <= SYNCHSTART || p.value >= SYNCHEND)
			return false;
		IpcBase.glTable.update(p.tgt, appTid, p.ip, p.value);
		//TODO also inject a SYNCH instruction here
		
		return true;
	}
}
