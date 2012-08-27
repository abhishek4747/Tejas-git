package generic;

import java.util.Vector;

import emulatorinterface.RunnableThread;

import memorysystem.AddressCarryingEvent;

public class CoreBcastBus extends SimulationElement{

	EventQueue eventQ;
	Vector<Core> coreList = new Vector<Core>();
	public Vector<Integer> toResume =  new Vector<Integer>();
	
	public CoreBcastBus() {
		super(PortType.Unlimited, 1, 1, null, 1, 1);
		// TODO Auto-generated constructor stub
	}
	
	public void setEventQueue(EventQueue eventQ)
	{
		this.eventQ = eventQ;
	}
	
	public Core getCore(int id){
		return coreList.elementAt(id);
	}
	
	public void addToCoreList(Core core){
		this.coreList.add(core);
	}
	public void addToResumeCore(int id){
		this.toResume.add(id);
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
		if(event.getRequestType() == RequestType.PIPELINE_RESUME){
			for(int i : toResume){
				coreList.get(i).activatePipeline();
				RunnableThread.setThreadState(i,false);
//				System.out.println("Resuming thread " + i);
			}
			toResume.clear();
		}
		else{
			coreList.get((int) ((AddressCarryingEvent)event).getAddress()).sleepPipeline();
		}
	}

}
