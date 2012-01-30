/*
 * This represents a reader thread in the simulator which keeps on reading from EMUTHREADS.
 */

package emulatorinterface;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
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
public class RunnableFromFile implements Runnable, Encoding {

	int tid;
	long sum = 0; // checksum
	int EMUTHREADS = IpcBase.EmuThreadsPerJavaThread;

	ThreadParams[] threadParams = new ThreadParams[EMUTHREADS];

	InstructionLinkedList[] inputToPipeline;
	IpcBase ipcType;


	int[] decodeWidth;
	int[] stepSize;
	long[] noOfMicroOps;
	long[] numInstructions;
	//FIXME PipelineInterface should be in IpcBase and not here as pipelines from other RunnableThreads
	// will need to interact.
	PipelineInterface[] pipelineInterfaces;

	//	MessageDigest md5;


	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	public RunnableFromFile(String threadName, int tid1, IpcBase ipcType, Core[] cores) {

		for(int i = 0; i < EMUTHREADS; i++)
		{ threadParams[i] = new ThreadParams();
		}

		inputToPipeline = new InstructionLinkedList[EMUTHREADS];
		decodeWidth = new int[EMUTHREADS];
		stepSize = new int[EMUTHREADS];
		noOfMicroOps = new long[EMUTHREADS];
		numInstructions = new long[EMUTHREADS];
		pipelineInterfaces = new PipelineInterface[EMUTHREADS];
		for(int i = 0; i < EMUTHREADS; i++)
		{
			int id = tid1*IpcBase.EmuThreadsPerJavaThread+i;
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
		System.out.println("--  starting java thread"+this.tid);
	}


	public void run() {

		//		System.out.println("-- in runnable thread run "+this.tid);
		ThreadParams thread;

		threadParams[0].started=true;

		long totMicroOps = readFile(SimulationConfig.InstructionsFilename);



		for (int i=0; i<EMUTHREADS; i++)
			this.inputToPipeline[i].appendInstruction(new Instruction(OperationType.inValid,null, null, null));

		System.out.println("Read the instructions. starting pipeline");
		Newmain.start = System.currentTimeMillis();

		while(true)
		{
			//System.out.println("Pin completed ");

			
			//System.out.println(pipelineInterfaces[0].isExecutionComplete()+"  "+pipelineInterfaces[1].isExecutionComplete());

			int maxN = inputToPipeline[0].getListSize()/decodeWidth[0] * pipelineInterfaces[0].getCoreStepSize();
	//FIXME Ask Kapil to fix the bug
	// 			if (pipelineInterfaces[0].isExecutionComplete()) break;
			if (maxN==0){ 
				pipelineInterfaces[0].getCore().getExecutionEngineIn().setExecutionComplete(true);
				break;
			}
			System.out.println("maxN is "+maxN);
			for (int i1=0; i1< maxN; i1++)	{

				pipelineInterfaces[0].oneCycleOperation();
				GlobalClock.incrementClock();
			}

		}
		Core core;
		for (int tidEmu = 0; tidEmu < EMUTHREADS; tidEmu++) {
			core = pipelineInterfaces[tidEmu].getCore();
			if(core.getExecutionEngineIn().getExecutionComplete()){
System.out.println("Setting statistics for core number = "+core.getCore_number()+"with step size= "+core.getStepSize());
				System.out.println("number of instructions executed = "+core.getNoOfInstructionsExecuted());
				pipelineInterfaces[tidEmu].setTimingStatistics();			
				pipelineInterfaces[tidEmu].setPerCoreMemorySystemStatistics();
			}
		}
		
		long timeTaken = System.currentTimeMillis() - Newmain.start;
		System.out.println("\nThread" + tid
                           +" microOps  "+totMicroOps
                           +" time "+ timeTaken 
                           +"\n microOp KIPS- "+ (double) totMicroOps / (double)timeTaken
				           + "\n");

		//		System.out.println("number of micro-ops = " + noOfMicroOps + "\t\t;\thash = " + makeDigest());

//System.out.println("Tid = "+tid+"microops= "+noOfMicroOps[0]);
		noOfMicroOps[tid]=totMicroOps;
		
		Statistics.setNumInstructions(numInstructions, tid);
		Statistics.setNoOfMicroOps(noOfMicroOps, tid);

		IpcBase.free.release();
	}

	private long readFile(String filename) {
		ObjectInputStream inputStream = null;
		long cnt=0;
		try {

			//Construct the ObjectInputStream object
			inputStream = new ObjectInputStream(new FileInputStream(filename));

			Object obj = null;

			while ((obj = inputStream.readObject()) != null) {

				if (obj instanceof Instruction) {
					cnt++;
					this.inputToPipeline[0].appendInstruction((Instruction)obj);
					//System.out.println(((Instruction)obj).toString());
				}

			}


		} catch (EOFException ex) { //This exception will be caught when EOF is reached
			System.out.println("End of file reached.");
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			//Close the ObjectInputStream
			try {
				if (inputStream != null) {
					System.out.println("Closing input stream");
					inputStream.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("done from function");
		return cnt;

		/*			// Writing
			if (noOfMicroOps[0]>10000000) doNotProcess=true;
  			for (Instruction ins : tempList.instructionLinkedList) {
				try {
					this.output.writeObject(ins);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		 */
	}

}