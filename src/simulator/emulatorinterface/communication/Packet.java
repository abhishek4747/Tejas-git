package emulatorinterface.communication;

// This is very hardbound to the jni C file. in the shmread function.Update JNIShm.c if any changes
// are made here
public class Packet 
{
	// If info packet then ip represents instruction pointer and tgt represents the target addr/mem 
	// address. Else if synchronization packet then ip represents time and tgt represents lock 
	// address. Else if timer packet then ip represents time and tgt represents nothing.
	public long ip;
	public int value;
	public long tgt;
	
	public Packet () 
	{
		ip = -1;
	}

	public Packet(long ip, int value, long tgt) 
	{
		this.ip = ip;
		this.value = value;
		this.tgt = tgt;
	}
}