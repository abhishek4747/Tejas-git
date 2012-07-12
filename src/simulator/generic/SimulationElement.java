package generic;

public abstract class SimulationElement implements Cloneable
{
	//a simulation element encapsulates a port.
	//all the request for the port are ported through simulationElement
	Port port;
	protected long latency;

   public Object clone()
    {
        try
        {
            // call clone in Object.
            return super.clone();
        } catch(CloneNotSupportedException e)
        {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }

	
	public SimulationElement(PortType portType,
								int noOfPorts,
								long occupancy,
								long latency,
								long frequency	//in MHz
								)
	{
		this.port = new Port(portType, noOfPorts, occupancy);
		this.latency = latency;
	}
//TODO remove this method
	public SimulationElement(PortType portType,
			int noOfPorts,
			long occupancy,
			EventQueue eq,
			long latency,
			long frequency	//in MHz
	)
	{
		this.port = new Port(portType, noOfPorts, occupancy);
		this.latency = latency;
	}
	
	//To get the time delay(due to latency) to schedule the event 
	public long getLatencyDelay()
	{
		return (this.latency /** this.stepSize*/);
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
	
	/*public long getFrequency() {
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
	}*/
	
//	public abstract void handleEvent(EventQueue eventQueue);
	public abstract void handleEvent(EventQueue eventQ, Event event);
}