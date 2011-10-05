package generic;

public abstract class SimulationElement
{
	//a simulation element encapsulates a port.
	//all the request for the port are ported through simulationElement
	Port port;
	protected long latency;
	long frequency;								//in MHz
	int stepSize;

	public SimulationElement(PortType portType,
								int noOfPorts,
								long occupancy,
								EventQueue eventQueue,
								long latency,
								long frequency	//in MHz
								)
	{
		this.port = new Port(portType, noOfPorts, occupancy, eventQueue);
		this.latency = latency;
		this.frequency = frequency;
	}
	
	//To get the time delay(due to latency) to schedule the event 
	public long getLatencyDelay()
	{
		return (this.latency * this.stepSize);
	}
	
	public long getLatency() 
	{
		return this.latency;
	}
	
	protected void setLatency(long latency) {
		this.latency = latency;
	}

	public Port getPort()
	{
		return this.port;
	}	
	
	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public int getStepSize() {
		return stepSize;
	}

	public void setStepSize(int stepSize) {
		this.stepSize = stepSize;
	}
	
//	public abstract void handleEvent(EventQueue eventQueue);
	public abstract void handleEvent(Event event);
}