package memorysystem.nuca;

import generic.Event;
import generic.EventQueue;
import generic.SimulationElement;

import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class CBDNuca extends NucaCache {
	public CBDNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) 
	{
		super(cacheParameters,containingMemSys);
		//System.out.println("CBDnuca ");
		initCacheMapping();
	}
	

    Vector<Integer> sort(int coreId,Vector<Integer> list)
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
