/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface;

import static emulatorinterface.ApplicationThreads.threads;

import java.util.ArrayList;

import config.SimulationConfig;

import emulatorinterface.ApplicationThreads.appThread;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.Encoding;
import generic.InstructionLinkedList;
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
	int[] cons_ptr = new int[EMUTHREADS]; // consumer pointers
	long[] tot_cons = new long[EMUTHREADS]; // total consumed data

	InstructionLinkedList inputToPipeline;
	IpcBase ipcType;
	public RunnableThread() {
	}

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int tid1, IpcBase ipcType) {
		for (int i = tid1 * EMUTHREADS; i < (tid1 + 1) * EMUTHREADS; i++) {
			threads.add(i,new appThread());
		}
		inputToPipeline = new InstructionLinkedList();
		this.tid = tid1;
		this.ipcType = ipcType;
		runner = new Thread(this, threadName);
		// System.out.println(runner.getName());
		runner.start(); // Start the thread.
	}

	private boolean expectingHalt(int tidEmu) {
		return threads.get(tidEmu).haltStates.size()!=0;
	}

	// returns true if all the emulator threads from which I was reading have
	// finished
	boolean emuThreadsFinished() {
		boolean ret = true;
		int start=tid*EMUTHREADS;
		appThread thread;
		for (int i = start; i < start+EMUTHREADS; i++) {
			thread = threads.get(i);
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
		
		ArrayList<ArrayList<Packet>> listPacketsList = new ArrayList<ArrayList<Packet>>();
		ArrayList<Packet> listPackets;
		Packet[] poldList = new Packet[EMUTHREADS];
		Packet pold;
		Packet pnew = new Packet();
		boolean allover = false;
		boolean emulatorStarted = false;

		boolean isFirstPacket[] = new boolean[EMUTHREADS];
		for (int i = 0; i < EMUTHREADS; i++) {
			isFirstPacket[i] = true;
			listPacketsList.add(i, new ArrayList<Packet>());
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
				
				if (SimulationConfig.debugMode) 
					System.out.println("--tidEmu "+tidEmu);
				
				int tidApp = tid * EMUTHREADS + tidEmu;
				thread = threads.get(tidApp);
				if (thread.halted || thread.finished)
					continue;
				listPackets = listPacketsList.get(tidEmu);
				pold = poldList[tidEmu];

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
				}

				if (isFirstPacket[tidEmu]) {
					thread.started = true;
				}

				// Read the entries
				for (int i = 0; i < numReads; i++) {
					pnew = ipcType.fetchOnePacket(tidApp, cons_ptr[tidEmu]+i);
					if (handleSynch(pnew, tidApp))
						continue;
					v = processPacket(listPackets, pold, pnew, tidApp,
							isFirstPacket[tidEmu]);
				}
				// update the consumer pointer, queue_size.
				// add a function : update queue_size
				cons_ptr[tidEmu] = (cons_ptr[tidEmu] + numReads) % COUNT;

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

		long dataRead = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
			dataRead += tot_cons[i];
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
		tot_cons[emuid] += numReads;
		int tot_prod = ipcType.totalProduced(tidApp);
		// System.out.println("tot_prod="+tot_prod+" tot_cons="+tot_cons[emuid]+" v="+v+" numReads"+numReads);
		if (tot_cons[emuid] > tot_prod) {
			System.out.println("numReads" + numReads + " queue_size"
					+ queue_size + " ip");
			System.out.println("tot_prod=" + tot_prod + " tot_cons="
					+ tot_cons[emuid] + " v=" + v + " emuid" + emuid);
			System.exit(1);
		}
		if (queue_size < 0) {
			System.out.println("queue less than 0");
			System.exit(1);
		}
	}
	
	private int processPacket(ArrayList<Packet> listPackets, Packet pold,
			Packet pnew, int appTid, boolean isFirstPacket) {
		int v;
		v = pnew.value;
		sum += v;
		if (pnew.ip == pold.ip || isFirstPacket) {
			if (isFirstPacket)
				pold = pnew;
			listPackets.add(pnew);
		} else {
			(ipcType.numInstructions[tid])++;
			DynamicInstruction dynamicInstruction = configurePackets(
					listPackets, tid, appTid);
			//TODO
			// translate Instruction append fusedInstructions to Runnable's ipTo pipe
			pold = pnew;
			listPackets.clear();
			listPackets.add(pold);
		}
		if (isFirstPacket)
			isFirstPacket = false;
		return v;
	}


	// false if the packet is not for synchronization. else true
	private boolean handleSynch(Packet p, int appTid) {
		if (p.value <= SYNCHSTART || p.value >= SYNCHEND)
			return false;
		IpcBase.glTable.update(p.tgt, appTid, p.ip, p.value);
		//TODO also inject a SYNCH instruction here
		return true;
	}

	public InstructionLinkedList getInputToPipeline() {
		return inputToPipeline;
	}
}
