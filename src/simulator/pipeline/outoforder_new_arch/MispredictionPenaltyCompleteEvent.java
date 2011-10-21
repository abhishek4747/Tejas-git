package pipeline.outoforder_new_arch;

import generic.Event;
import generic.RequestType;
import generic.SimulationElement;

public class MispredictionPenaltyCompleteEvent extends Event {

	public MispredictionPenaltyCompleteEvent(long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement, RequestType requestType) {
		super(eventTime, requestingElement, processingElement, requestType);
	}

}