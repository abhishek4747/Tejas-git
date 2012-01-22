package memorysystem;

import java.util.Vector;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class AddressCarryingEvent extends Event
{
	private long address;
	private Vector<Integer> sourceBankId;
	private Vector<Integer> destinationBankId;
	private int currentLevel;
	private boolean flag;
	public AddressCarryingEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		super(eventQ, eventTime, requestingElement, processingElement,
				requestType);
		this.address = address;
		setSourceBankId(null);
		setDestinationBankId(null);
		setCurrentLevel(0);
		setFlag(false);
	}

	public void updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address) {
		this.address = address;
		this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, 
			Vector<Integer> sourceBankId,
			Vector<Integer> destinationBankId) {
		this.sourceBankId = sourceBankId;
		this.destinationBankId = destinationBankId;
		return (AddressCarryingEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public AddressCarryingEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, 
			Vector<Integer> sourceBankId,
			Vector<Integer> destinationBankId,
			int currentLevel) {
		this.sourceBankId = sourceBankId;
		this.destinationBankId = destinationBankId;
		this.setCurrentLevel(currentLevel);
		return (AddressCarryingEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}

	public void setSourceBankId(Vector<Integer> sourceBankId) {
		this.sourceBankId = sourceBankId;
	}

	public Vector<Integer> getSourceBankId() {
		return sourceBankId;
	}

	public void setDestinationBankId(Vector<Integer> destinationBankId) {
		this.destinationBankId = destinationBankId;
	}

	public Vector<Integer> getDestinationBankId() {
		return destinationBankId;
	}

	public void setCurrentLevel(int currentLevel) {
		this.currentLevel = currentLevel;
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public boolean isFlag() {
		return flag;
	}
}
