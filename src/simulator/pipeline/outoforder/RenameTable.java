package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;

import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class RenameTable extends SimulationElement{
	
	OutOrderExecutionEngine execEngine;
	int nArchRegisters;
	int nPhyRegisters;
	int noOfThreads;
	
	int[] archReg;
	int[] threadID;							//thread that is going to write to the register
	boolean[] mappingValid;
	boolean[] valueValid;
	private ReorderBufferEntry[] producerROBEntry;
	
	int[][] archToPhyMapping;
	
	RegisterFile associatedRegisterFile;
	
	int availableList[];
	int availableListHead;
	int availableListTail;
	
	long numRATAccesses;
	long numFreeListAccesses;
	
	public RenameTable(OutOrderExecutionEngine execEngine,
						int nArchRegisters, int nPhyRegisters, RegisterFile associatedRegisterFile,
						int noOfThreads)
	{
		super(PortType.Unlimited, -1, -1, null, -1, -1);
		this.execEngine = execEngine;
		this.nArchRegisters = nArchRegisters;
		this.nPhyRegisters = nPhyRegisters;
		this.noOfThreads = noOfThreads;
		this.associatedRegisterFile = associatedRegisterFile;
		
		archReg = new int[this.nPhyRegisters];
		threadID = new int[this.nPhyRegisters];
		mappingValid = new boolean[this.nPhyRegisters];
		valueValid = new boolean[this.nPhyRegisters];
		producerROBEntry = new ReorderBufferEntry[this.nPhyRegisters];
		
		if(noOfThreads * this.nArchRegisters > this.nPhyRegisters)
		{
			System.out.println("too many threads, not enough registers!");
			System.exit(1);
		}
		
		int temp;
		for(int j = 0; j < noOfThreads; j++)
		{
			temp = j * this.nArchRegisters;
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				archReg[temp + i] = i;
				threadID[temp + i] = j;
				mappingValid[temp + i] = true;
				valueValid[temp + i] = true;
				producerROBEntry[temp + i] = null;
			}
		}
		
		for(int i = this.nArchRegisters * this.noOfThreads; i < this.nPhyRegisters; i++)
		{
			archReg[i] = -1;
			threadID[i] = -1;
			mappingValid[i] = false;
			valueValid[i] = false;
			producerROBEntry[i] = null;
		}
		
		availableList = new int[this.nPhyRegisters - this.nArchRegisters * this.noOfThreads];
		int ctr = 0;
		for(int i = this.nArchRegisters * this.noOfThreads; i < this.nPhyRegisters; i++)
		{
			availableList[ctr++] = i;
		}
		availableListHead = 0;
		availableListTail = this.nPhyRegisters - this.nArchRegisters * this.noOfThreads - 1;
		
		archToPhyMapping = new int[this.noOfThreads][this.nArchRegisters];
		for(int j = 0; j < this.noOfThreads; j++)
		{
			temp = j * this.nArchRegisters;
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				archToPhyMapping[j][i] = temp + i;
			}
		}
		
	}
	
	public int allocatePhysicalRegister(int threadID, int archReg)
	{
		if(availableListHead == -1)
		{
			//no free physical registers
			return -1;
		}
		
		int newPhyReg = removeFromAvailableList();
		int oldPhyReg = this.archToPhyMapping[threadID][archReg];
		this.archReg[newPhyReg] = archReg;
		this.threadID[newPhyReg] = threadID;
		this.valueValid[newPhyReg] = false;
		this.associatedRegisterFile.setValueValid(false, newPhyReg);		
		this.archToPhyMapping[threadID][archReg] = newPhyReg;
		
		if(this.associatedRegisterFile.getValueValid(oldPhyReg) == true)
		{
			addToAvailableList(oldPhyReg);
		}
		else
		{
			this.mappingValid[oldPhyReg] = false;
		}
		
		this.mappingValid[newPhyReg] = true;
		
		incrementRatAccesses(1);
		
		
		return newPhyReg;
	}

	public int getArchReg(int index) {
		return archReg[index];
	}
	
	public int getThreadID(int index)
	{
		return threadID[index];
	}

	public void setArchReg(int threadID, int archReg, int index) {
		this.archReg[index] = archReg;
		this.archToPhyMapping[threadID][archReg] = index;
	}

	public boolean getMappingValid(int index) {
		return mappingValid[index];
	}

	public void setMappingValid(boolean mappingValid, int index) {
		this.mappingValid[index] = mappingValid;
	}

	public boolean getValueValid(int index) {
		return valueValid[index];
	}

	public void setValueValid(boolean valueValid, int index) {
		this.valueValid[index] = valueValid;
	}
	
	public int getPhysicalRegister(int threadID, int archReg)
	{
		incrementRatAccesses(1);
		return archToPhyMapping[threadID][archReg];
	}

	public ReorderBufferEntry getProducerROBEntry(int index) {
		return producerROBEntry[index];
	}

	public void setProducerROBEntry(ReorderBufferEntry producerROBEntry, int index) {
		this.producerROBEntry[index] = producerROBEntry;
	}	

	public RegisterFile getAssociatedRegisterFile() {
		return associatedRegisterFile;
	}

	public void setAssociatedRegisterFile(RegisterFile associatedRegisterFile) {
		this.associatedRegisterFile = associatedRegisterFile;
	}
	
	public void addToAvailableList(int phyRegNum)
	{
		if(getAvailableListSize() >= this.nPhyRegisters - this.nArchRegisters * this.noOfThreads)
		{
			System.out.println("available register list overflow!!");
			System.exit(1);
		}
		
		availableListTail = (availableListTail + 1)%(this.nPhyRegisters - this.nArchRegisters * this.noOfThreads);
		availableList[availableListTail] = phyRegNum;
		if(availableListHead == -1)
		{
			availableListHead = 0;
		}
		
		incrementFreeListAccesses(1);
	}
	
	public int removeFromAvailableList()
	{
		//NOTE - list empty check to be done before this function is called
		int toBeReturned = availableList[availableListHead];
		
		if(availableListHead == availableListTail)
		{
			availableListHead = availableListTail = -1;
		}
		else
		{
			availableListHead = (availableListHead + 1)%(this.nPhyRegisters - this.nArchRegisters * this.noOfThreads);
		}
		
		incrementFreeListAccesses(1);
		
		return toBeReturned;
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}
	
	public int getAvailableListSize()
	{
		if(availableListHead == -1)
		{
			return 0;
		}
		
		if(availableListTail >= availableListHead)
		{
			return (availableListTail - availableListHead + 1);
		}
		
		return (this.nPhyRegisters - this.nArchRegisters * this.noOfThreads - availableListHead + availableListTail + 1);
	}
	
	public void incrementRatAccesses(int incrementBy)
	{
		numRATAccesses += incrementBy * execEngine.getContainingCore().getStepSize();
	}
	
	public void incrementFreeListAccesses(int incrementBy)
	{
		numFreeListAccesses += incrementBy * execEngine.getContainingCore().getStepSize();
	}
	
	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double RATleakagePower;
		double RATdynamicPower;
		double freeListleakagePower;
		double freeListdynamicPower;
		
		if(execEngine.getIntegerRenameTable() == this)
		{
			RATleakagePower = execEngine.getContainingCore().getIntRATPower().leakagePower;
			RATdynamicPower = execEngine.getContainingCore().getIntRATPower().dynamicPower;
			freeListleakagePower = execEngine.getContainingCore().getIntFreeListPower().leakagePower;
			freeListdynamicPower = execEngine.getContainingCore().getIntFreeListPower().dynamicPower;
		}
		else
		{
			RATleakagePower = execEngine.getContainingCore().getFpRATPower().leakagePower;
			RATdynamicPower = execEngine.getContainingCore().getFpRATPower().dynamicPower;
			freeListleakagePower = execEngine.getContainingCore().getFpFreeListPower().leakagePower;
			freeListdynamicPower = execEngine.getContainingCore().getFpFreeListPower().dynamicPower;
		}
		
		double RATactivityFactor = (double)numRATAccesses
									/(double)execEngine.getContainingCore().getCoreCyclesTaken()
									/(3*execEngine.getContainingCore().getDecodeWidth());
											//potentially decodeWidth number of instructions can
											// be renamed per cycle (3*decodeWidth RAT accesses)
		double freeListActivityFactor = (double)numFreeListAccesses
									/(double)execEngine.getContainingCore().getCoreCyclesTaken()
									/(2*execEngine.getContainingCore().getDecodeWidth());
											//potentially decodeWidth number of instructions can
											// be renamed/ per cycle (2*decodeWidth free-list accesses : add/remove to/from free-list)
		
		PowerConfigNew RATPower = new PowerConfigNew(RATleakagePower,
														RATdynamicPower * RATactivityFactor);
		PowerConfigNew freeListPower = new PowerConfigNew(freeListleakagePower,
														freeListdynamicPower * freeListActivityFactor);
		PowerConfigNew totalPower = new PowerConfigNew(RATleakagePower + freeListleakagePower,
														RATdynamicPower * RATactivityFactor + freeListdynamicPower * freeListActivityFactor);
		
		outputFileWriter.write("\n" + componentName + " :\n" + totalPower + "\n");
		outputFileWriter.write("RAT :\n" + RATPower + "\n");
		outputFileWriter.write("Free List :\n" + freeListPower + "\n");
		
		return totalPower;
	}

}