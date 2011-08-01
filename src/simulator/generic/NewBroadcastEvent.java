package generic;

public class NewBroadcastEvent extends NewStackedEvent 
{
	private int noOfReceivers;
	private int howManyCompleted;

	public NewBroadcastEvent(SimulationRequest simulationRequest,
			long eventTime, SimulationElement simulationElement, long tieBreaker) 
	{
		super(simulationRequest, eventTime, simulationElement, tieBreaker);
		
		howManyCompleted = 0;
	}
	
	boolean isBroadcastEventOver()
	{
		return (howManyCompleted == noOfReceivers);
	}
	
	void completedEvent()
	{
		howManyCompleted++;
	}
}