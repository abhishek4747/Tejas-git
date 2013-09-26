package pipeline.multi_issue_inorder;

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

public class MultiIssueInorderExecutionEngine extends ExecutionEngine{
	
	Core core;
	
	//private int numCycles;
	int issueWidth;
	private FetchUnitIn_MII fetchUnitIn;
	private DecodeUnit_MII decodeUnitIn;
	private ExecUnitIn_MII execUnitIn;
	private MemUnitIn_MII memUnitIn;
	private WriteBackUnitIn_MII writeBackUnitIn;
	private boolean executionComplete;
	private boolean fetchComplete;
	public InorderCoreMemorySystem_MII multiIssueInorderCoreMemorySystem;
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

	
	long valueReadyInteger[];
	long valueReadyFloat[];
	long valueReadyMSR[];
	
	private int mispredStall;	//to simulate pipeline flush during branch misprediction
	private FunctionalUnitSet functionalUnitSet;
	StageLatch_MII ifIdLatch,idExLatch,exMemLatch,memWbLatch,wbDoneLatch;
	
	public int noOfOutstandingLoads = 0;


	public MultiIssueInorderExecutionEngine(Core _core, int issueWidth){
		
		super();
		
		this.core = _core;

		this.issueWidth = issueWidth;
		
		ifIdLatch = new StageLatch_MII(issueWidth);
		idExLatch = new StageLatch_MII(issueWidth);
		exMemLatch = new StageLatch_MII(issueWidth);
		memWbLatch = new StageLatch_MII(issueWidth);
		wbDoneLatch = new StageLatch_MII(issueWidth);
		
		this.setFetchUnitIn(new FetchUnitIn_MII(core,core.getEventQueue(),this));
		this.setDecodeUnitIn(new DecodeUnit_MII(core,this));
		this.setExecUnitIn(new ExecUnitIn_MII(core,this));
		this.setMemUnitIn(new MemUnitIn_MII(core,this));
		this.setWriteBackUnitIn(new WriteBackUnitIn_MII(core,this));
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
		
		valueReadyInteger = new long[core.getNIntegerArchitecturalRegisters()];
		valueReadyFloat = new long[core.getNFloatingPointArchitecturalRegisters()];
		valueReadyMSR = new long[core.getNMachineSpecificRegisters()];
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

	public FetchUnitIn_MII getFetchUnitIn(){
		return this.fetchUnitIn;
	}
	public DecodeUnit_MII getDecodeUnitIn(){
		return this.decodeUnitIn;
	}
	public ExecUnitIn_MII getExecUnitIn(){
		return this.execUnitIn;
	}
	public MemUnitIn_MII getMemUnitIn(){
		return this.memUnitIn;
	}
	public WriteBackUnitIn_MII getWriteBackUnitIn(){
		return this.writeBackUnitIn;
	}
	public void setFetchUnitIn(FetchUnitIn_MII _fetchUnitIn){
		this.fetchUnitIn = _fetchUnitIn;
	}
	public void setDecodeUnitIn(DecodeUnit_MII _decodeUnitIn){
		this.decodeUnitIn = _decodeUnitIn;
	}
	public void setExecUnitIn(ExecUnitIn_MII _execUnitIn){
		this.execUnitIn = _execUnitIn;
	}
	public void setMemUnitIn(MemUnitIn_MII _memUnitIn){
		this.memUnitIn = _memUnitIn;
	}
	public void setWriteBackUnitIn(WriteBackUnitIn_MII _wbUnitIn){
		this.writeBackUnitIn = _wbUnitIn;
	}
	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
		System.out.println("Core "+core.getCore_number()+" numCycles = " + GlobalClock.getCurrentTime());
		
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
		Statistics.setNoOfTLBRequests(multiIssueInorderCoreMemorySystem.getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(multiIssueInorderCoreMemorySystem.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(multiIssueInorderCoreMemorySystem.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(multiIssueInorderCoreMemorySystem.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(multiIssueInorderCoreMemorySystem.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(multiIssueInorderCoreMemorySystem.getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(multiIssueInorderCoreMemorySystem.getiCache().noOfRequests, core.getCore_number());
		Statistics.setNoOfIHits(multiIssueInorderCoreMemorySystem.getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(multiIssueInorderCoreMemorySystem.getiCache().misses, core.getCore_number());
		Statistics.setBranchCount(decodeUnitIn.getNumBranches(), core.getCore_number());
		Statistics.setMispredictedBranchCount(decodeUnitIn.getNumMispredictedBranches(), core.getCore_number());
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

	public long[] getValueReadyInteger() {
		return valueReadyInteger;
	}

	public long[] getValueReadyFloat() {
		return valueReadyFloat;
	}

	public long[] getValueReadyMSR() {
		return valueReadyMSR;
	}

	public int getMispredStall() {
		return mispredStall;
	}

	public void setMispredStall(int stallFetch) {
		if(this.mispredStall > stallFetch)
			return;
		else
			this.mispredStall = stallFetch;
	}

	public void decrementMispredStall(int stallFetch) {
		this.mispredStall -= stallFetch;
	}

	public int getIssueWidth() {
		return issueWidth;
	}

	public StageLatch_MII getIfIdLatch(){
		return this.ifIdLatch;
	}
	public StageLatch_MII getIdExLatch(){
		return this.idExLatch;
	}
	public StageLatch_MII getExMemLatch(){
		return this.exMemLatch;
	}
	public StageLatch_MII getMemWbLatch(){
		return this.memWbLatch;
	}
	public StageLatch_MII getWbDoneLatch(){
		return this.wbDoneLatch;
	}

	@Override
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList) {
		
		fetchUnitIn.setInputToPipeline(inpList[0]);
		
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.multiIssueInorderCoreMemorySystem = (InorderCoreMemorySystem_MII)coreMemorySystem;
	}
	
	/*
	 * debug helper functions
	 */
//	public void dumpAllLatches()
//	{
//		System.out.println("ifid stall = " + ifIdLatch[0].getStallCount());
//		System.out.println(ifIdLatch[0].getInstruction());
//		System.out.println("idex stall = " + idExLatch[0].getStallCount());
//		System.out.println(idExLatch[0].getInstruction());		
//		System.out.println("exMem stall = " + exMemLatch[0].getStallCount());
//		System.out.println("exmem memdone = " + exMemLatch[0].getMemDone());
//		System.out.println(exMemLatch[0].getInstruction());
//		System.out.println("memWb stall = " + memWbLatch[0].getStallCount());
//		System.out.println(memWbLatch[0].getInstruction());
//	}	
}
