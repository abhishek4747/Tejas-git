package memorysystem.directory;

public class DirectoryEntry {
	DirectoryState state;
	boolean[] presenceBits;
	int numPresenceBits;
	public DirectoryEntry(int noOfCores){
		state=DirectoryState.uncached;
		presenceBits=new boolean[noOfCores];
		numPresenceBits=noOfCores;
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

}
