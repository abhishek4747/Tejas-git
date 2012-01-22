package memorysystem.directory;

import generic.Event;
import generic.RequestType;

public class CentralizedDirectory {
	static DirectoryEntry[] directory;
	static int numPresenceBits;
	
	public CentralizedDirectory(int noOfCacheLines, int noOfCores){

		directory = new DirectoryEntry[noOfCacheLines];
		numPresenceBits=noOfCores;
		for(int i=0;i<noOfCacheLines;i++)
			directory[i] = new DirectoryEntry(noOfCores);
	}

	public static long updateDirectory(int cacheLine, int requestingCore, RequestType reqType) {
		if(reqType==RequestType.Cache_Read){
			return readMissUpdate(cacheLine, requestingCore);
		}
		else if(reqType==RequestType.Cache_Write){
			return writeMissUpdate(cacheLine, requestingCore);
		}
		else{
			System.out.println("Inside Centralized Directory, Encountered an event which is neither cache read nor cache write");
			return -1;
		}
	}

	private static long writeMissUpdate(int cacheLine, int requestingCore) {
		long directoryAccessDelay=10;
		long memWBDelay=200;
		long invalidationSendDelay=30;
		long invalidationAckCollectDelay=30;
		long ownershipChangeDelay=15;
		DirectoryState state= directory[cacheLine].getState();
		//DirectoryEntry dirEntry= directory[cacheLine];
		if(state==DirectoryState.uncached){
			directory[cacheLine].setPresenceBit(requestingCore, true);			
			directory[cacheLine].setState(DirectoryState.exclusive);
			return directoryAccessDelay;
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
			return directoryAccessDelay+invalidationAckCollectDelay+invalidationSendDelay;
		}
		else if(state==DirectoryState.exclusive){
			//TODO send ownership change message
			int ownerNum = directory[cacheLine].getOwner();
			if(ownerNum==-1)
				System.out.println("Nobody owns this line. Some Error.");
			directory[cacheLine].setPresenceBit(requestingCore, true);			
			directory[cacheLine].setState(DirectoryState.exclusive);
			directory[cacheLine].setPresenceBit(ownerNum,false);			
			return directoryAccessDelay+ownershipChangeDelay;
		}
		else
			return -1;
	}

	private static long readMissUpdate(int cacheLine, int requestingCore) {
		long directoryAccessDelay=10;
		long memWBDelay=200;
		long dataTransferDelay=20;
		DirectoryState state= directory[cacheLine].getState();
		if(state==DirectoryState.readOnly){
			directory[cacheLine].setPresenceBit(requestingCore, true);
			return directoryAccessDelay;
		}
		else if(state==DirectoryState.uncached ){
			directory[cacheLine].setPresenceBit(requestingCore, true);
			directory[cacheLine].setState(DirectoryState.readOnly);
			return directoryAccessDelay+memWBDelay;
			//TODO Writeback to memory
		}
		else if(state==DirectoryState.exclusive){
			directory[cacheLine].setPresenceBit(requestingCore, true);
			directory[cacheLine].setState(DirectoryState.readOnly);
			return directoryAccessDelay+memWBDelay+dataTransferDelay;
			//TODO Writeback to memory
			
		}
		else
			return -1;
	}
}
