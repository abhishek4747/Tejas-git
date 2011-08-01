package generic;

import java.util.Stack;

public abstract class NewStackedEvent extends NewEvent 
{
	protected Stack<NewStackElement> stack;
	
	
	public NewStackedEvent(SimulationRequest simulationRequest, long eventTime,
			SimulationElement simulationElement, long tieBreaker) 
	{
		super(simulationRequest, eventTime, simulationElement, tieBreaker);
	}
}