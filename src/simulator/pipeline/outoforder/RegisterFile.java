package pipeline.outoforder;

import generic.SimulationElement;
import generic.Time_t;

public class RegisterFile extends SimulationElement{
	
	private int registerFileSize;
	private Object[] value;
	private boolean[] valueValid;					//28-6-11. currently used only for
	private ReorderBufferEntry[] producerROBEntry;	//machine specific registers
	
	public RegisterFile(int _registerFileSize)
	{
		super(-1, new Time_t(-1), new Time_t(-1));
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
}