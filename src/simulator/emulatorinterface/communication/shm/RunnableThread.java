/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface.communication.shm;

import java.io.IOException;
import java.util.Vector;

import config.SimulationConfig;

import emulatorinterface.DynamicInstruction;
import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.Newmain;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.InstructionList;
import generic.NewEventQueue;
import generic.OperationType;
import generic.Statistics;

/* MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class RunnableThread implements Runnable {

	Thread runner;
	int tid;
	int read_count=0;
	long sum=0;	//checksum
	long ibuf = SharedMem.ibuf;	//shared memory address
	int COUNT = SharedMem.COUNT;	// COUNT of packets per thread
	int EMUTHREADS = SharedMem.EMUTHREADS;
	int[] cons_ptr = new int[EMUTHREADS];	//consumer pointers
	long[] tot_cons = new long[EMUTHREADS];	// total consumed data
	boolean[] overstatus = new boolean[EMUTHREADS];
	boolean[] emuThreadStartStatus = new boolean[EMUTHREADS];
	long noOfMicroOps;
	NewEventQueue eventQ;
	Core[] cores;
	
	long noOfInstructionsArrived =0; //For testing purposes

	DynamicInstructionBuffer passPackets;
	InstructionList inputToPipeline;

	public RunnableThread() {
	}

	// initialise a reader thread with the correct thread id and the buffer to write the results in.
	public RunnableThread(String threadName, int tid1, DynamicInstructionBuffer pp, NewEventQueue eventQ, Core[] cores) {
		tid =tid1;
		passPackets = pp;
		for (int i=0; i<EMUTHREADS; i++) {
			emuThreadStartStatus[i] = false;
			overstatus[i] = false;
		}
		inputToPipeline = new InstructionList();
		this.eventQ = eventQ;
		this.cores = cores;
		noOfMicroOps = 0;
		runner = new Thread(this, threadName);
		//System.out.println(runner.getName());
		runner.start(); //Start the thread.
	}

	// returns true if all the emulator threads from which I was reading have finished
	boolean emuThreadsFinished() {
		boolean ret = true;
		for (int i=0; i<EMUTHREADS; i++) {
			if (emuThreadStartStatus[i]==true && overstatus[i]==false) {
				return false;
			}
		}
		return ret;
	}

	/* This keeps on reading from the appropriate index in the shared memory till it gets a -1
	 * after which it stops.
	 * NOTE this depends on each thread calling threadFini() which might not be
	 * the case. This function will break if the threads which started do not call threadfini
	 * in the PIN (in case of unclean termination). Although the problem is easily fixable.
	 */
	public void run() {
		
		Vector<Packet> vectorPacket = new Vector<Packet>();
		Packet pold = new Packet();
		Packet pnew;
		boolean allover = false;
		int tid_emu = -1;
		boolean emulatorStarted = false;
		boolean pipelineCommenced = false;
		boolean pipelineDone = false;		/* - set to true to detach pipeline - */
		
		boolean subsetSimulation = false;	/* - for pipeline of instructions 2000000 - 12000000 - */
		boolean memSystemDetach = false;	/* to detach memory system */
		long insCtr = 0;
		long s = 0, e = 0;
		
		int noOfFusedInstructions = 0;
		
		
		//FIXME:
		boolean breakLoop = false;
		
		boolean isFirstPacket[] = new boolean[EMUTHREADS];
		for (int i=0; i<EMUTHREADS; i++) {
			isFirstPacket[i] = true;
		}
		
		// start gets reinitialized when the program actually starts
		long start = System.currentTimeMillis();
		
		// keep on looping till there is something to read. iterates on the emulator threads from
		// which it has to read.
		long noOfInstr = 0;
		boolean toExit = false;
		
		InstructionList bufferedInstructions = null;
		
		while(true && breakLoop==false)
		{
			for (int emuid=0; (emuid < EMUTHREADS) & (breakLoop==false); emuid++) 
			{
				
				tid_emu = tid*EMUTHREADS+emuid;	// the actual tid of a PIN thread, from which I will read now
				
				if (overstatus[emuid]) continue;
				int queue_size, numReads=0,v=0;

				// get the number of packets to read. 'continue' and read from some
				//other thread if there is nothing.
				SharedMem.get_lock(tid_emu,ibuf, COUNT);
				queue_size = SharedMem.shmreadvalue(tid_emu,ibuf,COUNT,COUNT);
				SharedMem.release_lock(tid_emu,ibuf, COUNT);
				numReads = queue_size;
				if(numReads == 0)	
				{
					continue;
				}
				
				// If java thread itself is terminated then break out from this for loop.
				// also update the variable allover so that I can break from the while loop also.
				if (SharedMem.termination[tid]==true) {
					allover = true;
					break;
				}
				
				// need to do this only the first time
				if (!emulatorStarted) {
					emulatorStarted=true;
					start = System.currentTimeMillis();
					SharedMem.started[tid]=true;
				}
				
				if (isFirstPacket[emuid]) {
					emuThreadStartStatus[emuid]=true;
				}
				
				// Read the entries. The packets belonging to the same instruction are added
				// in a vector and passed to the DynamicInstructionBuffer which then processes it.
				for(int i=0 ; (i < numReads)  && (breakLoop==false); i++ ) 
				{
					pnew = SharedMem.shmread(tid_emu,ibuf,(cons_ptr[emuid] + i) %COUNT,COUNT );
					v = pnew.value;
					read_count ++;
					sum += v;
					if (pnew.ip == pold.ip || isFirstPacket[emuid]) {
						if (isFirstPacket[emuid]) pold = pnew;
						vectorPacket.add(pnew);
					}
					else 
					{
						(SharedMem.numInstructions[tid])++;
						
						//passPackets.configurePackets(vectorPacket,tid,tid_emu);
						DynamicInstruction dynamicInstruction = configurePackets(vectorPacket, tid, tid_emu);
						
						
												
						//TODO This instructionList must be provided to raj's code
						InstructionList fusedInstructions = null;
						Newmain.instructionCount ++;
						
						//gobble instructions till some time
//						if(Newmain.instructionCount >= 10000)
						{
							fusedInstructions = ObjParser.translateInstruction(SharedMem.insTable, pold.ip, dynamicInstruction);
						}
						
						//break loop after one million instructions
						if(Newmain.instructionCount>=5010000)
						{
//							breakLoop=true;
						}
						
						if(fusedInstructions != null && pipelineDone == false)
						{
							//if this is the first instruction, then pipeline needs
							//to be commenced
							if(pipelineCommenced == false && insCtr > cores[0].getDecodeWidth())
							{
								for(int i1 = 0; i1 < cores.length; i1++)
								{
									cores[i1].boot();
								}
								pipelineCommenced = true;
								GlobalClock.setCurrentTime(0);
								if(subsetSimulation == true) /* - for pipeline of instructions 2000000 - 12000000 - */
								{
									s = System.currentTimeMillis();
								}
							}
							
							noOfFusedInstructions += fusedInstructions.getListSize();
							
							long listSize;
							
							//add fused instructions to the input to the pipeline
							/* - for pipeline of instructions 2000000 - 12000000 - */
							if(subsetSimulation == false || noOfMicroOps > 2000000 && insCtr < 10000000)
							{
								for(int i3 = 0; i3 < fusedInstructions.getListSize(); i3++)
								{
									/* - to disconnect memory system - */
									if(memSystemDetach == false ||
											fusedInstructions.peekInstructionAt(i3).getOperationType() != OperationType.load &&
											fusedInstructions.peekInstructionAt(i3).getOperationType() != OperationType.store)
									{
										inputToPipeline.appendInstruction(fusedInstructions.peekInstructionAt(i3));
										insCtr++;
									}
								}
							}
							
							listSize = inputToPipeline.getListSize();
							
							for(int i2 = 0; i2 < listSize/cores[0].getDecodeWidth()*cores[0].getStepSize(); i2++)
							{
								eventQ.processEvents();
								
								GlobalClock.incrementClock();
							}
							
							/* - for pipeline of instructions 2000000 - 12000000 - */
							if(subsetSimulation == true && insCtr > 10000000)
							{
								inputToPipeline.appendInstruction(new Instruction(OperationType.inValid, null, null, null));
								
								while(eventQ.isEmpty() == false)
								{
									eventQ.processEvents();
									
									GlobalClock.incrementClock();
								}
								
								e = System.currentTimeMillis();
								long t = e - s;
								System.out.println("time for 10000000 microps = " + t);
								Statistics.setSubsetTime(t);
								pipelineDone = true;
							}
							
							noOfMicroOps += fusedInstructions.getListSize();
							
						}
						
						pold = pnew;
						vectorPacket.clear();
						vectorPacket.add(pold);
					}
					if (isFirstPacket[emuid]) isFirstPacket[emuid] = false;
				}

				// update the consumer pointer, queue_size.
				cons_ptr[emuid] = (cons_ptr[emuid] + numReads) % COUNT;
				SharedMem.get_lock(tid_emu,ibuf, COUNT);
				queue_size = SharedMem.shmreadvalue(tid_emu,ibuf,COUNT,COUNT);
				queue_size -= numReads;
				
				// some error checking
				tot_cons[emuid] += numReads;
				long tot_prod = SharedMem.shmreadvalue(tid_emu,ibuf,COUNT+4,COUNT);
				
//				if (SharedMem.numInstructions[tid] > 50000000)
//				{
//					toExit = true;
//					break;
//				}
				
				/*if(tot_cons[emuid] > tot_prod) {
					System.out.println("tot_prod = " + tot_prod + " tot_cons = " + tot_cons[emuid] + " v = " + v);
					System.exit(1);
				}*/
				if(queue_size < 0) 
				{
					System.out.println("queue less than 0");
					System.exit(1);
				}
				
				//update queue_size
				SharedMem.shmwrite(tid_emu,ibuf,COUNT,queue_size,COUNT);
				SharedMem.release_lock(tid_emu,ibuf, COUNT);

				// if we read -1, this means this emulator thread finished.
				if(v == -1) {
					//System.out.println(emuid+" pin thread got -1");
					overstatus[emuid]=true;
					}
				
				if (SharedMem.termination[tid]==true) {
					allover = true;
					break;
				}
			}
//			if (toExit)
//				break;
			
			// this runnable thread can be stopped in two ways. Either the emulator threads from 
			// which it was supposed to read never started(none of them) so it has to be 
			// signalled by the main thread. When this happens 'allover' becomes 'true' and it
			// breaks out from the loop. The second situation is that all the emulator threads
			// which started have now finished, so probably this thread should now terminate.
			// The second condition handles this situation.
			// NOTE this ugly state management cannot be avoided unless we use
			// some kind of a signalling mechanism between the emulator and simulator(TODO).
			// Although this should handle most of the cases.
			if (allover || (emulatorStarted && emuThreadsFinished())) {
				SharedMem.termination[tid]=true;
				break;
			}
		}
		
		//this instruction is a MARKER that indicates end of the stream - used by the pipeline logic
		inputToPipeline.appendInstruction(new Instruction(OperationType.inValid, null, null, null));
		
		while(eventQ.isEmpty() == false)
		{
			eventQ.processEvents();
			
			GlobalClock.incrementClock();
		}
		
		long dataRead = 0;
		for (int i=0; i<EMUTHREADS; i++) {
			dataRead+=tot_cons[i];
		}		
		long timeTaken = System.currentTimeMillis() - start;
		System.out.println("\nThread"+tid+" Bytes-"+dataRead*20
				+" instructions-"+SharedMem.numInstructions[tid]
				+" microops-"+noOfMicroOps
				+" time-"+timeTaken+" MBPS-"+
				(double)(dataRead*24)/(double)timeTaken/1000.0+" KIPS-"+
				(double)SharedMem.numInstructions[tid]/(double)timeTaken + "\n");
		
		Statistics.setDataRead(dataRead*20, tid);
		Statistics.setNumInstructions(SharedMem.numInstructions[tid], tid);
		Statistics.setNoOfMicroOps(noOfMicroOps, tid);
		
		SharedMem.free.release();
	}

	private DynamicInstruction configurePackets(Vector<Packet> vectorPacket,
			int tid2, int tidEmu) {
		Packet p;
		Vector<Long> memReadAddr = new Vector<Long>();
		Vector<Long> memWriteAddr = new Vector<Long>();
		Vector<Long> srcRegs = new Vector<Long>();
		Vector<Long> dstRegs = new Vector<Long>();

		long ip = vectorPacket.elementAt(0).ip;
		boolean taken = false;
		long branchTargetAddress = 0;
		for (int i = 0; i < vectorPacket.size(); i++) {
			p = vectorPacket.elementAt(i);
			assert (ip == p.ip) : "all instruction pointers not matching";
			switch (p.value) {
			case (-1):
				break;
			case (0):
				assert (false) : "The value is reserved for locks. Most probable cause is a bad read";
				break;
			case (1):
				assert (false) : "The value is reserved for locks";
				break;
			case (2):
				memReadAddr.add(p.tgt);
				break;
			case (3):
				memWriteAddr.add(p.tgt);
				break;
			case (4):
				taken = true;
				branchTargetAddress = p.tgt;
				break;
			case (5):
				taken = false;
				branchTargetAddress = p.tgt;
				break;
			case (6):
				srcRegs.add(p.tgt);
				break;
			case (7):
				dstRegs.add(p.tgt);
				break;
			default:
				assert (false) : "error in configuring packets";
			}
		}

		 return new DynamicInstruction(ip, tidEmu, taken,
				branchTargetAddress, memReadAddr, memWriteAddr, srcRegs,
				dstRegs);
	}

	public InstructionList getInputToPipeline() {
		return inputToPipeline;
	}
}