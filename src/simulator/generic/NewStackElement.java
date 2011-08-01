package generic;

public class NewStackElement 
{
	private SimulationElement simulationElement;
	private SimulationRequest simulationRequest;
	
	
	public NewStackElement(SimulationElement simulationElement,
			SimulationRequest simulationRequest) 
	{
		this.simulationElement = simulationElement;
		this.simulationRequest = simulationRequest;
	}


	public SimulationElement getSimulationElement() {
		return simulationElement;
	}


	public SimulationRequest getSimulationRequest() {
		return simulationRequest;
	}
}