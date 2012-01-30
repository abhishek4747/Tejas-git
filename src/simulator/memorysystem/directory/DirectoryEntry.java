package memorysystem.directory;

import memorysystem.CacheLine;
import memorysystem.MESI;

public class DirectoryEntry {
	DirectoryState state;
	boolean[] presenceBits;
	int numPresenceBits;
	private long tag;
	private int line_num;
//	private boolean valid;
	private double timestamp;
//	private boolean modified;
	private int pid;
	public long address;
	
	public DirectoryEntry(int noOfCores, int lineNum){
		state=DirectoryState.uncached;
		presenceBits=new boolean[noOfCores];
		numPresenceBits=noOfCores;
		line_num=lineNum;
	}
	
	protected DirectoryEntry copy()
	{
		DirectoryEntry newLine = new DirectoryEntry(this.numPresenceBits,0);
		newLine.setLine_num(this.getLine_num());
		newLine.setTag(this.getTag());
		newLine.setState(this.getState());
		newLine.setTimestamp(this.getTimestamp());
		return newLine;
	}
	
	public int getOwner(){
		for(int i=0;i<numPresenceBits;i++){
			if(presenceBits[i])
				return i;
		}
		return -1;
	}
	public DirectoryState getState(){
		return this.state;
	}
	public void setState(DirectoryState state){
		this.state=state;
	}
	public boolean getPresenceBit(int i){
		return this.presenceBits[i];
	}
	public void setPresenceBit(int i,boolean presenceBit){
		this.presenceBits[i]=presenceBit;
	}

	protected boolean hasTagMatch(long tag)
	{
		if (tag == this.getTag())
			return true;
		else
			return false;
	}
	public long getTag() {
		return tag;
	}

	protected void setTag(long tag) {
		this.tag = tag;
	}

	protected int getLine_num() {
		return line_num;
	}

	protected void setLine_num(int lineNum) {
		line_num = lineNum;
	}
	protected double getTimestamp() {
		return timestamp;
	}

	protected void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isValid() {
		if(this.getState()!=DirectoryState.uncached)
			return true;
		else
			return false;
	}

}
