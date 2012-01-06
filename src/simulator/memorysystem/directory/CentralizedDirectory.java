package memorysystem.directory;

import generic.Event;
import generic.RequestType;

public class CentralizedDirectory {
	static DirectoryEntry[] directory;
	static int numPresenceBits;
	
	public CentralizedDirectory(int noOfCacheLines, int noOfCores){
		/*Constructor called in Cache constructor */

		directory = new DirectoryEntry[noOfCacheLines];
		numPresenceBits=noOfCores;
		for(int i=0;i<noOfCacheLines;i++)
			directory[i] = new DirectoryEntry(noOfCores);
	}

	public static void updateDirectory(int cacheLine, int requestingCore, RequestType reqType) {
		if(reqType==RequestType.Cache_Read){
			readMissUpdate(cacheLine, requestingCore);
		}
		else if(reqType==RequestType.Cache_Write){
			writeMissUpdate(cacheLine, requestingCore);
		}
		else{
			System.out.println("Inside Centralized Directory, Encountered an event which is neither cache read nor cache write");
		}
	}

	private static void writeMissUpdate(int cacheLine, int requestingCore) {
		DirectoryState state= directory[cacheLine].getState();
		//DirectoryEntry dirEntry= directory[cacheLine];
		if(state==DirectoryState.uncached){
			directory[cacheLine].setPresenceBit(requestingCore, true);			
			directory[cacheLine].setState(DirectoryState.exclusive);
		}
		else if(state==DirectoryState.readOnly){
			for(int i=0;i<numPresenceBits;i++){
				if(directory[cacheLine].getPresenceBit(i)){
					//TODO send invalidation messages
					directory[cacheLine].setPresenceBit(i,false);
				}
				directory[cacheLine].setPresenceBit(requestingCore, true);			
				directory[cacheLine].setState(DirectoryState.exclusive);
				//TODO collect invalidation acks
			}
		
		}
		else if(state==DirectoryState.exclusive){
			//TODO send ownership change message
			int ownerNum = directory[cacheLine].getOwner();
			if(ownerNum==-1)
				System.out.println("Nobody owns this line. Some Error.");
			directory[cacheLine].setPresenceBit(requestingCore, true);			
			directory[cacheLine].setState(DirectoryState.exclusive);
			directory[cacheLine].setPresenceBit(ownerNum,false);			
			
		}
	}

	private static void readMissUpdate(int cacheLine, int requestingCore) {
		DirectoryState state= directory[cacheLine].getState();
		if(state==DirectoryState.uncached || state==DirectoryState.readOnly){
			directory[cacheLine].setPresenceBit(requestingCore, true);
		}
		else if(state==DirectoryState.exclusive){
			directory[cacheLine].setPresenceBit(requestingCore, true);
			directory[cacheLine].setState(DirectoryState.readOnly);
			//TODO Writeback to memory
		}
	}
}
