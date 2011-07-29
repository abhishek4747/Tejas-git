package generic;

import java.util.Stack;

public class NewStackedEvent extends NewEvent 
{
	Stack<SimulationElement> snoopyStack;
	Stack<SimulationElement> mainStack;
	
	
	public NewStackedEvent(SimulationRequest simulationRequest, long eventTime,
			SimulationElement simulationElement, long tieBreaker, int noOfCores) 
	{
		super(simulationRequest, eventTime, simulationElement, tieBreaker);
		
		//create a snoopy stack for each of the core.
		snoopyStack = new Stack<SimulationElement>();
	}
}