package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class CBDNuca extends DNuca {
	public CBDNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) 
	{
		super(cacheParameters,containingMemSys);
		System.out.println("CBDnuca ");
	}
	
	public Vector<Integer> getNearestBankId(long address,int coreId)
	{
		int setIndex = getSetIndex(address);
		//System.out.println("set index" + setIndex + "coreid "+ coreId + "coreMapping size "+cacheMapping.size());
		return integerToBankId(cacheMapping.get(coreId).get(setIndex).get(0));
	}
	
	public void handleEvent(EventQueue eventQ,Event event)
	{
		SimulationElement requestingElement = event.getRequestingElement();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		Vector<Integer> sourceBankId = getNearestBankId(address, ((AddressCarryingEvent)(event)).coreId);
		Vector<Integer> destinationBankId = getNearestBankId(address, ((AddressCarryingEvent)(event)).coreId);
		//System.out.println(sourceBankId + " " + destinationBankId + " " + getBankNumber(address));
		//System.exit(0);
		((AddressCarryingEvent)event).oldRequestingElement = (SimulationElement) event.getRequestingElement().clone();
		cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
								getPort().put(((AddressCarryingEvent)event).
														updateEvent(eventQ, 
																	0,//to be  changed to some constant(wire delay) 
																	requestingElement, 
																	cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
																	event.getRequestType(), 
																	sourceBankId, 
																	destinationBankId));	
	}
}
