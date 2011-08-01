package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.Instruction;

public class ReorderBufferEntry {
	
	private Instruction instruction;
	private int operand1PhyReg;
	private int operand2PhyReg;
	private int physicalDestinationRegister;
	private boolean isIssued;
	private int FUInstance;								//which FU has been assigned
	private boolean isExecuted;
	private long readyAtTime;
	private IWEntry associatedIWEntry;
	//TODO private LSQEntry associated LSQEntry;
	
	public ReorderBufferEntry(Core core, Instruction objectsInstruction)
	{
		instruction = objectsInstruction;
		operand1PhyReg = -1;
		operand2PhyReg = -1;
		physicalDestinationRegister = -1;
		isIssued = false;
		FUInstance = -1;
		isExecuted = false;
		readyAtTime = GlobalClock.getCurrentTime();
		associatedIWEntry = null;
	}
	
	
		
	public Instruction getInstruction()
	{
		return instruction;
	}
	
	public void setInstruction(Instruction newInstruction)
	{
		instruction = newInstruction;
	}
	
	public boolean getIssued()
	{
		return isIssued;
	}
	
	public void setIssued(boolean issued)
	{
		isIssued = issued;
	}
	
	public boolean getExecuted()
	{
		return isExecuted;
	}
	
	public void setExecuted(boolean executed)
	{
		isExecuted = executed;
	}
	
	public int getPhysicalDestinationRegister()
	{
		return physicalDestinationRegister;
	}
	
	public void setPhysicalDestinationRegister(int _physicalDestinationRegister)
	{
		physicalDestinationRegister = _physicalDestinationRegister;
	}
	
	public int getFUInstance()
	{
		return FUInstance;
	}
	
	public void setFUInstance(int _FUInstance)
	{
		FUInstance = _FUInstance;
	}
	
	public long getReadyAtTime()
	{
		return readyAtTime;
	}
	
	public void setReadyAtTime(long _readyAtTime)
	{
		readyAtTime = _readyAtTime;
	}

	public int getOperand1PhyReg() {
		return operand1PhyReg;
	}


	public void setOperand1PhyReg(int operand1PhyReg) {
		this.operand1PhyReg = operand1PhyReg;
	}


	public int getOperand2PhyReg() {
		return operand2PhyReg;
	}


	public void setOperand2PhyReg(int operand2PhyReg) {
		this.operand2PhyReg = operand2PhyReg;
	}



	public IWEntry getAssociatedIWEntry() {
		return associatedIWEntry;
	}



	public void setAssociatedIWEntry(IWEntry associatedIWEntry) {
		this.associatedIWEntry = associatedIWEntry;
	}

}