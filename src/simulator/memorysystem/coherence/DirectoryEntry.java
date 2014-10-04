package memorysystem.coherence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.MESI;

public class DirectoryEntry extends CacheLine {

	private LinkedList<Cache> sharers = null;
	private AddressCarryingEvent currentEvent = null;
	private LinkedList<Cache> listOfAwaitedCacheResponses = new LinkedList<Cache>();
	
	public void addCacheToAwaitedCacheList(Cache c) {
		if(listOfAwaitedCacheResponses.contains(c)==false) {
			listOfAwaitedCacheResponses.add(c);
		} else {
			misc.Error.showErrorAndExit("Cannot add cache to the awaited cache list");
		}		
	}
	
	public void removeCacheFromAwaitedCacheList(Cache c) {
		if(listOfAwaitedCacheResponses.contains(c)==false) {
			misc.Error.showErrorAndExit("Cache to be removed not found. Cache : " + c);
		}
		
		listOfAwaitedCacheResponses.remove(c);
	}
	
	public boolean isLocked() {
		return currentEvent!=null;
	}
	
	public void unlock() {
		if(currentEvent==null) {
			misc.Error.showErrorAndExit("Trying to unlock an unlocked event !!");
		}
		
		currentEvent = null;
	}
	
	public void setCurrentEvent(AddressCarryingEvent event) {
		if(isLocked() && event!=null) {
			misc.Error.showErrorAndExit("Trying to lock an already locked event !!");
		}
		
		currentEvent = event;
	}
	
	public AddressCarryingEvent getCurrentEvent() {
		if(isLocked()==false) {
			misc.Error.showErrorAndExit("The directory entry is in unlocked state !!");
		}
		
		return currentEvent;
	}
	
	public DirectoryEntry(){
		super(1);
		sharers = new LinkedList<Cache>();
		state = MESI.INVALID;
		tag = 0;
	}
	
	public DirectoryEntry copy()
	{
		DirectoryEntry newLine = new DirectoryEntry();
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
			return sharers.get(0); 
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
	
	public void addSharer(Cache c) {
		
		if(this.state==MESI.INVALID) {
			misc.Error.showErrorAndExit("Unholy mess !!");
		}
		
		// You cannot add a new sharer for a modified entry.
		// For same entry, if you try to add an event, it was because the cache sent multiple requests for 
		// the same cache line which triggered the memResponse multiple times. For the time being, just ignore this hack.
		if(this.state==MESI.MODIFIED && this.sharers.size()>0 && this.sharers.get(0)!=c) {
			misc.Error.showErrorAndExit("You cannot have multiple owners for a modified state !!\n" +
					"currentOwner : " + getOwner().containingMemSys.getCore().getCore_number() + 
					"\nnewOwner : " + c.containingMemSys.getCore().getCore_number() + 
					"\naddr : " + this.getAddress());
		}
		
		// You cannot add a new sharer for exclusive entry.
		// For same entry, if you try to add an event, it was because the cache sent multiple requests for 
		// the same cache line which triggered the memResponse multiple times. For the time being, just ignore this hack.
		if(this.state==MESI.EXCLUSIVE && this.sharers.size()>0 && this.sharers.get(0)!=c) {
			misc.Error.showErrorAndExit("You cannot have multiple owners for exclusive state !!\n" +
					"currentOwner : " + getOwner().containingMemSys.getCore().getCore_number() + 
					"\nnewOwner : " + c.containingMemSys.getCore().getCore_number() + 
					"\naddr : " + this.getAddress());
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

	public boolean hasTagMatch(long tag)
	{
		if (tag == this.getTag())
			return true;
		else
			return false;
	}
	
	public long getTag() {
		return tag;
	}

	public void setTag(long tag) {
		this.tag = tag;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}
	
	public void clearAllSharers() {
		this.sharers.clear();
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("addr = " + this.getAddress() + " : "  + "state = " + this.getState() + " cores : " );
		for(Cache c : sharers) {
			s.append(c.containingMemSys.getCore().getCore_number() + " , ");
		}
		return s.toString();
	}

	public LinkedList<Cache> getSharers() {
		return sharers;
	}
	
	public LinkedList<Cache> getListOfAwaitedCacheResponses() {
		return listOfAwaitedCacheResponses;
	}

	public Cache getFirstSharer() {
		return sharers.get(0);
	}
}
