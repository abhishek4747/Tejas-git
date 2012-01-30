package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Hashtable;
import config.CacheConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;

public class NucaCacheBank extends Cache
{
	Router router;
	CacheConfig cacheParameters;
	protected Hashtable<Long, ArrayList<SimulationElement>> forwardedRequests;

    NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
        this.router = new Router(bankId,cacheParameters.numberOfBuffers);
        forwardedRequests =new Hashtable<Long, ArrayList<SimulationElement>>();
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
	
	public Router getRouter()
	{
		return this.router;
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event){
		
		RequestType requestType = event.getRequestType();
		RoutingAlgo.DIRECTION nextID;
		Vector<Integer> destinationId;
		Vector<Integer> currentId = ((NucaCacheBank) event.getProcessingElement()).router.getBankId();
	   //Destination is stored inside event
		destinationId = ((AddressCarryingEvent)(event)).getDestinationBankId();
		if(currentId.equals(destinationId))
		{
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write)
				this.handleAccess(eventQ, event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.Main_Mem_Read ||  
					event.getRequestType() == RequestType.Main_Mem_Write)
				this.handleMemoryReadWrite(eventQ,event);
			//else if (event.getRequestType() == RequestType.Main_Mem_Response)
				//this.handleMainMemoryResponse(eventQ,event);
		}
		else
		{
			nextID = router.RouteComputation(currentId, destinationId, RoutingAlgo.ALGO.SIMPLE);
			if(router.CheckNeighbourBuffer(nextID))
			{
				//post event to nextID
				this.router.GetNeighbours().elementAt(nextID.ordinal()).getPort().put(
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
				this.getPort().put(
						event.update(
								eventQ,
								1,
								this, 
								this,
								requestType));
			}
		}
	}
	
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) {//changes are required....
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		SimulationElement processingElement = event.getProcessingElement();
		CacheLine evictedLine = this.fill(addr);
		//if condition to be  changed
		if (evictedLine != null)
		{
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																		 0,
																		 this, 
																		 this, 
																		 RequestType.Main_Mem_Write, 
																		 evictedLine.getTag() << this.blockSizeBits);
			addressEvent.setSourceBankId(((AddressCarryingEvent)event).getDestinationBankId());
			addressEvent.setDestinationBankId(((AddressCarryingEvent)event).getDestinationBankId());
			processingElement.getPort().put(addressEvent);
		}
		long blockAddr = addr >>> this.blockSizeBits;
			if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element from line 128");
				System.exit(1);
			}
			
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
			
			while (!/*NOT*/outstandingRequestList.isEmpty())
			{				
				if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Write)
				{
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																					 0,
																					 this, 
																					 this, 
																					 RequestType.Main_Mem_Write, 
																					 evictedLine.getTag() << this.blockSizeBits);
						addressEvent.setSourceBankId(((AddressCarryingEvent)event).getDestinationBankId());
						addressEvent.setDestinationBankId(((AddressCarryingEvent)event).getDestinationBankId());
						processingElement.getPort().put(addressEvent);
					}				
				}
				else if(outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read)
				{
					outstandingRequestList.get(0).getRequestingElement().getPort().put(
							((AddressCarryingEvent)(outstandingRequestList.get(0))).updateEvent(
									eventQ,
									0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
									this,
									outstandingRequestList.get(0).getRequestingElement(),
									RequestType.Mem_Response,									
									((AddressCarryingEvent)(outstandingRequestList.get(0))).getDestinationBankId(),
									((AddressCarryingEvent)(outstandingRequestList.get(0))).getSourceBankId()));
				}
				else
				{
					System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write from line 164");
					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
				outstandingRequestList.remove(0);
			}
	}

	private void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		RequestType requestType = event.getRequestType();
		((AddressCarryingEvent)event).setSourceBankId(((AddressCarryingEvent)event).getDestinationBankId());
		((AddressCarryingEvent)event).setDestinationBankId(((AddressCarryingEvent)event).getSourceBankId());
		MemorySystem.mainMemory.getPort().put(event.update(eventQ, 
															MemorySystem.mainMemory.getLatencyDelay(), 
															event.getProcessingElement(), 
															MemorySystem.mainMemory, 
															requestType));		
	}

	private void handleAccess(EventQueue eventQ, Event event)
	{
		SimulationElement requestingElement = event.getRequestingElement();
		SimulationElement processingElement = event.getProcessingElement();
		RequestType requestType = event.getRequestType();
		long address = ((AddressCarryingEvent)(event)).getAddress();
		
		//Process the access
		CacheLine cl = this.processRequest(requestType, address);

		//IF HIT
		if (cl != null)
		{
			//Schedule the requesting element to receive the block TODO (for LSQ)
			/* check for requesting element and processing element*/
			if (requestType == RequestType.Cache_Read)
				//Just return the read block
				requestingElement.getPort().put(
						((AddressCarryingEvent)event).updateEvent(
								eventQ,
								processingElement.getLatencyDelay(),
								this,
								this,
								RequestType.Mem_Response,
								((AddressCarryingEvent)event).getDestinationBankId(),
								((AddressCarryingEvent)event).getSourceBankId()));
			
			else if (requestType == RequestType.Cache_Write)
			{
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 this, 
																				 this, 
																				 RequestType.Main_Mem_Write, 
																				 address);
					addressEvent.setSourceBankId(((AddressCarryingEvent)event).getDestinationBankId());
					addressEvent.setDestinationBankId(((AddressCarryingEvent)event).getDestinationBankId());
					processingElement.getPort().put(addressEvent);
				}
			}
		}
		
		//IF MISS
		else
		{			
			//Add the request to the outstanding request buffer
			boolean alreadyRequested = this.addOutstandingRequest(event, address);
			
			if (!alreadyRequested)
			{
				// access the next level
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																			 0,
																			 this, 
																			 this, 
																			 RequestType.Main_Mem_Read, 
																			 address);
				addressEvent.setSourceBankId(((AddressCarryingEvent)event).getDestinationBankId());
				addressEvent.setDestinationBankId(((AddressCarryingEvent)event).getDestinationBankId());
				processingElement.getPort().put(addressEvent);

				return;
			}
		}
	}
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		long blockAddr = addr >>> blockSizeBits;
		if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
		{
			
			System.err.println("Memory System Error : An outstanding request not found in the requesting element from line 258");
			System.out.println(missStatusHoldingRegister);
			System.exit(1);
		}
		ArrayList<Event> outstandingEvents = this.missStatusHoldingRegister.get(blockAddr);
		
		while (!/*NOT*/outstandingEvents.isEmpty())
		{				
			Event tempEvent = outstandingEvents.get(0);
			tempEvent.getRequestingElement().getPort().put
											(event.update
														(eventQ, 
														 tempEvent.getRequestingElement().getLatencyDelay() , 
														 this,
														 tempEvent.getRequestingElement(),
														 tempEvent.getRequestType()));
			//Remove the processed entry from the outstanding request list
			outstandingEvents.remove(0);
		}		
	}
}