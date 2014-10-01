package net;

import generic.Event;

public class Bus extends InterConnect {
	 BusArbiter busArbiter;
	 
	 public Bus()
	 {
		 busArbiter = new BusArbiter();
	 }
	 
	 public void sendBusMessage(Event event)
	 {
		 //TODO : Add code for arbiter here
		 event.getProcessingElement().getPort().put(event);
	 }
}
