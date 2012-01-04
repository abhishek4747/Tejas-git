package emulatorinterface;

public class SynchType {

	public SynchType(int thread, long time, int encoding) {
		super();
		this.thread = thread;
		this.time = time;
		this.encoding = encoding;
	}

	int thread;
	long time;
	int encoding; // same as in Encoding.java
}
