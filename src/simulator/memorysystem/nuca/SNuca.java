package memorysystem.nuca;
import java.util.Vector;

import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import misc.Util;
import config.CacheConfig;
import config.SystemConfig;

public class SNuca extends NucaCache
{
	public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
        super(cacheParameters,containingMemSys);
    }
		
	public long getTag(long addr) {
		// TODO Auto-generated method stub
		long tag = (addr >>> (blockSizeBits +Util.logbase2(getNumOfBanks())));
		return tag;
	}
	public int getBankNumber(long addr)
	{
		return (int)(addr>>>blockSizeBits)%getNumOfBanks();
	}
	
	public Vector<Integer> getDestinationBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(bankNumber/cacheColumns);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}
	
	public Vector<Integer> getSourceBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(0);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}

	

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		SimulationElement requestingElement = event.getRequestingElement();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address);
		Vector<Integer> destinationBankId = getDestinationBankId(address);
		boolean alreadypresent= this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].addOutstandingRequest(event, address);
		if(!alreadypresent)
			this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].
									getPort().put(((AddressCarryingEvent)event).
															updateEvent(eventQ, 
																		0,//to be  changed to some constant(wire delay) 
																		requestingElement, 
																		this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)], 
																		event.getRequestType(), 
																		sourceBankId, 
																		destinationBankId));

	}
}