package generic;

public class CachePullEvent extends Event {

	public CachePullEvent(
							EventQueue eventQ,
							long eventTime,
							SimulationElement requestingElement,
							SimulationElement processingElement,
							RequestType requestType)
	{
		super(eventQ, eventTime, requestingElement, processingElement, requestType);
	}

}
