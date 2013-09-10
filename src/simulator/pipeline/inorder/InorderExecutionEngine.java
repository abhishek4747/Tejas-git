package pipeline.inorder;

import java.util.ArrayList;
import memorysystem.CoreMemorySystem;
import pipeline.ExecutionEngine;
import pipeline.outoforder.FunctionalUnitSet;
import generic.Core;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperationType;
import generic.Statistics;

public class InorderExecutionEngine extends ExecutionEngine{
	
	Core core;
	
	private int numCycles;
	private FetchUnitIn fetchUnitIn;
	private DecodeUnitIn decodeUnitIn;
	private ExecUnitIn execUnitIn;
	private MemUnitIn memUnitIn;
	private WriteBackUnitIn writeBackUnitIn;
	private boolean executionComplete;
	private boolean fetchComplete;
	public InorderCoreMemorySystem inorderCoreMemorySystem;
	private long noOfMemRequests;
	private long noOfLd;
	private long noOfSt;
	private long memStall;
	private long dataHazardStall;
	public long l2memres;
	public long oldl2req;
	public long freshl2req;
	public long icachehit;
	public long l2memoutstanding;
	public long l2hits;
	public long l2accesses;
	private int numPipelines;
	
	ArrayList<Operand> destRegisters = new ArrayList<Operand>();
	private int stallFetch;
	private FunctionalUnitSet functionalUnitSet;
	StageLatch[] ifIdLatch,idExLatch,exMemLatch,memWbLatch,wbDoneLatch;
	
	public int noOfOutstandingLoads = 0;


	public InorderExecutionEngine(Core _core, int numPipelines){
		
		super();
		
		this.core = _core;

		this.setFetchUnitIn(new FetchUnitIn(core,core.getEventQueue(),this));
		this.setDecodeUnitIn(new DecodeUnitIn(core,this));
		this.setExecUnitIn(new ExecUnitIn(core,this));
		this.setMemUnitIn(new MemUnitIn(core,this));
		this.setWriteBackUnitIn(new WriteBackUnitIn(core,this));
		this.executionComplete=false;
		functionalUnitSet = new FunctionalUnitSet(core.getAllNUnits(),core.getAllLatencies());
		memStall=0;
		dataHazardStall=0;
		
		l2memres=0;
		freshl2req=0;
		oldl2req=0;
		icachehit=0;
		l2memoutstanding=0;
		l2hits=0;
		l2accesses=0;
		ifIdLatch = new StageLatch[numPipelines];
		idExLatch = new StageLatch[numPipelines];
		exMemLatch = new StageLatch[numPipelines];
		memWbLatch = new StageLatch[numPipelines];
		wbDoneLatch = new StageLatch[numPipelines];
		for(int i=0;i<numPipelines;i++){
			ifIdLatch[i] = new StageLatch(_core);
			idExLatch[i] = new StageLatch(_core);
			exMemLatch[i] = new StageLatch(_core);
			memWbLatch[i] = new StageLatch(_core);
			wbDoneLatch[i]= new StageLatch(_core);
		}
		this.numPipelines = numPipelines;
	}

	public int getNumPipelines() {
		return numPipelines;
	}

	public void setNumPipelines(int numPipelines) {
		this.numPipelines = numPipelines;
	}

	public FunctionalUnitSet getFunctionalUnitSet() {
		return functionalUnitSet;
	}

	public void setFunctionalUnitSet(FunctionalUnitSet functionalUnitSet) {
		this.functionalUnitSet = functionalUnitSet;
	}

	public FetchUnitIn getFetchUnitIn(){
		return this.fetchUnitIn;
	}
	public DecodeUnitIn getDecodeUnitIn(){
		return this.decodeUnitIn;
	}
	public ExecUnitIn getExecUnitIn(){
		return this.execUnitIn;
	}
	public MemUnitIn getMemUnitIn(){
		return this.memUnitIn;
	}
	public WriteBackUnitIn getWriteBackUnitIn(){
		return this.writeBackUnitIn;
	}
	public void setFetchUnitIn(FetchUnitIn _fetchUnitIn){
		this.fetchUnitIn = _fetchUnitIn;
	}
	public void setDecodeUnitIn(DecodeUnitIn _decodeUnitIn){
		this.decodeUnitIn = _decodeUnitIn;
	}
	public void setExecUnitIn(ExecUnitIn _execUnitIn){
		this.execUnitIn = _execUnitIn;
	}
	public void setMemUnitIn(MemUnitIn _memUnitIn){
		this.memUnitIn = _memUnitIn;
	}
	public void setWriteBackUnitIn(WriteBackUnitIn _wbUnitIn){
		this.writeBackUnitIn = _wbUnitIn;
	}
	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
		System.out.println("Core "+core.getCore_number()+" numCycles="+this.numCycles);
		
		if (execComplete == true)
		{
			core.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize());
		}
	}
	public boolean getExecutionComplete(){
		return this.executionComplete;
	}
	public boolean getFetchComplete(){
		return this.fetchComplete;
	}
	
	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
		
		System.out.println("Mem Stalls = "+getMemStall());
		System.out.println("Data Hazard Stalls = "+getDataHazardStall());
		System.out.println("Instruction Mem Stalls = "+getInstructionMemStall());

	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(getNoOfMemRequests(), core.getCore_number());
		Statistics.setNoOfLoads(getNoOfLd(), core.getCore_number());
		Statistics.setNoOfStores(getNoOfSt(), core.getCore_number());
		Statistics.setNoOfTLBRequests(inorderCoreMemorySystem.getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(inorderCoreMemorySystem.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(inorderCoreMemorySystem.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(inorderCoreMemorySystem.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(inorderCoreMemorySystem.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(inorderCoreMemorySystem.getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(fetchUnitIn.numRequestsSent, core.getCore_number());
		Statistics.setNoOfIHits(inorderCoreMemorySystem.getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(inorderCoreMemorySystem.getiCache().misses, core.getCore_number());
		Statistics.setBranchCount(core.powerCounters.getBpredAccess(), core.getCore_number());
		Statistics.setMispredictedBranchCount(core.powerCounters.getBpredMisses(), core.getCore_number());
	}

	public void setPerCorePowerStatistics()
	{
		core.powerCounters.clearAccessStats();
		core.powerCounters.updatePowerAfterCompletion(core.getCoreCyclesTaken());
		Statistics.setPerCorePowerStatistics(core.powerCounters, core.getCore_number());
	}
	
	public long getNoOfSt() {
		return noOfSt;
	}

	public long getNoOfLd() {
		return noOfLd;
	}

	public long getNoOfMemRequests() {
		return noOfMemRequests;
	}

	public void updateNoOfLd(int i) {
		this.noOfLd += i;
	}

	public void updateNoOfMemRequests(int i) {
		this.noOfMemRequests += i;
	}

	public void updateNoOfSt(int i) {
		this.noOfSt += i;
	}

	public void incrementNumCycles(int numCycles) {
		this.numCycles += numCycles;
	}

	public int getNumCycles() {
		return numCycles;
	}

	public long getMemStall() {
		return memStall;
	}

	public long getDataHazardStall() {
		return dataHazardStall;
	}

	public void incrementDataHazardStall(int i) {
		this.dataHazardStall += i;
		
	}

	public void incrementMemStall(int i) {
		this.memStall += i;
		
	}
	
	public ArrayList<Operand> getDestRegisters() {
		return destRegisters;
	}

	public int getStallFetch() {
		return stallFetch;
	}

	public void setStallFetch(int stallFetch) {
		if(this.stallFetch > stallFetch)
			return;
		else
			this.stallFetch = stallFetch;
	}

	public void incrementStallFetch(int stallFetch) {
		this.stallFetch += stallFetch;
	}

	public void decrementStallFetch(int stallFetch) {
		this.stallFetch -= stallFetch;
	}


	public void setStallPipelinesDecode(int stallPipelines, int stall) {
		for(int i=stallPipelines+1;i<this.numPipelines;i++){
			ifIdLatch[i].setStallCount(stall);
		}
	}

	public void setStallPipelinesExecute(int stallPipelines, int stall) {
		for(int i=stallPipelines+1;i<this.numPipelines;i++){
			idExLatch[i].setStallCount(stall);
		}
		for(int i=0;i<stallPipelines;i++){
			ifIdLatch[i].setStallCount(stall);
		}
	}
	public void setStallPipelinesMem(int stallPipelines, int stall) {
		for(int i=stallPipelines+1;i<this.numPipelines;i++){
			exMemLatch[i].setStallCount(stall);
		}
		for(int i=0;i<stallPipelines;i++){
			idExLatch[i].setStallCount(stall);
		}
	}
	public StageLatch getIfIdLatch(int i){
		return this.ifIdLatch[i];
	}
	public StageLatch getIdExLatch(int i){
		return this.idExLatch[i];
	}
	public StageLatch getExMemLatch(int i){
		return this.exMemLatch[i];
	}
	public StageLatch getMemWbLatch(int i){
		return this.memWbLatch[i];
	}
	public StageLatch getWbDoneLatch(int i){
		return this.wbDoneLatch[i];
	}

	public void setMemDone(long address, boolean b) {
		for(int i=0;i<this.numPipelines;i++){
			if(exMemLatch[i].getOperationType()==OperationType.load && exMemLatch[i].getInstruction().getSourceOperand1MemValue()==address){
				exMemLatch[i].setMemDone(b);
			}
			else if(exMemLatch[i].getOperationType()==OperationType.store && exMemLatch[i].getInstruction().getSourceOperand1().getValue()==address){
			}
		}
	}

	@Override
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList) {
		
		fetchUnitIn.setInputToPipeline(inpList[0]);
		
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.inorderCoreMemorySystem = (InorderCoreMemorySystem)coreMemorySystem;
	}
	
	
	
	/*
	 * debug helper functions
	 */
	public void dumpAllLatches()
	{
		System.out.println("ifid stall = " + ifIdLatch[0].getStallCount());
		System.out.println(ifIdLatch[0].getInstruction());
		System.out.println("idex stall = " + idExLatch[0].getStallCount());
		System.out.println(idExLatch[0].getInstruction());		
		System.out.println("exMem stall = " + exMemLatch[0].getStallCount());
		System.out.println("exmem memdone = " + exMemLatch[0].getMemDone());
		System.out.println(exMemLatch[0].getInstruction());
		System.out.println("memWb stall = " + memWbLatch[0].getStallCount());
		System.out.println(memWbLatch[0].getInstruction());
	}	
}
