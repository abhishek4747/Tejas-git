package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;

import java.util.Vector;

import net.NOC.CONNECTIONTYPE;
import net.optical.OpticalNOC;
import net.optical.TopLevelTokenBus;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.nuca.NucaCache.NucaType;
import config.CacheConfig;
import config.SimulationConfig;

public class CBDNuca extends NucaCache {
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
//    	for(int i =0 ;i<cacheMapping.size();i++) {
//    		System.out.println("cire number "+i);
//    		for (int j=0;j<cacheMapping.get(i).size();j++)
//    			System.out.println(cacheMapping.get(i).get(j));
//    		System.out.println("\n\n");
//    	}
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
	
	/*public boolean addEvent(AddressCarryingEvent addressEvent)
	{
		SimulationElement requestingElement = addressEvent.getRequestingElement();
		long address = addressEvent.getAddress();
		Vector<Integer> sourceBankId = getSourceBankId(address,addressEvent.coreId);
		Vector<Integer> destinationBankId = getDestinationBankId(address,addressEvent.coreId);
		addressEvent.oldRequestingElement = (SimulationElement) requestingElement.clone();
		addressEvent.setDestinationBankId(destinationBankId);
		addressEvent.setSourceBankId(sourceBankId);
		addressEvent.setProcessingElement(this.cacheBank[destinationBankId.get(0)][destinationBankId.get(1)]);
		addressEvent.oldSourceBankId = (Vector<Integer>) sourceBankId.clone();
		if(missStatusHoldingRegister.isFull())
		{
			return false;
		}
				
		boolean entryCreated = missStatusHoldingRegister.addOutstandingRequest(addressEvent);
		if(entryCreated)
		{
			if(this.cacheBank[0][0].cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
			{
				if(SimulationConfig.broadcast)
				{
					int setIndex = getSetIndex(address);
					for(int i=0;i< cacheMapping.get(addressEvent.coreId).get(setIndex).size();i++)
					{
						destinationBankId = (Vector<Integer>) integerToBankId(cacheMapping.get(addressEvent.coreId).get(setIndex).get(i)).clone();
						//System.out.println("added event to cache bank "+ destinationBankId + "address ="+ addressEvent.getAddress());
						AddressCarryingEvent addrEvent = new AddressCarryingEvent(addressEvent.getEventQ(),
																				   0,requestingElement,
																				   this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(),
																				   addressEvent.getRequestType(),
																				   addressEvent.getAddress(),
																				   addressEvent.coreId,
																				   sourceBankId,destinationBankId);
						addrEvent.index = i+1;
						if(requestingElement.getClass() != Cache.class)
						{
							System.err.println(" requesting element other than cache ");
						}
						addrEvent.oldRequestingElement = (SimulationElement) requestingElement.clone();
						addrEvent.oldSourceBankId = (Vector<Integer>) sourceBankId.clone();
						this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().getPort().put(addrEvent);
					}
				}
				else
				{
					this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
					getPort().put(addressEvent.
											updateEvent(addressEvent.getEventQ(), 
														0,
														requestingElement, 
														this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
														addressEvent.getRequestType(), 
														sourceBankId, 
														destinationBankId));
				}
			}
			else{
//				System.out.println("Event to NOC" + "from" + sourceBankId + "to" +destinationBankId + "with address" + address);
				((OpticalNOC)this.noc).entryPoint.
				getPort().put(addressEvent.
										updateEvent(addressEvent.getEventQ(), 
													0,//to be  changed to some constant(wire delay) 
													requestingElement, 
													((OpticalNOC)this.noc).entryPoint, 
													addressEvent.getRequestType(), 
													sourceBankId, 
													destinationBankId));
			}
			
		}
		return true;
	}*/
	
	/* void putEventToRouter(AddressCarryingEvent addrEvent)
		{
			long address = addrEvent.getAddress();
			Vector<Integer> sourceBankId = getSourceBankId(address,addrEvent.coreId);
			Vector<Integer> destinationBankId = getDestinationBankId(address,addrEvent.coreId);
			AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(addrEvent.getEventQ(),
																									0,this, this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(), 
																									addrEvent.getRequestType(), address,addrEvent.coreId,
																									sourceBankId,destinationBankId);
			eventToBeSent.oldSourceBankId = new Vector<Integer>(sourceBankId);
			if(this.cacheBank[0][0].cacheParameters.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
			{
				this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter().
				getPort().put(eventToBeSent);
			}
			else
			{
				((OpticalNOC)this.noc).entryPoint.getPort().put(eventToBeSent);
			}
		}*/
	
	void putAndBroadCast(AddressCarryingEvent addrEvent)
	{
		if(SimulationConfig.broadcast)
		{
			int setIndex = getSetIndex(addrEvent.getAddress());
			for(int i=0;i< cacheMapping.get(addrEvent.coreId).get(setIndex).size();i++)
			{
				Vector<Integer> sourceBankId = getSourceBankId(addrEvent.getAddress(), addrEvent.coreId);
				Vector<Integer> destinationBankId = (Vector<Integer>) integerToBankId(cacheMapping.get(addrEvent.coreId).get(setIndex).get(i)).clone();
				AddressCarryingEvent addressEvent = new AddressCarryingEvent(addrEvent.getEventQ(),
																		   0,addrEvent.getRequestingElement(),
																		   this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].getRouter(),
																		   addrEvent.getRequestType(),
																		   addrEvent.getAddress(),
																		   addrEvent.coreId,
																		   sourceBankId,destinationBankId);
				addressEvent.index = i+1;
				addressEvent.oldRequestingElement = (SimulationElement) addrEvent.getRequestingElement().clone();
				addressEvent.oldSourceBankId = (Vector<Integer>) sourceBankId.clone();
				cacheBank[destinationBankId.get(0)][destinationBankId.get(1)].handleAccess(addressEvent.getEventQ(),addressEvent);
			}
		}
	}
}