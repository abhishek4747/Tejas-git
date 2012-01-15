package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import memorysystem.*;

import java.util.Vector;

import java.util.*;

import config.CacheConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import misc.Util;
public class NucaCacheBank extends Cache
{
    private double timestamp;//used when LRU replacement policy is used for LLC
	Router router;
	CacheConfig cacheParameters;

    NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	int bankSize = cacheParameters.getBankSize();
        this.cacheParameters = cacheParameters;
        this.router = new Router(bankId,cacheParameters.numberOfBuffers);
    }
    
    public boolean lookup(long tag)//looks for tag in cache lines present in the bank sequentially
    {
        for(int i=0;i<lines.length;i++)
        {
            if(tag ==lines[i].getTag())
                return true;
        }
        return false;
    }
    
	


    public Object clone()
    {
        try
        {
            // call clone in Object.
            return super.clone();
        } catch(CloneNotSupportedException e)
        {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }
	
    public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}
	
    public double getTimestamp() {
		return timestamp;
	}
    
	public Router getRouter()
	{
		return this.router;
	}
	
	@Override
	
	public void handleEvent(EventQueue eventQ, Event event){
		// TODO Auto-generated method stub
		
		SimulationElement requestingElement = event.getRequestingElement();
		RequestType requestType = event.getRequestType();
		
		RoutingAlgo.DIRECTION nextID;
		Vector<Integer> destinationId;
		Vector<Integer> currentId = ((NucaCacheBank) event.getRequestingElement()).router.getBankId();

	   //Destination is stored inside event
		destinationId = ((DestinationBankEvent)(event)).getDestination();
			
		if(currentId.equals(destinationId))
		{
			//TODO
			//process the bank search
			//if request is for read ,then return data
		}
		
		nextID = router.RouteComputation(currentId, destinationId, RoutingAlgo.ALGO.SIMPLE);
		
		if(router.CheckNeighbourBuffer(nextID)) 
		{
			//post event to nextID
			requestingElement.getPort().put(
					event.update(
							eventQ,
							1,
							this, 
							this.router.GetNeighbours().elementAt(nextID.ordinal()),
							requestType));
			this.router.FreeBuffer();
		}
		else
		{
			//post event to this ID
			requestingElement.getPort().put(
					event.update(
							eventQ,
							1,
							this, 
							this,
							requestType));
		}
	}

}