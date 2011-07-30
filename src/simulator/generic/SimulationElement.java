package generic;

public abstract class SimulationElement 
{
	//a simulation element encapsulates a port.
	//all the request for the port are ported through simulationElement
	private Port port;
	
	//processEvent returns whether the event can be process's in current 
	//time slot or not ??
	//if the event cannot be processed, the event parameters are changed to 
	//1) schedule in next-time slot
	//2) increase priority to avoid starvation.
	abstract SimulationRequest processRequest(SimulationRequest simulationRequest);
	
	public SimulationElement(int noOfPorts, int occupancy)
	{
		port = new Port(int noOfPorts, int occupancy);
	}
	
	//returns the next available slot.
	Time_t getNextSlot()
	{
		return port.getNextSlot();
	}
	
	Time_t getNextSlot(Time_t occupancyRequired)
	{
		return port.getNextSlot(occupancyRequired);
	}
	
	//returns the next available slot for booking the port for n slots
	Time_t occupySlots(int noOfSlots)
	{
		return port.occupySlots(noOfSlots);
	}
	
	//returns the next slot without booking anything
	Time_t calculateNextSlot()
	{
		return port.calculateNextSlot();
	}
	
	//locks the port for n cycles
	void lockForNCycles(Time_t noOfCycles)
	{
		return port.lockForNCycles(noOfCycles);
	}
}