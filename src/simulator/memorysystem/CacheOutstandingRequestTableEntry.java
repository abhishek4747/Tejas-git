package memorysystem;

import generic.RequestType;
import generic.SimulationElement;

public class CacheOutstandingRequestTableEntry
{	
	RequestType requestType;
	SimulationElement requestingElement;
	
	/**
	 * Just stores the LSQ entry index if the ready event is for an LSQ.
	 * Stores the INVALID_INDEX otherwise.
	 */
	int lsqIndex = LSQ.INVALID_INDEX;
	
	public CacheOutstandingRequestTableEntry(RequestType requestType,
			SimulationElement requestingElement, int index) 
	{
		super();
		this.requestType = requestType;
		this.requestingElement = requestingElement;
		this.lsqIndex = index;
	}	
}
