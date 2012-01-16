package memorysystem.nuca;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import memorysystem.*;
import java.util.ArrayList;
import java.util.Vector;
import config.CacheConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;


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
			if (event.getRequestType() == RequestType.Cache_Read
					|| event.getRequestType() == RequestType.Cache_Write)
				this.handleAccess(eventQ, event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.Main_Mem_Read ||  
					event.getRequestType() == RequestType.Main_Mem_Write)
				this.handleMemoryReadWrite(eventQ,event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMainMemoryResponse(eventQ,event);
		}
		else
		{
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
	
	private void handleMainMemoryResponse(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		SimulationElement processingElement = event.getProcessingElement();
		SimulationElement requestingElement = event.getRequestingElement();
		CacheLine evictedLine = this.fill(addr);
		//if condition to be  changed
		if (evictedLine != null)
		{
			((AddressCarryingEvent)event).setAddress(evictedLine.getTag() << this.blockSizeBits);//this address is not correct to be checked 
			processingElement.getPort().put(
					((AddressCarryingEvent)event).updateEvent(
							eventQ,
							0,
							this,
							this,
							RequestType.Main_Mem_Write,
							((AddressCarryingEvent)event).getDestinationBankId(),
							((AddressCarryingEvent)event).getSourceBankId()));

		}
		long blockAddr = addr >>> this.blockSizeBits;
			if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element");
				System.exit(1);
			}
			
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
			
			while (!/*NOT*/outstandingRequestList.isEmpty())
			{				
				if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Write)
				{
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						//Handle in any case (Whether requesting element is LSQ or cache)
						//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
						long address = ((AddressCarryingEvent)(event)).getAddress();
						processingElement.getPort().put(
								((AddressCarryingEvent)event).updateEvent(
										eventQ,
										0,
										this,
										this,
										RequestType.Main_Mem_Write,
										(Vector<Integer>)((AddressCarryingEvent)event).getDestinationBankId().clone(),
										(Vector<Integer>)((AddressCarryingEvent)event).getSourceBankId().clone()));
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
									(Vector<Integer>)((AddressCarryingEvent)(outstandingRequestList.get(0))).getDestinationBankId().clone(),
									(Vector<Integer>)((AddressCarryingEvent)(outstandingRequestList.get(0))).getSourceBankId().clone()));
				}
				else
				{
					System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write");
					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
				outstandingRequestList.remove(0);
			}
	}

	private void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		long address = ((AddressCarryingEvent)event).getAddress();
		RequestType requestType = event.getRequestType();
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(
													eventQ,
													MemorySystem.mainMemory.getLatencyDelay(),
													this,
													MemorySystem.mainMemory,
													requestType,
													address);
		Vector<Integer> sourceBankId = (Vector<Integer>) addressEvent.getDestinationBankId().clone();
		Vector<Integer> destinationBankId = (Vector<Integer>) addressEvent.getSourceBankId().clone();
		addressEvent.setDestinationBankId(destinationBankId);
		addressEvent.setSourceBankId(sourceBankId);		
		MemorySystem.mainMemory.getPort().put(addressEvent);		
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
				//Write the data to the cache block (Do Nothing)
				
				//If the cache level is Write-through
				//to be put in Nuca Cache
				/*if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					MemorySystem.mainMemory.getPort().put(
									event.update(
											eventQ,
											MemorySystem.mainMemory.getLatencyDelay(),
											this,
											MemorySystem.mainMemory,
											RequestType.Main_Mem_Write));
										
				}*/
				
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					requestingElement.getPort().put(
							((AddressCarryingEvent)event).updateEvent(
									eventQ,
									processingElement.getLatencyDelay(),
									this,
									this,
									RequestType.Main_Mem_Write,
									((AddressCarryingEvent)event).getDestinationBankId(),
									((AddressCarryingEvent)event).getSourceBankId()));
				}
				else
				{
					Core.outstandingMemRequests--;
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
				requestingElement.getPort().put(
						((AddressCarryingEvent)event).updateEvent(
								eventQ,
								processingElement.getLatencyDelay(),
								this,
								this,
								RequestType.Main_Mem_Read,
								((AddressCarryingEvent)event).getDestinationBankId(),
								((AddressCarryingEvent)event).getSourceBankId()));

				return;
			}
		}
	}
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		/*some thing to be done for mem_response for upper level cache */
		
		long addr = ((AddressCarryingEvent)(event)).getAddress();
		SimulationElement processingElement = event.getProcessingElement();
		SimulationElement requestingElement = event.getRequestingElement();
		long blockAddr = addr >>> this.blockSizeBits;
		if (!/*NOT*/this.missStatusHoldingRegister.containsKey(blockAddr))
		{
			System.err.println("Memory System Error : An outstanding request not found in the requesting element");
			System.exit(1);
		}
		
		ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
		
		while (!/*NOT*/outstandingRequestList.isEmpty())
		{				
			if (outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Write)
			{
				//Write the value to the block (Do Nothing)
				//Handle further writes for Write through
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					//Handle in any case (Whether requesting element is LSQ or cache)
					//TODO : handle write-value forwarding (for Write-Through and Coherent caches)
					long address = ((AddressCarryingEvent)(event)).getAddress();
					processingElement.getPort().put(
							((AddressCarryingEvent)event).updateEvent(
									eventQ,
									0,
									this,
									this,
									RequestType.Main_Mem_Write,
									(Vector<Integer>)((AddressCarryingEvent)event).getDestinationBankId().clone(),
									(Vector<Integer>)((AddressCarryingEvent)event).getSourceBankId().clone()));
					
				}				
			}
			else if(outstandingRequestList.get(0).getRequestType() == RequestType.Cache_Read)
			{
				outstandingRequestList.get(0).getRequestingElement().getPort().put(
						outstandingRequestList.get(0).update(
								eventQ,
								0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
								this,
								outstandingRequestList.get(0).getRequestingElement(),
								RequestType.Mem_Response));
			}
			else
			{
				System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write");
				System.exit(1);
			}
			
			//Remove the processed entry from the outstanding request list
			outstandingRequestList.remove(0);
		}		
	}
}