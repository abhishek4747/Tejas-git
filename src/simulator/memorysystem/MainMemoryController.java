package memorysystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import net.NocInterface;
import net.Router;

import memorysystem.nuca.NucaCacheBank;
import memorysystem.nuca.NucaCache.NucaType;
import config.Interconnect;
import config.EnergyConfig;
import config.SystemConfig;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.Event;
import generic.RequestType;
import generic.Statistics;

public class MainMemoryController extends SimulationElement implements NocInterface
{
	NucaType nucaType;
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
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Cache_Read)
		{
			AddressCarryingEvent e = new AddressCarryingEvent(eventQ, 0,
					this, event.getRequestingElement(),	RequestType.Mem_Response,
					((AddressCarryingEvent)event).getAddress());
			
			getNetworkInterface().put(e);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Write)
		{
			//Just to tell the requesting things that the write is completed
		}
		
		incrementNumAccesses();
	}
	
	void incrementNumAccesses()
	{
		numAccesses += 1;
	}
	
	@Override
	public SimulationElement getSimulationElement() {
		// TODO Auto-generated method stub
		return this;
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
	
	public EnergyConfig calculateEnergy(FileWriter outputFileWriter) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		return power;
	}
}
