package memorysystem.snoopyCoherence;

import java.util.ArrayList;
import java.util.Stack;
import memorysystem.*;

public class Bus 
{
	protected BusController busController;
	protected Cache requestingCache;
	protected BusReqType requestType;
	protected long address;
	protected CacheLine sourceLine;
	protected LSQEntry lsqEntry;
	
	protected int snoopingCoresProcessed = 0;
	protected int copiesFound = 0;

	protected CacheLine singleFoundCopy = null;
	protected Cache cacheContainingTheCopy = null;
	
	protected boolean blockRWITM = false;
	
	protected ArrayList<BusRequestQElmnt> reqQueue = 
		new ArrayList<BusRequestQElmnt>();
	
	protected boolean isLocked = false;
	
	public enum BusReqType{
		INVALIDATE,	//Broadcast INVALID (Happens on a write hit if state is SHARED)
		RWITM,		//Broadcast "Read With Intent To Modify" (Happens on a write miss)
		MEM_ACCESS	//Memory (or lower level cache) access request (Happens on a read miss)
	}
	
	private Cache getBank(long address)
	{
		return this.busController.lowerCache;
	}
}
