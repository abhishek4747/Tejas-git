package memorysystem.nuca;
import java.util.ArrayList;
import java.util.Vector;

import net.RoutingAlgo;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.ExecCompleteEvent;
import generic.RequestType;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.DestinationBankEvent;
import memorysystem.LSQEntryContainingEvent;
import memorysystem.MemorySystem;
import memorysystem.Cache.CacheType;
import misc.Util;
import config.CacheConfig;
import config.SystemConfig;

public class SNuca extends NucaCache
{
	public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys,SystemConfig sysConfig) {
        super(cacheParameters,containingMemSys,sysConfig);
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
