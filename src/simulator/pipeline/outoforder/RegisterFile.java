package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class RegisterFile extends SimulationElement{
	
	private Core core;
	private int registerFileSize;
	private Object[] value;
	private boolean[] valueValid;					//currently used only for
	private ReorderBufferEntry[] producerROBEntry;	//machine specific registers
	long numAccesses;
	
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
		incrementNumAccesses(1);
		return valueValid[index];
	}

	public void setValueValid(boolean valueValid, int index) {
		this.valueValid[index] = valueValid;
		if(valueValid == true)
		{
			incrementNumAccesses(1);
		}
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
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}
	
	void incrementNumAccesses(int incrementBy)
	{
		numAccesses += incrementBy * core.getStepSize();
	}
	
	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double leakagePower;
		double dynamicPower;
		
		if(((OutOrderExecutionEngine)core.getExecEngine()).getIntegerRegisterFile() == this)
		{
			leakagePower = core.getIntRegFilePower().leakagePower;
			dynamicPower = core.getIntRegFilePower().dynamicPower;
		}
		else
		{
			leakagePower = core.getFpRegFilePower().leakagePower;
			dynamicPower = core.getFpRegFilePower().dynamicPower;
		}
		
		double activityFactor = (double)numAccesses
									/(double)core.getCoreCyclesTaken()
									/(core.getDecodeWidth() + core.getRetireWidth());
										//register file accessed at rename and write-back
		
		PowerConfigNew totalPower = new PowerConfigNew(leakagePower,
														dynamicPower * activityFactor);
		
		totalPower.printPowerStats(outputFileWriter, componentName);
		
		return totalPower;
	}
}