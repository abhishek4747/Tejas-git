package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import net.NOC.CONNECTIONTYPE;
import net.optical.OpticalNOC;
import net.optical.TopLevelTokenBus;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.MESI;
import memorysystem.MainMemory;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache.NucaType;
import config.CacheConfig;
import config.SimulationConfig;

public class CBDNuca extends NucaCache {
	
	Hashtable<Long,BroadCastRequestHandler> sendRequests = new Hashtable<Long,BroadCastRequestHandler>();
	
	public static boolean debugPrint = false;
	
	public CBDNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus) 
	{
		
		super(cacheParameters,containingMemSys, tokenbus);
		initCacheMapping();
	}
	
	Vector<Double> getDistance(int coreId,Vector<Integer> list)
	{
		int x1 = (coreId*2)/cacheRows;
    	int y1 = (coreId*2)%cacheRows;
    	Vector<Double> distance = new Vector<Double>();
    	for(int i=0;i<list.size();i++)
    	{
        	int x2 = list.get(i)/cacheRows;
        	int y2 = list.get(i)%cacheRows;
        	int x = (x1-x2)*(x1-x2);
        	int y = (y1-y2)*(y1-y2);
        	distance.add(Math.sqrt(x+y));
    	}
    	return distance;
	}
	
    Vector<Integer> sort(int coreId,Vector<Integer> list)
    {
    	Vector<Double> distance = getDistance(coreId, list);
    	for(int i=0;i<distance.size();i++)
    	{
    		int index = i;
    		double val = distance.get(i);
    		for(int j=i+1;j<distance.size();j++)
    		{
    			if( val > distance.get(j))
    			{
    				index = j;
    				val = distance.get(j);
    			}
    		}
    		if(index != i){
	    		double temp = distance.get(i);
				double temp1 = distance.get(index);
				int t = list.get(i);
				int t1 = list.get(index);
				distance.remove(i);
				distance.add(i, temp1);
				distance.remove(index);
				distance.add(index, temp);
				list.remove(i);
				list.add(i, t1);
				list.remove(index);
				list.add(index, t);
    		}
    	}
    	return list;
    }
    
    
    void fillCacheMapping()
    {
    	int numOfCores = cacheRows*cacheColumns/2;
    	for(int i=0;i<numOfCores;i++)
    	{
    		for(int j=0;j<cacheColumns;j++)
    		{
    			for(int k=0;k<cacheRows;k++)
    			{
    				if(j%2==0)
    				{
    					if(k%2 == 1)
    					{
    						cacheMapping.get(i).get(j).add(k*cacheColumns + j);
    					}
    					else
    						continue;
    				}
    				else
    				{
    					if(k%2 == 0)
    					{
    						cacheMapping.get(i).get(j).add(k*cacheColumns + j);
    					}
    					else
    						continue;
    				}
    			}
    		}
    	}
    }
    
    public void handleEvent(EventQueue eventQ, Event event) 
    {
    	if(event.getRequestType() == RequestType.Cache_Hit || 
    		  event.getRequestType() == RequestType.Cache_Miss) 
    	{
    		handleCacheBankResponses(eventQ,(AddressCarryingEvent)event);
    	} else if(event.getRequestType() == RequestType.Main_Mem_Response) 
    	{
    		handleMainMemResponse(eventQ,(AddressCarryingEvent)event);
    	}
    }
    
    protected void handleCacheBankResponses(EventQueue eventQ, AddressCarryingEvent event)
	{
		if(!sendRequests.containsKey(event.getAddress())) 
		{
			misc.Error.showErrorAndExit("Address Not Present in send Requests hash");
		} 
		
		BroadCastRequestHandler br = sendRequests.get(event.getAddress());
		if(debugPrint)System.out.println(event.getAddress() + " 3reduced number of requests for address " );
		br.num_requests--;
		if(event.getRequestType() == RequestType.Cache_Hit) {
			br.hit = true;
		}
		
		if(br.num_requests == 0) {
			if(br.hit) {
				for(int i=0; i < br.num_unique_req;i++) {
			    	ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequestsIfAvailable((AddressCarryingEvent)event);
					sendResponseToWaitingEvent(eventsToBeServed);
				}
				if(debugPrint)System.out.println(event.getAddress() +  " 4removed entry for address  from cachebankreadwrite");
				sendRequests.remove(event.getAddress());
			} else {
				AddressCarryingEvent addrEvent = new AddressCarryingEvent(eventQ,
																							MemorySystem.mainMemory.getLatencyDelay(), this, 
																							MemorySystem.mainMemory, RequestType.Main_Mem_Read,event.getAddress(), 
																							event.coreId,event.getSourceBankId(),event.getDestinationBankId());
				MemorySystem.mainMemory.getPort().put(addrEvent);
			}
		}
	}
    
    protected void handleMainMemResponse(EventQueue eventQ, AddressCarryingEvent event)
    {
		if(!sendRequests.containsKey(event.getAddress())) 
		{
			misc.Error.showErrorAndExit("Address Not Present in send Requests hash" + event.getAddress());
		} 

		BroadCastRequestHandler br = sendRequests.get(event.getAddress());
		//br.num_requests--;
		
		for(int i=0; i < br.num_unique_req;i++) {
	    	ArrayList<Event> eventsToBeServed = missStatusHoldingRegister.removeRequestsIfAvailable((AddressCarryingEvent)event);
			sendResponseToWaitingEvent(eventsToBeServed);
		}
		if(debugPrint)System.out.println(event.getAddress() +" 4removed entry for address  from maim mem response " +  br.num_unique_req);
		sendRequests.remove(event.getAddress());
		if(debugPrint)System.out.println("removed entry for address " + event.getAddress() + " from maim mem response");
		int setIndex = getSetIndex(event.getAddress());
    	Vector<Integer> bankId = integerToBankId(cacheMapping.get(event.coreId).get(setIndex).get(cacheMapping.get(event.coreId).get(setIndex).size() -1));
    	cacheBank[bankId.get(0)][bankId.get(1)].fill(event.getAddress(), MESI.EXCLUSIVE);
    }
    
    void initCacheMapping()
    {
    	fillCacheMapping();
    	int numOfCores = cacheRows*cacheColumns/2;
    	for(int i=0;i<numOfCores;i++)
    	{
    		for(int j=0;j<cacheColumns;j++)
    		{
    			Vector<Integer> set= cacheMapping.get(i).remove(j);
    			cacheMapping.get(i).add(j,sort(i,set));
    		}
    	}
    	Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>(cacheMapping.get(0));
    	cacheMapping.add(0,temp);
    	cacheMapping.add(33,(Vector<Vector<Integer>>) cacheMapping.get(0).clone());
    	cacheMapping.add(34,(Vector<Vector<Integer>>) cacheMapping.get(0).clone());
    	cacheMapping.add(35,(Vector<Vector<Integer>>) cacheMapping.get(0).clone());
    	cacheMapping.add(36,(Vector<Vector<Integer>>) cacheMapping.get(0).clone());
    	for(int i =0 ;i<cacheMapping.size();i++) {
    		System.out.println("cire number "+i);
    		for (int j=0;j<cacheMapping.get(i).size();j++)
    			System.out.println(cacheMapping.get(i).get(j));
    		System.out.println("\n\n");
    	}
    }
	
	public Vector<Integer> getNearestBankId(long address,int coreId)
	{
		int setIndex = getSetIndex(address);
		return integerToBankId(cacheMapping.get(coreId).get(setIndex).get(0));
	}
	
	public int bankIdtoIndex(int coreId,int setIndex,Vector<Integer> bankId)
	{
		int bankNumber = bankId.get(0)*cacheColumns + bankId.get(1);
		for(int i=0;i < cacheMapping.get(coreId).get(setIndex).size(); i++)
		{
			if(cacheMapping.get(coreId).get(setIndex).get(i) == bankNumber)
			{
				return i;
			}
		}
		return -1;
	}
	
	public Vector<Integer> getSourceBankId(long addr,int coreId)
	{
		return getNearestBankId(addr, coreId);
	}
	
	public Vector<Integer> getDestinationBankId(long addr,int coreId)
	{
		return getNearestBankId(addr, coreId);
	}
	

	 public boolean addEvent(AddressCarryingEvent addrEvent) {
		 if(SimulationConfig.broadcast) {
			 if(missStatusHoldingRegister.isFull())
				{
					return false;
				}
				boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(addrEvent);
				if(entryCreated)
				{
					putAndBroadCast(addrEvent);
				}
				return true;
		 } else {
			 return super.addEvent(addrEvent);
		 }
	 }
	
	void putAndBroadCast(AddressCarryingEvent addrEvent)
	{
		if(SimulationConfig.broadcast)
		{
			int setIndex = getSetIndex(addrEvent.getAddress());
			if(sendRequests.containsKey(addrEvent.getAddress())) {
				//sendRequests.get(addrEvent.getAddress()).num_requests += cacheMapping.get(addrEvent.coreId).get(setIndex).size();
				sendRequests.get(addrEvent.getAddress()).num_unique_req++;
				if(debugPrint)System.out.println(addrEvent.getAddress()+ " 1Send Request"  + cacheMapping.get(addrEvent.coreId).get(setIndex).size() + " number of unique requests "+sendRequests.get(addrEvent.getAddress()).num_unique_req + "  " +sendRequests.get(addrEvent.getAddress()).num_requests+"  for address ");
				return;
			} else {
				BroadCastRequestHandler br = new BroadCastRequestHandler(cacheMapping.get(addrEvent.coreId).get(setIndex).size(),false,1);
				sendRequests.put(addrEvent.getAddress(), br);
			}

			Vector<Integer> sourceBankId = getSourceBankId(addrEvent.getAddress(), addrEvent.coreId);
			if(debugPrint)System.out.println(addrEvent.getAddress()+ " 1Send Request"  + cacheMapping.get(addrEvent.coreId).get(setIndex).size() + " number of unique requests "+sendRequests.get(addrEvent.getAddress()).num_unique_req + "  " +sendRequests.get(addrEvent.getAddress()).num_requests+"  for address "  );
			for(int i=0;i< cacheMapping.get(addrEvent.coreId).get(setIndex).size();i++)
			{
				Vector<Integer> destinationBankId = (Vector<Integer>) integerToBankId(cacheMapping.get(addrEvent.coreId).get(setIndex).get(i)).clone();
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(addrEvent.getEventQ(),
																		   0,addrEvent.getRequestingElement(),
																		   this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(),
																		   addrEvent.getRequestType(),
																		   addrEvent.getAddress(),
																		   addrEvent.coreId,
																		   sourceBankId,destinationBankId);
				this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
				getPort().put(addressEvent);
			}
		}
	}
}

class BroadCastRequestHandler {
	int num_requests;
	boolean hit;
	int num_unique_req;
	public BroadCastRequestHandler(int num_requests,boolean hit,int num_unique_req) {
		this.num_requests = num_requests;
		this.hit = hit;
		this.num_unique_req = num_unique_req;
	}
}
