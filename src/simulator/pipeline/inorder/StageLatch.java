package pipeline.inorder;

import generic.Core;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class StageLatch {
	
	private Core core;
	private boolean busy;
	private Instruction instruction;
	private long PC;	//??
	private long NPC;	//??
	private Operand out1;
	private Operand in1;
	private Operand in2;
	private OperationType operationType;
	private boolean LoadFlag;
	private int stallCount;
	private boolean memDone;
	
	public StageLatch(Core _core){
		this.core = _core;
		this.busy=false;
		this.instruction = null;
		this.PC=-1;
		this.NPC=-1;
		this.out1=null;
		this.in1=null;
		this.in2=null;
		this.operationType=null;
		this.LoadFlag=false;
		this.stallCount=0;
		this.memDone=true;
	}

	public boolean getBusy(){
		return this.busy;
	}
	public Instruction getInstruction(){
		return this.instruction;
	}
	public long getPC(){
		return this.PC;
	}
	public long getNPC(){
		return this.NPC;
	}
	public Operand getOut1(){
		return this.out1;
	}
	public Operand getIn1(){
		return this.in1;
	}
	public Operand getIn2(){
		return this.in2;
	}
	public OperationType getOperationType(){
		return this.operationType;
	}
	public boolean getLoadFlag(){
		return this.LoadFlag;
	}
	public int getStallCount(){
		return this.stallCount;
	}
	public boolean getMemDone(){
		return this.memDone;
	}
	
	public void setBusy(boolean _busy){
		this.busy=_busy;
	}
	public void setInstruction(Instruction _ins){
		this.instruction=_ins;
	}
	public void setPC(long _PC){
		this.PC=_PC;
	}
	public void setNPC(long _NPC){
		this.NPC=_NPC;
	}
	public void setOut1(Operand _out1){
		this.out1=_out1;
	}
	public void setIn1(Operand _in1){
		this.in1=_in1;
	}
	public void setIn2(Operand _in2){
		this.in2=_in2;
	}
	public void setOperationType(OperationType _optype){
		this.operationType=_optype;
	}
	public void setLoadFlag(boolean flagValue){
		this.LoadFlag=flagValue;
	}
	public void incrementStallCount(){
		this.stallCount++;
	}
	public void decrementStallCount(){
		this.stallCount--;
	}
	public void setStallCount(int count){
		this.stallCount = count;
	}
	public void setMemDone(boolean val){
		this.memDone=val;
	}
	public void clear(){
		this.instruction=null;
		this.in1 = null;
		this.in2 = null;
		this.out1 = null;
		this.operationType = null;
	}
}
