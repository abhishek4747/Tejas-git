package memorysystem;

import memorysystem.CacheLine.MESI;
import generic.*;

public class BorrowSharedEvent extends Event
{
	Cache destination;
	long addr;
	
	public BorrowSharedEvent(Cache _destination, long _addr, long eventTime)
	{
		super(eventTime, 2, 0);
		
		destination = _destination;
		addr = _addr;
	}
	public void handleEvent()
	{
		CacheRequestPacket request = new CacheRequestPacket();
		request.setType(MemoryAccessType.READ);
		destination.fill(request, MESI.SHARED);
		
		Bus.endRequest();
	}
}
