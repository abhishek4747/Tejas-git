package emulatorinterface;

import java.io.*;
import java.security.MessageDigest;
import java.util.Hashtable;
import java.util.Iterator;

import main.Main;
import net.optical.TopLevelTokenBus;
import pipeline.PipelineInterface;
import config.SimulationConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.*;

public class RunnableFromFile extends RunnableThread implements Runnable {

	IpcBase ipcType;

	public RunnableFromFile(String threadName, int tid1, IpcBase ipcType,
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

		threadParams[0].started=true;

		long totMicroOps = readFile(SimulationConfig.InstructionsFilename);

		System.out.println("Read "+totMicroOps+" Micro-instructions. Starting pipeline");
		
		//TODO currently reading from file is supported for only 1 thread
		noOfMicroOps[0] = totMicroOps;
		currentEMUTHREADS = 1;
		
		Main.setStartTime(System.currentTimeMillis());
		
		super.finishAllPipelines();
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
					this.inputToPipeline[0].enqueue((Instruction)obj);
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
