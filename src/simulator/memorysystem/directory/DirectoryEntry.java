package memorysystem.directory;

import memorysystem.CacheLine;
import memorysystem.MESI;
import memorysystem.Cache.CacheType;

public class DirectoryEntry extends CacheLine {
	MESI state;
	boolean[] presenceBits;
	
//	private boolean valid;
	private double timestamp;
//	private boolean modified;

	public DirectoryEntry(int noOfCores, long lineNum){
		super(1);
		presenceBits=new boolean[noOfCores];
		
		state = MESI.INVALID;
		tag = 0;
		for(int i=0;i<noOfCores;i++)
			presenceBits[i]=false;
	}
	
	public DirectoryEntry copy()
	{
		DirectoryEntry newLine = new DirectoryEntry(this.presenceBits.length,0);
		newLine.setTag(this.getTag());
		newLine.setState(this.getState());
		newLine.setTimestamp(this.getTimestamp());
		return newLine;
	}
	
	public int getOwner(){
		//This should be called only when the state of the directory entry is "modified"
		for(int i=0;i<this.presenceBits.length;i++){
			if(presenceBits[i])
				return i;
		}
		return -1;
	}
	
	public MESI getState(){
		return this.state;
	}
	
	public void setState(MESI state){
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

	
	public double getTimestamp() {
		return timestamp;
	}

	protected void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}
	
	public void resetAllPresentBits()
	{
		for(int i=0;i< this.presenceBits.length ; i++)
		{
			presenceBits[i] = false;
		}
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("line number = " + this.getTag() + " : "  + "state = " + this.getState() + " : " );
		for(int i = 0; i< presenceBits.length;i++)
		{
			if(presenceBits[i])
				s.append(1 + " ");
			else 
				s.append(0 + " ");
		}
		return s.toString();
	}
}
