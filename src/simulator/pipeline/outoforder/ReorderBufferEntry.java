package pipeline.outoforder;

import memorysystem.LSQEntry;
import memorysystem.LSQ;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;

public class ReorderBufferEntry {
	
	private Core core;
	private Instruction instruction;
	private int operand1PhyReg1;
	private int operand1PhyReg2;
	private int operand2PhyReg1;
	private int operand2PhyReg2;
	private int physicalDestinationRegister;
	private boolean isIssued;
	private int FUInstance;								//which FU has been assigned
	private boolean isExecuted;
	private boolean isWriteBackDone1;
	private boolean isWriteBackDone2;
	private long readyAtTime;							//in terms of GlobalClock cycles
	private IWEntry associatedIWEntry;
	private LSQEntry lsqEntry = null; //entry in LSQ

	public ReorderBufferEntry(Core core, Instruction objectsInstruction)
	{
		this.core = core;
		instruction = objectsInstruction;
		operand1PhyReg1 = -1;
		operand1PhyReg2 = -1;
		operand2PhyReg1 = -1;
		operand2PhyReg2 = -1;
		physicalDestinationRegister = -1;
		isIssued = false;
		FUInstance = -1;
		isExecuted = false;
		isWriteBackDone1 = false;
		isWriteBackDone2 = false;
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
		if(readyAtTime <= GlobalClock.getCurrentTime())
		{
			return GlobalClock.getCurrentTime() + 1; //TODO not 1 - step_size
		}
		else
		{
			return readyAtTime;
		}
	}
	
	public void setReadyAtTime(long _readyAtTime)
	{
		readyAtTime = _readyAtTime + core.getRegFileOccupancy()*core.getStepSize();
		//at places where the readyAtTime is calculated, the focus is on completion of operation
		//hence, the register file latency is accounted for here
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
	
	public LSQEntry getLsqEntry() {
		return lsqEntry;
	}

	protected void setLsqEntry(LSQEntry lsqEntry) {
		this.lsqEntry = lsqEntry;
	}

	public boolean isWriteBackDone1() {
		return isWriteBackDone1;
	}

	public void setWriteBackDone1(boolean isWriteBackDone1) {
		this.isWriteBackDone1 = isWriteBackDone1;
	}

	public boolean isWriteBackDone2() {
		return isWriteBackDone2;
	}

	public void setWriteBackDone2(boolean isWriteBackDone2) {
		this.isWriteBackDone2 = isWriteBackDone2;
	}
	
	public boolean isWriteBackDone()
	{
		return (isWriteBackDone1 && isWriteBackDone2);
	}

}