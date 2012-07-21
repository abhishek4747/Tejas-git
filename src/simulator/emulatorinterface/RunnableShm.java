package emulatorinterface;

import java.util.ArrayList;

import config.SimulationConfig;

import net.optical.TopLevelTokenBus;

import config.SimulationConfig;

import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.SharedMem;
import generic.Core;

public class RunnableShm extends RunnableThread implements Runnable {

	IpcBase ipcType;

	public RunnableShm(String threadName, int tid1, IpcBase ipcType,
			Core[] cores, TopLevelTokenBus tokenBus) {
		super(threadName, tid1, cores, tokenBus);
		// TODO Auto-generated constructor stub
		this.ipcType = ipcType;
		(new Thread(this, threadName)).start();
	}

	/*
	 * This keeps on reading from the appropriate index in the shared memory
	 * till it gets a -1 after which it stops. NOTE this depends on each thread
	 * calling threadFini() which might not be the case. This function will
	 * break if the threads which started do not call threadfini in the PIN (in
	 * case of unclean termination). Although the problem is easily fixable.
	 */
	public void run() {

		// System.out.println("-- in runnable thread run "+this.tid);
		ArrayList<Packet> fromPIN = new ArrayList<Packet>(SharedMem.COUNT);
		// little pooling on its own.
		for (int i = 0; i < SharedMem.COUNT; i++) {
			fromPIN.add(new Packet());
		}
		
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
			long microOpsDone = 0;
			for (int i = 0; i  < EMUTHREADS; i++) {
				microOpsDone += noOfMicroOps[i];
			}

			for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {

				thread = threadParams[tidEmu];
				if(thread.packets.size() != 0){
					for(Packet p : thread.packets)
					{
						if(p.value > 9 && p.value <24)
							System.out.println("starting stage " + p);
					}
				}
				if (thread.halted || thread.finished) {
					continue;
				}
				int tidApp = tid * EMUTHREADS + tidEmu;
				int queue_size, numReads = 0;
				long v = 0;

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
				
				//subset simulation : 100M micro-ops
				
				
				if(SimulationConfig.subsetSimulation && microOpsDone > SimulationConfig.subsetSimSize)
				{
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

/*				// Read the entries
				boolean poolExhausted = false;
				for (int i = 0; i < numReads; i++) {
					if(Newmain.instructionPool.getNumIdle() < 30)
					{
						poolExhausted = true;
						break;
					}
					pnew = ipcType.fetchOnePacket(tidApp, thread.readerLocation
							+ i);
					v = pnew.value;
					processPacket(thread, pnew, tidEmu);
				}
				
				if(poolExhausted)
				{
					break;
				}
*/
				while (poolExhausted()) {
					//System.out.println("infinte loop");
					runPipelines();
				}
				
				// Read the entries
				ipcType.fetchManyPackets(tidApp, thread.readerLocation, numReads, fromPIN);
				for (int i = 0; i < numReads; i++) {
					pnew = fromPIN.get(i);
					v = pnew.value;
				//	System.out.println(pnew.toString());
					processPacket(thread,pnew,tidEmu);
				}
				
				// update reader location
				thread.readerLocation = (thread.readerLocation + numReads) % SharedMem.COUNT;
				
				queue_size = ipcType.update(tidApp, numReads);
				errorCheck(tidApp, tidEmu, queue_size, numReads, v);

				// if we read -1, this means this emulator thread finished.
				if (v == -1) {
					signalFinish(tidApp);
				}

				if (ipcType.termination[tid] == true) {
					allover = true;
					break;
				}
			}
			runPipelines();
			// System.out.println("after execution n=  "+n+" Thread finished ? "+threadParams[1].finished);

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

		finishAllPipelines();
	}

	void errorCheck(int tidApp, int emuid, int queue_size,
			int numReads, long v) {
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
}
