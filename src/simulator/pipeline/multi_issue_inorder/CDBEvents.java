package pipeline.multi_issue_inorder;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class CDBEvents extends Event {
	ROB rob;
	int robslot;

	public CDBEvents(EventQueue eventQ,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType,
			ROB rob,
			int robslot,
			long eventTime)
	{
		super(eventQ, eventTime, requestingElement, processingElement, requestType, -1);
		
		this.rob = rob;
		this.robslot = robslot;
	}

	public ROB getROBEntry() {
		return rob;
	}
}

