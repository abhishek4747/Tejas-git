package generic;

public class NewUnicastEvent extends NewStackedEvent 
{
	public NewUnicastEvent(SimulationRequest simulationRequest, long eventTime,
			SimulationElement simulationElement, long tieBreaker) 
	{
		super(simulationRequest, eventTime, simulationElement, tieBreaker);
	}
}
