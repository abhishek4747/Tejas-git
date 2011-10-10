package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Event;
import generic.PortType;
import generic.SimulationElement;

public class RegisterFile extends SimulationElement{
	
	private Core core;
	private int registerFileSize;
	private Object[] value;
	private boolean[] valueValid;					//28-6-11. currently used only for
	private ReorderBufferEntry[] producerROBEntry;	//machine specific registers
	
	public RegisterFile(Core core, int _registerFileSize)
	{
		super(PortType.Unlimited, -1, -1, null, -1, -1);
		
		this.core = core;
		registerFileSize = _registerFileSize;
		value = new Object[registerFileSize];
		valueValid = new boolean[registerFileSize];
		producerROBEntry = new ReorderBufferEntry[registerFileSize];
		for(int i = 0; i < registerFileSize; i++)
		{
			valueValid[i] = true;
			producerROBEntry[i] = null;
		}
	}

	public Object getValue(int index) {
		return value[index];
	}

	public void setValue(Object value, int index) {
		this.value[index] = value;
	}

	public int getRegisterFileSize() {
		return registerFileSize;
	}

	public boolean getValueValid(int index) {
		return valueValid[index];
	}

	public void setValueValid(boolean valueValid, int index) {
		this.valueValid[index] = valueValid;
	}

	public ReorderBufferEntry getProducerROBEntry(int index) {
		return producerROBEntry[index];
	}

	public void setProducerROBEntry(ReorderBufferEntry producerROBEntry, int index) {
		this.producerROBEntry[index] = producerROBEntry;
	}

	public Core getCore() {
		return core;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
}