package generic;

public abstract class SimulationElement
{
	//a simulation element encapsulates a port.
	//all the request for the port are ported through simulationElement
	Port port;
	protected Time_t latency;
	
	
	public SimulationElement(int noOfPorts, Time_t occupancy, Time_t latency)
	{
		port = new Port(noOfPorts, occupancy);
		this.latency = latency;
	}
	
	public Time_t getLatency() 
	{
		return latency;
	}
}