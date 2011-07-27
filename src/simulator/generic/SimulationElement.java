package generic;

public abstract class SimulationElement 
{
	private Port port;
	private long latency;
	
	//return the next free slot
	public long getNextSlot(long currentTime)
	{
		return port.getNextSlot(currentTime);
	}
	
	//processEvent returns whether the event can be process's in current 
	//time slot or not ??
	//if the event cannot be processed, the event parameters are changed to 
	//1) schedule in next-time slot
	//2) increase priority to avoid starvation.
	abstract SimulationRequest processRequest(SimulationRequest simulationRequest);
}