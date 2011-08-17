package memorysystem;

import generic.RequestType;
import generic.SimulationElement;

public class CacheOutstandingRequestTableEntry
{	
	RequestType requestType;
	SimulationElement requestingElement;
	long address;
	
	/**
	 * Just stores the LSQ entry index if the ready event is for an LSQ.
	 * Stores the INVALID_INDEX otherwise.
	 */
	//int lsqIndex = LSQ.INVALID_INDEX;
	LSQEntry lsqEntry = null;
	
	public CacheOutstandingRequestTableEntry(RequestType requestType,
			SimulationElement requestingElement, long address, LSQEntry lsqEntry) 
	{
		super();
		this.requestType = requestType;
		this.requestingElement = requestingElement;
		this.address =address;
		this.lsqEntry = lsqEntry;
	}	
}
