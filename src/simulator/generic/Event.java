package generic;

/**
 * all events are with respect to a particular instruction
 * (a particular ROB entry, to be more precise)
 */

public abstract class Event {
	
	private long eventTime;
	private int priority;
	private int tieBreaker;
	
	public Event()
	{
		eventTime = -1;
		priority = -1;
	}
	
	public Event(long _eventTime, int _priority, int _tieBreaker)
	{
		eventTime = _eventTime;
		priority = _priority;
		tieBreaker = _tieBreaker;
	}
	
	public long getEventTime()
	{
		return eventTime;
	}
	
	public void setEventTime(long _eventTime)
	{
		eventTime = _eventTime;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public void setPriority(int _priority)
	{
		priority = _priority;
	}

	public int getTieBreaker() {
		return tieBreaker;
	}

	public void setTieBreaker(int tieBreaker) {
		this.tieBreaker = tieBreaker;
	}	
	
	abstract public void handleEvent();
}

//					event 							priority
//------------------------------------------------------------
//         ExecutionCompleteEvent					    5
//      FunctionalUnitAvailableEvent					4
//           RenameCompleteEvent						3
//    AllocateDestinationRegisterEvent					2
//			 DecodeCompleteEvent						1