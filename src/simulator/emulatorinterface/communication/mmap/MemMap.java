package emulatorinterface.communication.mmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import config.SimulationConfig;

import emulatorinterface.communication.*;


/*XXX
 * Caution, this code has not been tested.
 * */

public class MemMap extends IpcBase
{
	// Must ensure that this is same as in mmap.h
	public static final int COUNT = 1000;
	static final String FILEPATH = "pfile";

	File aFile;
	RandomAccessFile ioFile;
	FileChannel ioChannel;

	private IntBuffer ibuf;

	private LongBuffer lockBuf;
	MappedByteBuffer buf;
	MappedByteBuffer lBuf;

	public MemMap(){
		super();
		aFile = new File (FILEPATH);
		try {
			ioFile = new RandomAccessFile (aFile, "rw");
			ioChannel = ioFile.getChannel ();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Process startPIN(String cmd) throws Exception{
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec(cmd);
			StreamGobbler s1 = new StreamGobbler ("stdin", p.getInputStream ());
			StreamGobbler s2 = new StreamGobbler ("stderr", p.getErrorStream ());
			s1.start ();
			s2.start ();
			return p;
		} catch (Exception e) {
			return null;
		}
	}



	public long doExpectedWaitForSelf() throws InterruptedException {
		// this takes care if no thread started yet.
		free.acquire();	

		// if any thread has started and not finished then wait.
		for (int i=0; i<MaxNumJavaThreads; i++) {
			if (started[i] && !termination[i]) {
				free.acquire();
			}
		}

		long totalInstructions = 0;

		//inform threads which have not started about finish
		for (int i=0; i<MaxNumJavaThreads; i++) {
			if (started[i]==false) termination[i]=true;
			//totalInstructions += numInstructions[i];
		}

		//return totalInstructions;
		return 0;
	}

	public void finish(){
		try {
			ioFile.close ();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Packet fetchOnePacket(int tidApp, int index) {
		 //TODO
		//this should return a packet	(ibuf.get( (index) %COUNT ) );
		return null;
	}

	@Override
	public void initIpc() {
		if (SimulationConfig.debugMode) 
			System.out.println("-- Mmap initialising");
		try {
			buf = ioChannel.map (FileChannel.MapMode.READ_WRITE, 0L,
					(long) ((COUNT) * 4)).load ();

			lBuf = ioChannel.map (FileChannel.MapMode.READ_WRITE, (long) ((COUNT) * 4),
					(long) (20) ).load ();
			ioChannel.close ();

			//FIXME TODO
			// these should be as packet buffer not int buffers
			ibuf = buf.asIntBuffer ();
			lockBuf = lBuf.asLongBuffer ();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		String name;
		for (int i=0; i<MaxNumJavaThreads; i++){
			name = "thread"+Integer.toString(i);
			termination[i]=false;
			started[i]=false;
			//TODO not all cores are assigned to each thread
			//when the mechanism to tie threads to cores is in place
			//this has to be changed
		}
	}

	@Override
	public long numPackets(int tidApp) {
		get_lock(lockBuf, 0, lBuf);
		long queue_size = lockBuf.get(0);
		release_lock(lockBuf, 0, lBuf);
		return queue_size;
	}

	private void release_lock(LongBuffer lockBuf2, int i, MappedByteBuffer lBuf2) {
		ibuf.put(COUNT + 2, 0);
		buf.force();

	}

	private void get_lock(LongBuffer lockBuf2, int i, MappedByteBuffer lBuf2) {

		ibuf.put(COUNT + 2, 1); // flag[1] = 1
		buf.force();
		ibuf.put(COUNT + 3, 0); // turn = 0
		buf.force();
		while( (ibuf.get(COUNT+1) == 1) && (ibuf.get(COUNT + 3) == 0 )) {}

	}

	@Override
	public long totalProduced(int tidApp) {
		return lockBuf.get(0 + 4);
	}

	@Override
	public long update(int tidApp, long numReads) {
		long queue_size;
		get_lock(lockBuf, 0, lBuf);
		  queue_size = lockBuf.get(0);
	      queue_size -= numReads;
		  lockBuf.put(0, queue_size);
		  release_lock(lockBuf, 0,lBuf);
		  return queue_size;
	}

	public ArrayList<Packet> fetchManyPackets(int tidApp, int readerLocation,
			int numReads) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long fetchManyPackets(int tidApp, long readerLocation,
			long numReads, ArrayList<Packet> fromPIN) {
		// TODO Auto-generated method stub
		return 0;
	}
}
