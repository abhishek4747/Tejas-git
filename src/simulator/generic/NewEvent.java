package generic;

public class NewEvent 
{
	private long eventTime;
	private long priority;
	private SimulationRequest simulationRequest;
	private SimulationElement simulationElement;

	//TODO: checkout if the tie-breaker should be an enum or something
	private long tieBreaker;

	
	public NewEvent(SimulationRequest simulationRequest, long eventTime,
			SimulationElement simulationElement, long tieBreaker) 
	{
		this.simulationRequest = simulationRequest;
		this.eventTime = eventTime;
		this.simulationElement = simulationElement;
		this.tieBreaker = tieBreaker;
		
		//set the priority of the event on the base of the requestType.
		this.priority = calcPriority(simulationRequest.getRequestType());
	}


	public SimulationRequest getSimulationRequest() 
	{
		return simulationRequest;
	}


	public long getEventTime() 
	{
		return eventTime;
	}


	public SimulationElement getSimulationElement() 
	{
		return simulationElement;
	}


	public long getPriority() {
		return priority;
	}


	public void setPriority(long priority) {
		this.priority = priority;
	}
	
	private long calcPriority(RequestType requestType)
	{
		return 0;
	}
	
	public long getTieBreaker()
	{
		return this.tieBreaker;
	}
}