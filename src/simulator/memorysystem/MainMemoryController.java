package memorysystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import memorysystem.nuca.NucaCacheBank;
import memorysystem.nuca.NucaCache.NucaType;
import config.PowerConfigNew;
import config.SystemConfig;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.Event;
import generic.RequestType;
import generic.Statistics;

public class MainMemoryController extends SimulationElement
{
	NucaType nucaType;
	public int numberOfMemoryControllers;
	public int[] mainmemoryControllersLocations;
	long numAccesses;
	
	public MainMemoryController(NucaType nucaType) {
		super(SystemConfig.mainMemPortType,
				SystemConfig.mainMemoryAccessPorts,
				SystemConfig.mainMemoryPortOccupancy,
				SystemConfig.mainMemoryLatency,
				SystemConfig.mainMemoryFrequency
				);
		this.nucaType = nucaType;
	}
	
	public MainMemoryController() {
		super(PortType.Unlimited,
				-1, 
				10,
				250,
				3600);
		this.nucaType = NucaType.NONE;
	}
	
	public MainMemoryController(int[] memoryControllersLocations, NucaType nucaType) 
	{
		super(PortType.Unlimited,
				-1, 
				10,
				250,
				3600);
		this.nucaType = nucaType;
		this.numberOfMemoryControllers = memoryControllersLocations.length;
		this.mainmemoryControllersLocations = memoryControllersLocations;
	}
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Main_Mem_Read)
		{
			if(nucaType == NucaType.NONE)
			{	
				event.getRequestingElement().getPort().put(
						event.update(
								eventQ,
								2,//wire delay from main memory to cache
								null,
								event.getRequestingElement(),
								RequestType.Mem_Response));
			}
			else
			{
				
				if(event.getRequestingElement().getClass() == NucaCacheBank.class){
				NucaCacheBank requestingBank =  (NucaCacheBank) event.getRequestingElement();
				requestingBank.getRouter().getPort().put(
						event.update(
								eventQ,
								event.getRequestingElement().getLatencyDelay(),
								this,
								requestingBank.getRouter(),
								RequestType.Main_Mem_Response));
				}
				else{
					SimulationElement requestingElement = event.getRequestingElement();
					Vector<Integer> sourceBankId = new Vector<Integer>(
							   ((AddressCarryingEvent)
							    (event)).
							    getDestinationId());
					Vector<Integer> destinationBankId = new Vector<Integer>(
									((AddressCarryingEvent)
								     (event)).
									 getSourceId());
					((AddressCarryingEvent)event).setSourceId(sourceBankId);
					((AddressCarryingEvent)event).setDestinationId(destinationBankId);
	//				System.out.println("From main memory" + ((AddressCarryingEvent) event).getSourceBankId() + " " + ((AddressCarryingEvent) event).getDestinationBankId());
					requestingElement.getPort().put(
							event.update(
									eventQ,
									1,
									this,
									requestingElement,
									RequestType.Main_Mem_Response));
				}
			}
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Write)
		{
			//Just to tell the requesting things that the write is completed
		}
		
		incrementNumAccesses((int)latency);
	}
	
	void incrementNumAccesses(int incrementBy)
	{
		numAccesses += incrementBy * stepSize;
	}

	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double leakagePower = SystemConfig.mainMemoryControllerPower.leakagePower;
		double dynamicPower = SystemConfig.mainMemoryControllerPower.dynamicPower;
		
		double activityFactor = (double)numAccesses
									/(double)Statistics.maxCoreCycles;
		
		PowerConfigNew power = new PowerConfigNew(leakagePower, dynamicPower * activityFactor);
		
		outputFileWriter.write("\n" + componentName + " :\n" + power + "\n");
		
		return power;
	}
}
