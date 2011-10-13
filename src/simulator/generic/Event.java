package generic;

import memorysystem.MemorySystem;

/*
 * Event class contains the bare-minimum that every event must contain.
 * This class must be extended for every RequestType.
 * 
 * The extendedClass would define the requestType and would contain the payLoad 
 * of the request too.This simplifies the code as we now don't have to create a 
 * separate pay-load class for each type of request. 
 */
public class Event 
{
	private long eventTime;
	RequestType requestType;
	private long priority;
	
	//Element which processes the event.
	private SimulationElement requestingElement;
	private SimulationElement processingElement;

	public Event(SimulationElement requestingElement,
			SimulationElement processingElement, RequestType requestType) 
	{
		eventTime = -1; // this should be set by the port
		this.requestingElement = requestingElement;
		this.processingElement = processingElement;
		this.requestType = requestType;
		
		this.priority = requestType.ordinal();
	}
/* NOT REQUIRED	
	public Event update(long eventTime, SimulationElement requestingElement,
			SimulationElement processingElement, long tieBreaker, RequestType requestType, Object payload)
	{
		this.eventTime = eventTime;
		this.requestingElement = requestingElement;
		this.processingElement = processingElement;
		this.tieBreaker = tieBreaker;
		this.requestType = requestType;
		this.payload = payload;
		
		//this.priority = calculatePriority(requestType);
		this.priority = requestType.ordinal();
		
		return this;
	}
*/

	//Converts request-type to priority.
	private long calculatePriority(RequestType requestType) 
	{
		//TODO: switch case for different request types would come here.
		return 0;
	}

	public long getEventTime() {
		return eventTime;
	}

	public long getPriority() {
		return priority;
	}

	public SimulationElement getRequestingElement() {
		return requestingElement;
	}
	
	protected void setRequestingElement(SimulationElement requestingElement) {
		this.requestingElement = requestingElement;
	}

	public SimulationElement getProcessingElement() {
		return processingElement;
	}

	protected void setProcessingElement(SimulationElement processingElement) {
		this.processingElement = processingElement;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	
	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}
	
	public void addEventTime(long additionTime) {
		this.setEventTime(this.eventTime + additionTime);
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}
	
	public void resetPriority(RequestType requestType)
	{
		this.priority = requestType.ordinal();
	}
	
	public RequestType getRequestType()
	{
		return requestType;
	}

	//If the event cannot be handled in the current clock-cycle,
	//then the eventPriority and eventTime will be changed and then 
	//the modified event will be added to the eventQueue which is 
	//now passed as a paramter to this function.
	//TODO handleEvent(event)
	public void handleEvent(EventQueue eventQueue)
	{
		if (processingElement == null &&
				(requestType == RequestType.Main_Mem_Read
						|| requestType == RequestType.Main_Mem_Write))
		{
			MemorySystem.handleMainMemAccess(this);
		}
		else
			processingElement.handleEvent(this);
	}
}
