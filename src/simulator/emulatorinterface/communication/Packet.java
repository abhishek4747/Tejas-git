package emulatorinterface.communication;

// This is very hardbound to the jni C file. in the shmread function.Update JNIShm.c if any changes
// are made here
public class Packet 
{
	// If info packet then ip represents instruction pointer and tgt represents the target addr/mem 
	// address. Else if synchronization packet then ip represents time and tgt represents lock 
	// address. Else if timer packet then ip represents time and tgt represents nothing.
	public long ip;
	public long value;
	public long tgt;
	public String asm;
	
	public Packet () 
	{
		ip = -1;
	}

	public Packet(long ip, long value, long tgt, String asm) 
	{
		this.ip = ip;
		this.value = value;
		this.tgt = tgt;
		this.asm = asm;
	}

	@Override
	public String toString() {
		if(asm==null) {
			return "Packet [ip=" + ip + ", tgt=" + tgt + ", value=" + value + "]";
		} else {
			return "Packet [ip=" + ip + ", asm=" + asm + ", value=" + value + "]";
		}
	}

	public void set(long ip, long value, long tgt, String asm) {
		this.ip = ip;
		this.value = value;
		this.tgt = tgt;
		this.asm = asm;
	}
	/*
	public Long getTgt()
	{
		return tgt;
	}
	public Long getIp()
	{
		return ip;
	}*/
	
}