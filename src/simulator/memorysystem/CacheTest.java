package memorysystem;

import memorysystem.nuca.NucaCache.NucaType;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import config.CacheConfig.WritePolicy;

public class CacheTest extends SimulationElement{
	
	public CacheTest() {
		super(PortType.FirstComeFirstServe,
				2, 
				2,
				2,
				3600);
	}

	static Cache cache;
	static EventQueue eventQueue;
	static int responseReceived;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int size = 16;
		int associativity = 2;
		int blockSize = 1;
		WritePolicy writePolicy = WritePolicy.WRITE_THROUGH;
		int mshrSize = 1;
		
		cache = new Cache(
				size,
				associativity,
				blockSize,
				writePolicy,
				mshrSize);
		
		MemorySystem.mainMemory = new MainMemory();
		
		eventQueue = new EventQueue();
		GlobalClock.setStepSize(1);
		
		responseReceived = mshrSize;
		
		CacheTest tester = new CacheTest();
		
		for(int j = 0; j < 5; j++)
		//for(int i = 0; i < 16*1024; i++)
		for(int i = 0; i < 2; i++)
		{
			/*int address = i;
			if(j%2 == 0)
			{
				address = i;
			}else
			{
				address = 16*1024 + i;
			}*/
			if(responseReceived > 0)
			{
				tester.issueRequestToCache(i*16*1024);
				responseReceived--;
			}
			else
			{
				i--;
			}
			eventQueue.processEvents();
			GlobalClock.incrementClock();
		}
		
		while(responseReceived != mshrSize)
		{
			eventQueue.processEvents();
			GlobalClock.incrementClock();
		}
		
		System.out.println("no of requests = " + cache.noOfRequests);
		System.out.println("no of hits = " + cache.hits);
		System.out.println("no of misses = " + cache.misses);
		cache.getMissStatusHoldingRegister().dump();

	}
	
	//To issue the request to instruction cache
	public void issueRequestToCache(long address)
	{
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(eventQueue,
				 cache.getLatencyDelay(),
				 this, 
				 cache,
				 RequestType.Cache_Write, 
				 address,
				 0);

		//attempt issue to lower level cache
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		cache.addEvent(clone);
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		responseReceived++;
		
	}

}
