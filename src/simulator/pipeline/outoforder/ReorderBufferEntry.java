package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.Instruction;

public class ReorderBufferEntry {
	
	private Instruction instruction;
	private int operand1PhyReg1;
	private int operand1PhyReg2;
	private int operand2PhyReg1;
	private int operand2PhyReg2;
	private int physicalDestinationRegister;
	private boolean isIssued;
	private int FUInstance;								//which FU has been assigned
	private boolean isExecuted;
	private long readyAtTime;
	private IWEntry associatedIWEntry;
	private int lsqIndex; //Index of the entry in LSQ
	
	protected int getLsqIndex() {
		return lsqIndex;
	}



	protected void setLsqIndex(int lsqIndex) {
		this.lsqIndex = lsqIndex;
	}



	public ReorderBufferEntry(Core core, Instruction objectsInstruction)
	{
		instruction = objectsInstruction;
		operand1PhyReg1 = -1;
		operand1PhyReg2 = -1;
		operand2PhyReg1 = -1;
		operand2PhyReg2 = -1;
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
	
	public int getOperand1PhyReg1() {
		return operand1PhyReg1;
	}

	public void setOperand1PhyReg1(int operand1PhyReg1) {
		this.operand1PhyReg1 = operand1PhyReg1;
	}

	public int getOperand1PhyReg2() {
		return operand1PhyReg2;
	}

	public void setOperand1PhyReg2(int operand1PhyReg2) {
		this.operand1PhyReg2 = operand1PhyReg2;
	}

	public int getOperand2PhyReg1() {
		return operand2PhyReg1;
	}

	public void setOperand2PhyReg1(int operand2PhyReg1) {
		this.operand2PhyReg1 = operand2PhyReg1;
	}

	public int getOperand2PhyReg2() {
		return operand2PhyReg2;
	}

	public void setOperand2PhyReg2(int operand2PhyReg2) {
		this.operand2PhyReg2 = operand2PhyReg2;
	}

	public IWEntry getAssociatedIWEntry() {
		return associatedIWEntry;
	}

	public void setAssociatedIWEntry(IWEntry associatedIWEntry) {
		this.associatedIWEntry = associatedIWEntry;
	}

}