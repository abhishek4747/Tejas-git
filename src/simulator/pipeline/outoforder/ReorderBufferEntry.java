package pipeline.outoforder;

import memorysystem.LSQEntry;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class ReorderBufferEntry {
	
	private Core core;
	private boolean isValid;
	private Instruction instruction;
	int threadID;
	private int operand1PhyReg1;
	private int operand1PhyReg2;
	private int operand2PhyReg1;
	private int operand2PhyReg2;
	boolean isOperand1Available;
	boolean isOperand2Available;
	boolean isOperand11Available;
	boolean isOperand12Available;
	boolean isOperand21Available;
	boolean isOperand22Available;
	private int physicalDestinationRegister;
	private boolean isRenameDone;
	private boolean isIssued;
	private int FUInstance;								//which FU has been assigned
	private boolean isExecuted;
	private boolean isWriteBackDone1;
	private boolean isWriteBackDone2;
	private long readyAtTime;							//in terms of GlobalClock cycles
	private IWEntry associatedIWEntry;
	private LSQEntry lsqEntry = null; //entry in LSQ
	
	int pos;
	
	public ReorderBufferEntry(Core core, int pos)
	{
		this.core = core;
		this.pos = pos;
		isValid = false;
	}

	/*
	public ReorderBufferEntry(Core core, Instruction objectsInstruction, int threadID)
	{
		this.core = core;
		instruction = objectsInstruction;
		this.threadID = threadID;
		operand1PhyReg1 = -1;
		operand1PhyReg2 = -1;
		operand2PhyReg1 = -1;
		operand2PhyReg2 = -1;
		physicalDestinationRegister = -1;
		isRenameDone = false;
		isIssued = false;
		FUInstance = -1;
		isExecuted = false;
		isWriteBackDone1 = false;
		isWriteBackDone2 = false;
		readyAtTime = GlobalClock.getCurrentTime();
		associatedIWEntry = null;

		isOperand1Available = false;
		isOperand2Available = false;
		isOperand11Available = false;
		isOperand12Available = false;
		isOperand21Available = false;
		isOperand22Available = false;
		
	}
	*/
	
	public void attemptToWriteBack()
	{
		if(isExecuted == true)
		{
			RegisterFile tempRF = null;
			RenameTable tempRN = null;
			
			Operand tempOpnd = instruction.getDestinationOperand();
			if(tempOpnd == null)
			{
				if(instruction.getOperationType() == OperationType.xchg)
				{
					writeBackForXchg();
				}
				else
				{
					isWriteBackDone1 = true;
					isWriteBackDone2 = true;
				}
			}
			
			else
			{
				OperandType tempOpndType = tempOpnd.getOperandType(); 
				if(tempOpndType == OperandType.machineSpecificRegister)
				{
					tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
					tempRN = null;
				}
				else if(tempOpndType == OperandType.integerRegister)
				{
					tempRF = core.getExecEngine().getIntegerRegisterFile();
					tempRN = core.getExecEngine().getIntegerRenameTable();
				}
				else if(tempOpndType == OperandType.floatRegister)
				{
					tempRF = core.getExecEngine().getFloatingPointRegisterFile();
					tempRN = core.getExecEngine().getFloatingPointRenameTable();
				}
				else
				{
					isWriteBackDone1 = true;
					isWriteBackDone2 = true;
					return;
				}
				
				if(instruction.getOperationType() == OperationType.mov)
				{
					writeBackForMov(tempRF, tempRN);
				}		
				else
				{
					writeBackForOthers(tempRF, tempRN);
				}
			}
			
		}
	}
		
	void writeBackForMov(RegisterFile tempRF, RenameTable tempRN)
	{
		//attempt to write-back
		WriteBackLogic.writeBack(this, 3, tempRF, tempRN, physicalDestinationRegister, core);	
	}
	
	void writeBackForXchg()
	{
		RegisterFile tempRF = null;
		RenameTable tempRN = null;
		int phyReg;
		
		//operand 1
		phyReg = operand1PhyReg1;
		if(instruction.getSourceOperand1().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
		}
		else if(instruction.getSourceOperand1().getOperandType() == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
		}
		else if(instruction.getSourceOperand1().getOperandType() == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
		}
		
		//attempt to write-back
		WriteBackLogic.writeBack(this, 1, tempRF, tempRN, phyReg, core);
		
		
		
		tempRF = null;
		tempRN = null;
		//operand 2
		phyReg = operand2PhyReg1;
		if(instruction.getSourceOperand2().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(threadID);
		}
		else if(instruction.getSourceOperand2().getOperandType() == OperandType.integerRegister)
		{
			tempRF = core.getExecEngine().getIntegerRegisterFile();
			tempRN = core.getExecEngine().getIntegerRenameTable();
		}
		else if(instruction.getSourceOperand2().getOperandType() == OperandType.floatRegister)
		{
			tempRF = core.getExecEngine().getFloatingPointRegisterFile();
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
		}
		
		//attempt to write-back
		WriteBackLogic.writeBack(this, 2, tempRF, tempRN, phyReg, core);
		
	}
	
	void writeBackForOthers(RegisterFile tempRF, RenameTable tempRN)
	{
		//attempt to write-back
		WriteBackLogic.writeBack(this, 3, tempRF, tempRN, physicalDestinationRegister, core);
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
	
	protected LSQEntry getLsqEntry() {
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
	public boolean isOperand1Available() {
		return isOperand1Available;
	}
	
	public void setOperand1Available(boolean isOperand1Available) {
		this.isOperand1Available = isOperand1Available;
	}
	
	public boolean isOperand2Available() {
		return isOperand2Available;
	}
	
	public void setOperand2Available(boolean isOperand2Available) {
		this.isOperand2Available = isOperand2Available;
	}
	
	public boolean isOperand11Available() {
		return isOperand11Available;
	}

	public void setOperand11Available(boolean isOperand11Available) {
		this.isOperand11Available = isOperand11Available;
	}

	public boolean isOperand12Available() {
		return isOperand12Available;
	}

	public void setOperand12Available(boolean isOperand12Available) {
		this.isOperand12Available = isOperand12Available;
	}

	public boolean isOperand21Available() {
		return isOperand21Available;
	}

	public void setOperand21Available(boolean isOperand21Available) {
		this.isOperand21Available = isOperand21Available;
	}

	public boolean isOperand22Available() {
		return isOperand22Available;
	}

	public void setOperand22Available(boolean isOperand22Available) {
		this.isOperand22Available = isOperand22Available;
	}
	
	public int getThreadID() {
		return threadID;
	}

	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}

	public boolean isRenameDone() {
		return isRenameDone;
	}

	public void setRenameDone(boolean isRenameDone) {
		this.isRenameDone = isRenameDone;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(instruction.toString());
		sb.append("thread ID = " + threadID + "\n");
		sb.append("phy regs = " + operand1PhyReg1 +  " " + operand1PhyReg2 + " ");
		sb.append(operand2PhyReg1 + " " + operand2PhyReg2 + " ");
		sb.append(physicalDestinationRegister + "\n");
		sb.append("op available = " + isOperand1Available + " " + isOperand2Available);
		sb.append(" " + isOperand11Available + " " + isOperand12Available + " ");
		sb.append(isOperand21Available + " " + isOperand22Available + "\n");
		sb.append("rename = " + isRenameDone);
		sb.append(" issued = " + isIssued + "\n");
		sb.append("fu = " + FUInstance + "\n");
		sb.append("exec = " + isExecuted + " ");
		sb.append("wb1 = " + isWriteBackDone1 + " ");
		sb.append("wb2 = " + isWriteBackDone2 + "\n");
		sb.append("ready at time = " + readyAtTime + "\n\n\n");
		return sb.toString();
	}

}