package memorysystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import net.NocInterface;
import net.Router;

import memorysystem.nuca.NucaCacheBank;
import memorysystem.nuca.NucaCache.NucaType;
import config.Interconnect;
import config.PowerConfigNew;
import config.SystemConfig;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.Event;
import generic.RequestType;
import generic.Statistics;

public class MainMemoryController extends SimulationElement implements NocInterface
{
	Router router;
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
		this.router = new Router(SystemConfig.nocConfig, this);
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
									this.getId(),((AddressCarryingEvent)event).getSourceId()));
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
	
	@Override
	public Router getRouter() {
		// TODO Auto-generated method stub
		return router;
	}

	@Override
	public Vector<Integer> getId() {
		// TODO Auto-generated method stub
		return nocElementId;
	}
	public void setId(Vector<Integer> id) {
		// TODO Auto-generated method stub
		nocElementId = id;
	}
	@Override
	public SimulationElement getSimulationElement() {
		// TODO Auto-generated method stub
		return this;
	}

	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		PowerConfigNew power = new PowerConfigNew(SystemConfig.mainMemoryControllerPower, numAccesses);
		power.printPowerStats(outputFileWriter, componentName);
		return power;
	}
	public PowerConfigNew calculatePower(FileWriter outputFileWriter) throws IOException
	{
		PowerConfigNew power = new PowerConfigNew(SystemConfig.mainMemoryControllerPower, numAccesses);
		return power;
	}
}
