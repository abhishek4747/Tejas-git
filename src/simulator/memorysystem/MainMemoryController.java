package memorysystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import net.BusInterface;
import net.NocInterface;
import net.Router;

import memorysystem.nuca.NucaCache.NucaType;
import config.Interconnect;
import config.EnergyConfig;
import config.SystemConfig;
import generic.CommunicationInterface;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.Event;
import generic.RequestType;

public class MainMemoryController extends SimulationElement
{
	public CommunicationInterface comInterface;
	Vector<Integer> nocElementId;
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
		if(SystemConfig.interconnect == Interconnect.Bus)
		{
			comInterface = new BusInterface(this);
		}
		else if(SystemConfig.interconnect == Interconnect.Noc)
		{
			comInterface = new NocInterface(SystemConfig.nocConfig, this);
		}
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
		if (event.getRequestType() == RequestType.Main_Mem_Read)
		{
			if(SystemConfig.interconnect==Interconnect.Bus)
			{	
				event.getRequestingElement().getPort().put(
						event.update(
								eventQ,
								2,//wire delay from main memory to cache
								null,
								event.getRequestingElement(),
								RequestType.Mem_Response));
			}
			else if(SystemConfig.interconnect==Interconnect.Noc)
			{
					//System.err.println("At Mem Controller " + ((AddressCarryingEvent)event).getAddress() + " " + ((AddressCarryingEvent)event).getSourceId());
					this.getRouter().getPort().put(
							new AddressCarryingEvent(
									eventQ,
									0,
									this,
									this.getRouter(),
									RequestType.Main_Mem_Response,((AddressCarryingEvent)event).getAddress(),
									event.coreId,
									((NocInterface) this.comInterface).getId(),((AddressCarryingEvent)event).getSourceId()));
			}
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
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
	public EnergyConfig calculatePower(FileWriter outputFileWriter) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		return power;
	}
}
