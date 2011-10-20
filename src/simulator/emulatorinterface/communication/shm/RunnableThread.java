/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface.communication.shm;

import static emulatorinterface.communication.shm.ApplicationThreads.threads;

import java.util.ArrayList;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.ApplicationThreads.appThread;
import generic.InstructionList;
/* MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class RunnableThread implements Runnable, Encoding {

	Thread runner;
	int tid;
	long sum = 0; // checksum
	long shmAddress = SharedMem.shmAddress; // shared memory address
	int COUNT = SharedMem.COUNT; // COUNT of packets per thread
	int EMUTHREADS = SharedMem.EMUTHREADS;
	int[] cons_ptr = new int[EMUTHREADS]; // consumer pointers
	long[] tot_cons = new long[EMUTHREADS]; // total consumed data

	InstructionList inputToPipeline;

	public RunnableThread() {
	}

	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableThread(String threadName, int tid1) {
		for (int i = tid1 * EMUTHREADS; i < (tid1 + 1) * EMUTHREADS; i++) {
			threads.add(i,new appThread());
		}
		inputToPipeline = new InstructionList();
		this.tid = tid1;
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
		// tid_emu is the actual tid of a pin thread
		while (true) {
			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
				int tidApp = tid * EMUTHREADS + tidEmu;
				thread = threads.get(tidApp);
				if (thread.halted || thread.finished)
					continue;
				listPackets = listPacketsList.get(tidEmu);
				pold = poldList[tidEmu];

				int queue_size, numReads = 0, v = 0;

				// get the number of packets to read. 'continue' and read from
				// some other thread if there is nothing.
				SharedMem.get_lock(tidApp, shmAddress, COUNT);
				queue_size = SharedMem.shmreadvalue(tidApp, shmAddress, COUNT,
						COUNT);
				SharedMem.release_lock(tidApp, shmAddress, COUNT);
				numReads = queue_size;
				if (numReads == 0) {
					continue;
				}

				// If java thread itself is terminated then break out from this
				// for loop. also update the variable allover so that I can 
				// break from the while loop also.
				if (SharedMem.termination[tid] == true) {
					allover = true;
					break;
				}

				// need to do this only the first time
				if (!emulatorStarted) {
					emulatorStarted = true;
					start = System.currentTimeMillis();
					SharedMem.started[tid] = true;
				}

				if (isFirstPacket[tidEmu]) {
					thread.started = true;
				}

				// Read the entries
				for (int i = 0; i < numReads; i++) {
					pnew = SharedMem.shmread(tidApp, shmAddress,
							(cons_ptr[tidEmu] + i) % COUNT, COUNT);
					if (handleSynch(pnew, tidApp))
						continue;
					v = processPacket(listPackets, pold, pnew, tidApp,
							isFirstPacket[tidEmu]);
				}
				// update the consumer pointer, queue_size.
				cons_ptr[tidEmu] = (cons_ptr[tidEmu] + numReads) % COUNT;
				SharedMem.get_lock(tidApp, shmAddress, COUNT);
				queue_size = SharedMem.shmreadvalue(tidApp, shmAddress, COUNT,
						COUNT);
				queue_size -= numReads;

				errorCheck(tidApp, tidEmu, queue_size, numReads, v);

				// update queue_size
				SharedMem
						.shmwrite(tidApp, shmAddress, COUNT, queue_size, COUNT);
				SharedMem.release_lock(tidApp, shmAddress, COUNT);

				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					System.out.println(tidApp+" pin thread got -1");
					thread.finished = true;
				}

				if (SharedMem.termination[tid] == true) {
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
				SharedMem.termination[tid] = true;
				break;
			}
		}

		long dataRead = 0;
		for (int i = 0; i < EMUTHREADS; i++) {
			dataRead += tot_cons[i];
		}
		long timeTaken = System.currentTimeMillis() - start;
		System.out.println("\nThread" + tid + " Bytes-" + dataRead * 20
				+ " instructions-" + SharedMem.numInstructions[tid] + " time-"
				+ timeTaken + " MBPS-" + (double) (dataRead * 24)
				/ (double) timeTaken / 1000.0 + " KIPS-"
				+ (double) SharedMem.numInstructions[tid] / (double) timeTaken
				+ "checksum " + sum + "\n");

		SharedMem.free.release();
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
			(SharedMem.numInstructions[tid])++;
			DynamicInstruction dynamicInstruction = configurePackets(
					listPackets, tid, appTid);
			pold = pnew;
			listPackets.clear();
			listPackets.add(pold);
		}
		if (isFirstPacket)
			isFirstPacket = false;
		return v;
	}

	private void errorCheck(int tid_emu, int emuid, int queue_size,
			int numReads, int v) {
		// some error checking
		tot_cons[emuid] += numReads;
		int tot_prod = SharedMem.shmreadvalue(tid_emu, shmAddress, COUNT + 4,
				COUNT);
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

	// false if the packet is not for synchronization. else true
	private boolean handleSynch(Packet p, int appTid) {
		if (p.value <= SYNCHSTART || p.value >= SYNCHEND)
			return false;
		SharedMem.glTable.update(p.tgt, appTid, p.ip, p.value);
		//TODO also inject a SYNCH instruction here
		return true;
	}

	private DynamicInstruction configurePackets(ArrayList<Packet> listPackets,
			int tid2, int tidEmu) {
		Packet p;
		ArrayList<Long> memReadAddr = new ArrayList<Long>();
		ArrayList<Long> memWriteAddr = new ArrayList<Long>();
		ArrayList<Long> srcRegs = new ArrayList<Long>();
		ArrayList<Long> dstRegs = new ArrayList<Long>();

		long ip = listPackets.get(0).ip;
		boolean taken = false;
		long branchTargetAddress = 0;
		for (int i = 0; i < listPackets.size(); i++) {
			p = listPackets.get(i);
			if (ip != p.ip)
				misc.Error.shutDown("IP mismatch " + ip + " " + p.ip + " " + i
						+ " " + listPackets.size());
			switch (p.value) {
			case (-1):
				break;
			case (MEMREAD):
				memReadAddr.add(p.tgt);
				break;
			case (MEMWRITE):
				memWriteAddr.add(p.tgt);
				break;
			case (TAKEN):
				taken = true;
				branchTargetAddress = p.tgt;
				break;
			case (NOTTAKEN):
				taken = false;
				branchTargetAddress = p.tgt;
				break;
			case (REGREAD):
				srcRegs.add(p.tgt);
				break;
			case (REGWRITE):
				dstRegs.add(p.tgt);
				break;
			default:
				misc.Error.shutDown("error in configuring packets" + p.value
						+ " size" + listPackets.size());
			}
		}

		return new DynamicInstruction(ip, tidEmu, taken, branchTargetAddress,
				memReadAddr, memWriteAddr, srcRegs, dstRegs);
	}

	public InstructionList getInputToPipeline() {
		return inputToPipeline;
	}
}