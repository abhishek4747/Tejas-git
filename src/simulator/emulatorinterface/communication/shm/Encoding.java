package emulatorinterface.communication.shm;



// change encoding.h if any change here.
public interface Encoding {
	static final int MEMREAD  = 2;
	static final int MEMWRITE = 3;
	static final int TAKEN = 4;
	static final int NOTTAKEN = 5;
	static final int REGREAD = 6;
	static final int REGWRITE = 7;

	// synchronization values should be between SYNCHSTART AND SYNCHEND
	static final int SYNCHSTART = 9;
	static final int BCAST = 10;
	static final int SIGNAL = 12;
	static final int LOCK = 14;
	static final int UNLOCK = 16;
	static final int JOIN = 18;
	static final int CONDWAIT = 20;
	static final int BARRIERWAIT = 22;
	static final int SYNCHEND = 24;
}
