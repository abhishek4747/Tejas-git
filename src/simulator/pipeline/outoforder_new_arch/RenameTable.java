package pipeline.outoforder_new_arch;

import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

import java.util.LinkedList;

public class RenameTable extends SimulationElement{
	
	int nArchRegisters;
	int nPhyRegisters;
	int noOfThreads;
	
	int[] archReg;
	int[] threadID;							//thread that is going to write to the register
	boolean[] mappingValid;
	boolean[] valueValid;
	private ReorderBufferEntry[] producerROBEntry;
	
	//LinkedList <Integer>availableList;
	
	int[][] archToPhyMapping;
	
	RegisterFile associatedRegisterFile;
	RenameTableCheckpoint checkpoint;
	
	int availableList[];
	int availableListHead;
	int availableListTail;
	
	public RenameTable(int nArchRegisters, int nPhyRegisters, RegisterFile associatedRegisterFile,
						int noOfThreads)
	{
		super(PortType.Unlimited, -1, -1, null, -1, -1);
		this.nArchRegisters = nArchRegisters;
		this.nPhyRegisters = nPhyRegisters;
		this.noOfThreads = noOfThreads;
		this.associatedRegisterFile = associatedRegisterFile;
		
		archReg = new int[this.nPhyRegisters];
		threadID = new int[this.nPhyRegisters];
		mappingValid = new boolean[this.nPhyRegisters];
		valueValid = new boolean[this.nPhyRegisters];
		producerROBEntry = new ReorderBufferEntry[this.nPhyRegisters];
		checkpoint = new RenameTableCheckpoint(this.noOfThreads, this.nArchRegisters);
		
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
				//associatedRegisterFile.setValueValid(true, temp + i);
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
		
		/*availableList = new LinkedList<Integer>();
		for(int i = this.nArchRegisters * this.noOfThreads; i < this.nPhyRegisters; i++)
		{
			availableList.addLast(i);
		}
		*/
		
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
		/*
		for(int i = 0; i < this.nPhyRegisters; i++)
		{
			System.out.println(archReg[i] + " " + threadID[i] + " " + valueValid[i] + " " + producerROBEntry[i]);
		}
		
		System.out.println("\n\n");
		
		for(int j = 0; j < this.noOfThreads; j++)
		{
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				System.out.println("thread : " + j + "\t\tarch reg : " + i + "\t\t=\t" + archToPhyMapping[j][i] + "\n");
			}
		}
		*/
	}
	
	public int allocatePhysicalRegister(int threadID, int archReg)
	{
		if(availableListHead == -1)
		{
			//no free physical registers
			return -1;
		}
		/*
		int curPhyReg = this.archToPhyMapping[threadID][archReg];
		if(valueValid[curPhyReg] == true)
		{
			addToAvailableList(curPhyReg);
		}
		*/
		int newPhyReg = removeFromAvailableList();
		int oldPhyReg = this.archToPhyMapping[threadID][archReg];
		this.archReg[newPhyReg] = archReg;
		this.threadID[newPhyReg] = threadID;
		this.valueValid[newPhyReg] = false;
		this.associatedRegisterFile.setValueValid(false, newPhyReg);		
		this.archToPhyMapping[threadID][archReg] = newPhyReg;
		/*if(this.valueValid[this.archToPhyMapping[threadID][archReg]] == true
				&& this.mappingValid[this.archToPhyMapping[threadID][archReg]] == true)*/
		/*if(this.associatedRegisterFile.getValueValid(oldPhyReg) == true)*/
		if(this.associatedRegisterFile.getNoOfActiveWriters(oldPhyReg) == 0)
		{
			if(this.mappingValid[oldPhyReg] == false)
			{
				System.out.println("attempting to add a register, whose mapping is false, to the available list!!");
			}
			addToAvailableList(oldPhyReg);
		}
		if(this.associatedRegisterFile.getNoOfActiveWriters(oldPhyReg) < 0)
		{
			System.out.println("number of active writers < 0!!");
		}

		this.mappingValid[newPhyReg] = true;
		this.mappingValid[oldPhyReg] = false;
		
		this.associatedRegisterFile.incrementNoOfActiveWriters(newPhyReg);
		
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

	public RenameTableCheckpoint getCheckpoint() {
		return checkpoint;
	}

	public void setCheckpoint(RenameTableCheckpoint checkpoint) {
		this.checkpoint = checkpoint;
	}
	
	/*public void addToAvailableList(int phyRegNum)
	{
		this.availableList.addLast(phyRegNum);
		//setValueValid(false, phyRegNum);
	}
	*/
	
	public void addToAvailableList(int phyRegNum)
	{
		/*for(int i = availableListHead; ; i = (i+1)%(this.nPhyRegisters - this.nArchRegisters * this.noOfThreads))
		{
			if(i == -1)
				break;
			
			if(availableList[i] == phyRegNum)
			{
				System.out.println("adding already existing register to available list!!");
			}
			
			if(i == availableListTail)
				break;
		}
		for(int i = 0; i < noOfThreads; i++)
		{
			for(int j = 0; j < nArchRegisters; j++)
			{
				if(archToPhyMapping[i][j] == phyRegNum)
				{
					System.out.println("adding register, that is currently mapped, to available list!!");
				}
			}
		}*/
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
		
		return toBeReturned;
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
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

}