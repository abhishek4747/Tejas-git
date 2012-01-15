package memorysystem.nuca;
import java.util.Vector;

import net.RoutingAlgo;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.DestinationBankEvent;
import misc.Util;
import config.CacheConfig;
import config.SystemConfig;

public class SNuca extends NucaCache
{
    public boolean lookup(long addr)
    {
        long tag = getTag(addr);//get the tag from the address
        Vector<Integer> bankId = getBankId(addr);
        return cacheBank[bankId.get(0)][bankId.get(1)].lookup(tag);
    }
    public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys,SystemConfig sysConfig) {
        super(cacheParameters,containingMemSys,sysConfig);
    }

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
		SimulationElement requestingElement = event.getRequestingElement();
	//	SimulationElement processingElement = event.getProcessingElement();
		RequestType requestType = event.getRequestType();
		
		RoutingAlgo.DIRECTION nextID;
		Vector<Integer> destinationId = new Vector<Integer>(2);
		
		Vector<Integer> currentId = ((NucaCacheBank) event.getRequestingElement()).router.getBankId();
		long address = ((AddressCarryingEvent)(event)).getAddress();  //destination address
		
		long tag = getTag(address);
		int bankNumber = getBankNumber(address);
		/*if(requestType == RequestType.Cache_Read ||requestType == RequestType.Cache_Write)
		{
			int numOfBanks = cacheColumns*cacheRows ;
			int numOfBanksBits = Util.logbase2(numOfBanks);
			int cacheSizeBits = Util.logbase2(cacheSize);		
			long tag = address >>> (cacheSizeBits - numOfBanksBits);
			destinationId = integerToBankId(tag);
			//if(requestType == RequestType.Cache_Read)
		//		requestType = RequestType.CacheBank_Read;
		//	else
		//		requestType = RequestType.CacheBank_Write;
		}
		else           //Destination is stored inside event
			destinationId = ((DestinationBankEvent)(event)).getDestination();
			
		if(currentId.equals(destinationId))
		{
			//process the bank search
		}
		
		nextID = router.RouteComputation(currentId, destinationId, RoutingAlgo.ALGO.SIMPLE);
		
		if(router.CheckNeighbourBuffer(nextID)) 
		{
			//post event to nextID
			requestingElement.getPort().put(
					new AddressCarryingEvent(
							eventQ,
							1,
							this, 
							this.router.GetNeighbours().elementAt(nextID.ordinal()),
							requestType, 
							address));
			this.router.FreeBuffer();
		}
		else
		{
			//post event to this ID
			requestingElement.getPort().put(
					new AddressCarryingEvent(
							eventQ,
							1,
							this, 
							this,
							requestType, 
							address));
		}*/
	}
	@Override
	public long getTag(long addr) {
		// TODO Auto-generated method stub
		long tag = (addr >>> (blockSizeBits +Util.logbase2(getNumOfBanks())));
		return tag;
	}
	public int getBankNumber(long addr)
	{
		return (int)(addr>>>blockSizeBits)%getNumOfBanks();
	}
	
	public Vector<Integer> getBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(bankNumber/cacheColumns);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}
}
