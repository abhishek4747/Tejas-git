/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

				Contributor: Mayur Harne
*****************************************************************************/
package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import net.NOC.CONNECTIONTYPE;
import net.NOC.TOPOLOGY;
import net.optical.OpticalRouter;
import java.util.Vector;
import config.CacheConfig;
import config.SystemConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache.NucaType;

public class NucaCacheBank extends Cache implements NocInterface
{
	public Router router;
	CacheConfig cacheParameters;
	boolean isLastLevel;
	boolean isFirstLevel;
	NucaType nucaType;
	TOPOLOGY topology;
	public Policy policy;
	int cacheBankRows;
	int cacheBankColumns;
	int counter = 0;
	int sendcounter =0;
	public boolean cacheBank = false;
	public int setNum;
	public static int totalNucaBankAcesses;
	protected Vector<Integer> bankId;
	NucaCache nucaCache;

	NucaCacheBank(Vector<Integer> bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,NucaCache nucaCache, NucaType nucaType)
    {
        super(cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
    	if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
    		this.router = new Router(SystemConfig.nocConfig,this);
    	else
    		this.router = new OpticalRouter(SystemConfig.nocConfig, this);
        isLastLevel = false;
        isFirstLevel = false;
        this.nucaType = nucaType;
        topology = SystemConfig.nocConfig.topology;
        policy = new Policy(nucaCache);
        this.nucaCache = nucaCache;
        this.cacheBankColumns = SystemConfig.nocConfig.getNumberOfBankColumns();
        this.cacheBankRows = SystemConfig.nocConfig.getNumberOfBankRows();
        this.bankId  = bankId;
        this.setNum = (size*1024)/(blockSize*assoc);
    }
    
    public Router getRouter()
	{
		return this.router;
	}
    
    public Vector<Integer> getBankId()
	{
		return this.bankId;
	}
    
    @Override
	public void handleEvent(EventQueue eventQ, Event event)
    {
    	if (event.getRequestType() == RequestType.Cache_Read
				|| event.getRequestType() == RequestType.Cache_Write ) 
    	{
			this.handleAccess(eventQ, (AddressCarryingEvent)event);
    	}
		else if (event.getRequestType() == RequestType.Mem_Response)
		{
			this.handleMemResponse(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Read ||
				  event.getRequestType() == RequestType.Main_Mem_Write )
		{
			this.handleMemoryReadWrite(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Response )
		{
			handleMainMemoryResponse(eventQ, event);
		}
		else {
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}

	

	
	protected void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
		/*AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		long addr = addrEvent.getAddress();
		
				
		Vector<Integer> sourceId = this.getId();
		Vector<Integer> destinationId = nucaCache.getBankId(addr);
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																	0,this, this.getRouter(), 
																	RequestType.Main_Mem_Response, 
																	addr,((AddressCarryingEvent)event).coreId,
																	sourceId,destinationId);
		this.getRouter().getPort().put(addressEvent);
		int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(addrEvent);
		nucaCache.misses += numOfOutStandingRequests;//change this value
		nucaCache.noOfRequests += numOfOutStandingRequests;//change this value
		policy.sendResponseToWaitingEvent((AddressCarryingEvent)event, this, false);*/
	}
	

	
	protected void handleMemoryReadWrite(EventQueue eventQ, Event event) {
		//System.out.println(((AddressCarryingEvent)event).getDestinationBankId() + ""+ ((AddressCarryingEvent)event).getSourceBankId());
		/*AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		Vector<Integer> sourceId = addrEvent.getSourceId();
		Vector<Integer> destinationId = ((AddressCarryingEvent)event).getDestinationId();
		
		RequestType requestType = event.getRequestType();
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
		{
			MemorySystem.mainMemoryController.getPort().put(((AddressCarryingEvent)event).updateEvent(eventQ, 
												MemorySystem.mainMemoryController.getLatencyDelay(), this, 
												MemorySystem.mainMemoryController, requestType, sourceId,
												destinationId));
		}*/
	}

	public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
		/*RequestType requestType = event.getRequestType();
		long address = event.getAddress();
		nucaCache.incrementTotalNucaBankAcesses(1);
		//Process the access
		CacheLine cl = this.processRequest(requestType, address,event);
		
		//IF HIT
		if (cl != null || nucaCache.missStatusHoldingRegister.containsWriteOfEvictedLine(address) )
		{
			//System.exit(0);
			int numOfOutStandingRequests = nucaCache.missStatusHoldingRegister.numOutStandingRequests(event);
			nucaCache.hits+=numOfOutStandingRequests; //
			nucaCache.noOfRequests += numOfOutStandingRequests;//
			policy.updateEventOnHit(event, this);
		}
		//IF MISS
		else
		{
			AddressCarryingEvent tempEvent= policy.updateEventOnMiss( (AddressCarryingEvent)event,this);
			if(tempEvent != null)
			{
				tempEvent.getProcessingElement().getPort().put(tempEvent);
			}
		}*/
	}

	@Override
	public Vector<Integer> getId() {
		
		return this.getBankId();
	}

	@Override
	public SimulationElement getSimulationElement() {
		// TODO Auto-generated method stub
		return this;
	}
	
	public int getStartIdx(long addr) {
		long SetMask =( 1 << (numSetsBits) )- 1;
		int bankNumBits = (int) (Math.log(nucaCache.cacheRows)/Math.log(2));
		int startIdx = (int) ((addr >>> (blockSizeBits+bankNumBits)) & (SetMask));
		return startIdx;
	}
}