package memorysystem.directory;

import java.util.Vector;

import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.MESI;
import memorysystem.Cache.CacheType;

public class DirectoryEntry extends CacheLine {
	// MESI state;
	//boolean[] presenceBits;
	Vector<Cache> sharers = null;
	
//	private boolean valid;
	private double timestamp;
//	private boolean modified;
	int noOfCores;

	public DirectoryEntry(int noOfCores){
		super(1);
		sharers = new Vector<Cache>(noOfCores);
		this.noOfCores = noOfCores;
		state = MESI.INVALID;
		tag = 0;
	}
	
	public DirectoryEntry copy()
	{
		DirectoryEntry newLine = new DirectoryEntry(noOfCores);
		newLine.setAddress(this.address);
		newLine.setTag(this.getTag());
		newLine.setState(this.getState());
		newLine.setTimestamp(this.getTimestamp());
		newLine.sharers.addAll(this.sharers);
		return newLine;
	}
	
	public DirectoryEntry clone() 
	{
		return copy();
	}
	
	public Cache getOwner(){
						
		if(sharers.size()==0) {
			return null;
		} else if (sharers.size()==1) {
			return sharers.elementAt(0); 
		} else {
			misc.Error.showErrorAndExit("This directory entry has multiple owners : " + this);
			return null;
		}
	}
	
	public boolean isSharer(Cache c) {
		return (this.sharers.indexOf(c)!=-1);
	}
	
	public MESI getState(){
		return this.state;
	}
	
	public void setState(MESI state){
		this.state=state;
	}
	
	public int getNoOfSharers() {
		return this.sharers.size();
	}
	
	public Cache getSharerAtIndex(int i){
		return this.sharers.elementAt(i);
	}
	
	public void addSharer(Cache c) {
		
		if(this.state==MESI.INVALID) {
			misc.Error.showErrorAndExit("Unholy mess !!");
		}
		
		// You cannot add a new sharer for a modified entry.
		// For same entry, if you try to add an event, it was because the cache sent multiple requests for 
		// the same cache line which triggered the memResponse multiple times. For the time being, just ignore this hack.
		if(this.state==MESI.MODIFIED && this.sharers.size()>0 && this.sharers.elementAt(0)!=c) {
			misc.Error.showErrorAndExit("You cannot have multiple owners for a modified state !!\n" +
					"currentOwner : " + getOwner().containingMemSys.getCore().getCore_number() + 
					" newOwner : " + c.containingMemSys.getCore().getCore_number() + 
					" addr : " + this.getAddress());
		}
		
		if(this.isSharer(c)==true) {
			return;
		}
		
		this.sharers.add(c);
	}
	
	public void removeSharer(Cache c) {
		
		if(this.isSharer(c)==false) {
			misc.Error.showErrorAndExit("Trying to remove a sharer which is not a sharer !!");
		}
		
		this.sharers.remove(c);
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
	
	public void clearAllSharers() {
		this.sharers.clear();
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("line number = " + this.getTag() + " : "  + "state = " + this.getState() + " cores : " );
		for(int i=0; i<this.sharers.size(); i++) {
			s.append(this.sharers.elementAt(i).containingMemSys.getCore().getCore_number() + " , ");
		}
		return s.toString();
	}
}
