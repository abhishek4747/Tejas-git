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
import memorysystem.MESI;
import memorysystem.MemorySystem;

public class NucaCacheBank extends Cache
{
	Router router;
	CacheConfig cacheParameters;
	protected Hashtable<Long, ArrayList<Event>> forwardedRequests;

    NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
        this.router = new Router(bankId,cacheParameters.numberOfBuffers);
        forwardedRequests =new Hashtable<Long, ArrayList<Event>>();
    }
    
    public Router getRouter()
	{
		return this.router;
	}
	
	public CacheLine processRequest(RequestType requestType, long addr)
	{
		noOfRequests++;
		//boolean isHit;
		/* access the Cache */
		CacheLine ll = null;
		if(requestType == RequestType.Cache_Read || requestType == RequestType.Cache_Read_from_iCache)
			ll = this.read(addr);
		else if (requestType == RequestType.Cache_Write)
			ll = this.write(addr);
		
		if(ll == null)
		{
			/* Miss */
//			if (!(request.isWriteThrough()))//TODO For testing purposes only
			this.misses++;
		} 
		else 
		{
			/* Hit */
			/* do nothing */
//			if (!(request.isWriteThrough()))//TODO For testing purposes only
			this.hits++;				
		}
		return ll;
	}

	
	public CacheLine fill(long addr) //Returns a copy of the evicted line
	{
		CacheLine evictedLine = null;
		
		/* remove the block size */
		long tag = addr >>> this.blockSizeBits;

		/* search all the lines that might match */
		long laddr = tag >>> this.assocBits;
		laddr = laddr << assocBits; // replace the associativity bits with zeros.

		/* remove the tag portion */
		laddr = laddr & numLinesMask;

		/* find any invalid lines -- no eviction */
		CacheLine fillLine = null;
		boolean evicted = false;
		for (int idx = 0; idx < assoc; idx++) 
		{
			CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
			if (!(ll.isValid())) 
			{
				fillLine = ll;
				break;
			}
		}
		
		/* LRU replacement policy -- has eviction*/
		if (fillLine == null) 
		{
			evicted = true; // We need eviction in this case
			double minTimeStamp = Double.MAX_VALUE;
			for(int idx=0; idx<assoc; idx++) 
			{
				CacheLine ll = this.lines[(int)(laddr + (long)(idx))];
				if(minTimeStamp > ll.getTimestamp()) 
				{
					minTimeStamp = ll.getTimestamp();
					fillLine = ll;
				}
			}
		}

		/* if there has been an eviction */
		if (evicted) 
		{
			evictedLine = fillLine.copy();
			
			//if (fillLine.getPid() != request.getThreadID()) //TODO I didn't understand the logic
			//{
				/* increase eviction count */
				this.evictions++;

				/* log the line */
				evictedLines.addElement(fillLine.getTag());
			//}
		}
		/* This is the new fill line */
		fillLine.setState(MESI.SHARED);
		//fillLine.setValid(true);
		mark(fillLine, tag);
		return evictedLine;
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
					|| event.getRequestType() == RequestType.Cache_Write
					|| event.getRequestType() == RequestType.Cache_Read_from_iCache)
				this.handleAccess(eventQ, event);
			else if (event.getRequestType() == RequestType.Mem_Response)
				this.handleMemResponse(eventQ, event);
			else if (event.getRequestType() == RequestType.Main_Mem_Read ||  
					event.getRequestType() == RequestType.Main_Mem_Write)
				this.handleMemoryReadWrite(eventQ,event);
			else if (event.getRequestType() == RequestType.Main_Mem_Response)
				this.handleMainMemoryResponse(eventQ,event);
		}
		else
		{
			nextID = router.RouteComputation(currentId, destinationId, RoutingAlgo.ALGO.SIMPLE);
//			if(router.CheckNeighbourBuffer(nextID))
			{
				//post event to nextID
				this.router.GetNeighbours().elementAt(nextID.ordinal()).getPort().put(
						event.update(
								eventQ,
								0,
								this, 
								this.router.GetNeighbours().elementAt(nextID.ordinal()),
								requestType));
				this.router.FreeBuffer();
			}
/*			else
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
*/		}
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
			Vector<Integer> sourceBankId = new Vector<Integer>(
															   ((AddressCarryingEvent)
															    (event)).
															    getDestinationBankId());
			Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     (event)).
																	 getSourceBankId());
			
			addressEvent.setSourceBankId(sourceBankId);
			addressEvent.setDestinationBankId(destinationBankId);
			this.getPort().put(addressEvent);
		}
/*		long blockAddr = addr >>> this.blockSizeBits;
			if (!NOTthis.missStatusHoldingRegister.containsKey(blockAddr))
			{
				System.err.println("Memory System Error : An outstanding request not found in the requesting element from line 128");
				//System.out.println("error in bankid  "+router.getBankId());
				System.exit(1);
			}
			
			ArrayList<Event> outstandingRequestList = this.missStatusHoldingRegister.get(blockAddr);
			
*/			//while (!/*NOT*/outstandingRequestList.isEmpty())
			{				
				if (event.getRequestType() == RequestType.Cache_Write ||
					((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Write	)
				{
					if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
					{
						AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																					 0,
																					 this, 
																					 this, 
																					 RequestType.Main_Mem_Write, 
																					 addr);
						Vector<Integer> sourceBankId = new Vector<Integer>(
																		   ((AddressCarryingEvent)
																		    (event)).
																		    getDestinationBankId());
						Vector<Integer> destinationBankId = new Vector<Integer>(
																			((AddressCarryingEvent)
																		     (event)).
																			 getSourceBankId());

						addressEvent.setSourceBankId(sourceBankId);
						addressEvent.setDestinationBankId(destinationBankId);
						processingElement.getPort().put(addressEvent);
					}				
				}
				else if(event.getRequestType() == RequestType.Cache_Read ||
						((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Read ||
						event.getRequestType() == RequestType.Cache_Read_from_iCache ||
						((AddressCarryingEvent)event).oldRequestType == RequestType.Cache_Read_from_iCache)
				{
					Vector<Integer> sourceBankId = new Vector<Integer>(
																	   ((AddressCarryingEvent)
																	    event).
																	    getDestinationBankId());
					Vector<Integer> destinationBankId = new Vector<Integer>(
							   												((AddressCarryingEvent)
							   											     event).
							   												 getSourceBankId());
					this.getPort().put(
							((AddressCarryingEvent)event).updateEvent(
									eventQ,
									0, //For same cycle response //outstandingRequestList.get(0).getRequestingElement().getLatencyDelay(),
									this,
									this,
									RequestType.Mem_Response,									
									sourceBankId,
									destinationBankId));
				}
				else
				{
					System.err.println("Cache Error : A request was of type other than Cache_Read or Cache_Write from line 164");
					System.exit(1);
				}
				
				//Remove the processed entry from the outstanding request list
//				outstandingRequestList.remove(0);
			}
	}

	private void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		Vector<Integer> sourceBankId = new Vector<Integer>(
														   ((AddressCarryingEvent)
														    event).
														    getDestinationBankId());
		Vector<Integer> destinationBankId = new Vector<Integer>(
															((AddressCarryingEvent)
														     event).
															 getSourceBankId());
		
		RequestType requestType = event.getRequestType();
		MemorySystem.mainMemory.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
															MemorySystem.mainMemory.getLatencyDelay(), 
															event.getProcessingElement(), 
															MemorySystem.mainMemory, 
															requestType,
															sourceBankId,
															destinationBankId));		
		//this.router.FreeBuffer();
	}

	protected void handleAccess(EventQueue eventQ, Event event)
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
			if (requestType == RequestType.Cache_Read || requestType == RequestType.Cache_Read_from_iCache ) 
			{
			
				//Just return the read block
				Vector<Integer> sourceBankId = new Vector<Integer>(
																   ((AddressCarryingEvent)
																    (event)).
																    getDestinationBankId());
				Vector<Integer> destinationBankId = new Vector<Integer>(
																	((AddressCarryingEvent)
																     (event)).
																	 getSourceBankId());

				requestingElement.getPort().put(
						((AddressCarryingEvent)event).updateEvent(
								eventQ,
								processingElement.getLatencyDelay(),
								this,
								this,
								RequestType.Mem_Response,
								sourceBankId,
								destinationBankId));
			}
			else if (requestType == RequestType.Cache_Write)
			{
				if (this.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					Vector<Integer> sourceBankId = new Vector<Integer>(
																	   ((AddressCarryingEvent)
																	    (event)).
																	    getDestinationBankId());
					Vector<Integer> destinationBankId = new Vector<Integer>(
																		((AddressCarryingEvent)
																	     (event)).
																		 getSourceBankId());

					AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																				 0,
																				 this, 
																				 this, 
																				 RequestType.Main_Mem_Write, 
																				 address);
					addressEvent.setSourceBankId(sourceBankId);
					addressEvent.setDestinationBankId(destinationBankId);
					processingElement.getPort().put(addressEvent);
				}
			}
		}
		
		//IF MISS
		else
		{			
			//Add the request to the outstanding request buffer
//			boolean alreadyRequested = this.addOutstandingRequest(event, address);
			//System.out.println("added a new event in bankid " + router.getBankId());
//			if (!alreadyRequested)
			{
				// access the next level
				Vector<Integer> sourceBankId =new Vector<Integer>(((AddressCarryingEvent)event).getDestinationBankId());
				Vector<Integer> destinationBankId = new Vector<Integer>(((AddressCarryingEvent)event).getSourceBankId());
				((AddressCarryingEvent)event).oldRequestType = event.getRequestType();
				this.getPort().put(((AddressCarryingEvent)event).
																updateEvent(eventQ, 
																		    0,
																		    this, 
																		    this, 
																		    RequestType.Main_Mem_Read, 
																		    sourceBankId, 
																		    destinationBankId));
/*				AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQ,
																			 0,
																			 this, 
																			 this, 
																			 RequestType.Main_Mem_Read, 
																			 address);
				addressEvent.setSourceBankId(sourceBankId);
				addressEvent.setDestinationBankId(destinationBankId);
				this.getPort().put(addressEvent);
	*/			return;
			}
		}
	}
	
	protected void handleMemResponse(EventQueue eventQ, Event event)
	{
		//long addr = ((AddressCarryingEvent)(event)).getAddress();
		//System.out.println("accessing from nuca bank" + addr);
		//long blockAddr = addr >>> blockSizeBits;
		//if (!/*NOT*/this.forwardedRequests.containsKey(blockAddr))
		/*{
			
			System.err.println("Memory System Error : An outstanding request not found in the requesting element from line 258");
			//System.out.println(forwardedRequests);
			System.exit(1);
		}*/
		//ArrayList<Event> outstandingEvents = this.forwardedRequests.get(blockAddr);
		
		//while (!/*NOT*/outstandingEvents.isEmpty())
		{				
			//Event tempEvent = outstandingEvents.get(0);
			((AddressCarryingEvent)event).oldRequestingElement.getPort().put
											(event.update
														(eventQ, 
														((AddressCarryingEvent)event).oldRequestingElement.getLatencyDelay() , 
														 this,
														 ((AddressCarryingEvent)event).oldRequestingElement,
														 event.getRequestType()));
			//Remove the processed entry from the outstanding request list
			//outstandingEvents.remove(0);
		}
		//this.forwardedRequests.remove(blockAddr);
//		this.router.FreeBuffer();
	}
}