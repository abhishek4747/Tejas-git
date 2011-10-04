package generic;

public abstract class SimulationElement
{
	//a simulation element encapsulates a port.
	//all the request for the port are ported through simulationElement
	Port port;
	protected Time_t latency;
	long frequency;								//in MHz
	int stepSize;

	public SimulationElement(PortType portType,
								int noOfPorts,
								Time_t occupancy,
								NewEventQueue newEventQueue,
								Time_t latency,
								long frequency	//in MHz
								)
	{
		this.port = new Port(portType, noOfPorts, occupancy, newEventQueue);
		this.latency = latency;
		this.frequency = frequency;
	}
	
	//To get the time delay(due to latency) to schedule the event 
	public Time_t getLatencyDelay()
	{
		return (new Time_t(this.latency.getTime() * this.stepSize));
	}
	
	public Time_t getLatency() 
	{
		return this.latency;
	}
	
	protected void setLatency(Time_t latency) {
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
}