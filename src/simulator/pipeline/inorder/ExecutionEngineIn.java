package pipeline.inorder;

import config.SystemConfig;
import memorysystem.CoreMemorySystem;
import generic.Core;
import generic.GlobalClock;
import generic.Statistics;

public class ExecutionEngineIn {
	
	Core core;
	StageLatch ifId,idEx,exMem,memWb,wbDone;
	
	private int numCycles;
	private FetchUnitIn fetchUnitIn;
	private DecodeUnitIn decodeUnitIn;
	private RegFileIn regFileIn;
	private ExecUnitIn execUnitIn;
	private MemUnitIn memUnitIn;
	private WriteBackUnitIn writeBackUnitIn;
	private boolean executionComplete;
	private boolean fetchComplete;
	public CoreMemorySystem coreMemorySystem;
	private int noOfMemRequests;
	private int noOfLd;
	private int noOfSt;

	public ExecutionEngineIn(Core _core){
		this.core = _core;
		this.ifId = new StageLatch(_core);
		this.idEx = new StageLatch(_core);
		this.exMem = new StageLatch(_core);
		this.memWb = new StageLatch(_core);
		this.wbDone = new StageLatch(_core);
		this.setFetchUnitIn(new FetchUnitIn(core,core.getEventQueue()));
		this.setDecodeUnitIn(new DecodeUnitIn(core));
		this.setRegFileIn(new RegFileIn(core));
		this.setExecUnitIn(new ExecUnitIn(core));
		this.setMemUnitIn(new MemUnitIn(core));
		this.setWriteBackUnitIn(new WriteBackUnitIn(core));
		this.executionComplete=false;

	}

	public FetchUnitIn getFetchUnitIn(){
		return this.fetchUnitIn;
	}
	public DecodeUnitIn getDecodeUnitIn(){
		return this.decodeUnitIn;
	}
	public RegFileIn getRegFileIn(){
		return this.regFileIn;
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
	public void setRegFileIn(RegFileIn _regFileIn){
		this.regFileIn = _regFileIn;
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
//	public void setCoreMemorySystem(CoreMemorySystem coreMemSys){
//		this.coreMemorySystem=coreMemSys;
//	}
	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
		System.out.println("Core "+core.getCore_number()+" numCycles="+this.numCycles);
	}
	public void setFetchComplete(boolean fetchComplete){
		this.fetchComplete=fetchComplete;
	}
	public StageLatch getIfIdLatch(){
		return this.ifId;
	}
	public StageLatch getIdExLatch(){
		return this.idEx;
	}
	public StageLatch getExMemLatch(){
		return this.exMem;
	}
	public StageLatch getMemWbLatch(){
		return this.memWb;
	}
	public StageLatch getWbDoneLatch(){
		return this.wbDone;
	}
//	public CoreMemorySystem getCoreMemorySystem(){
//		return this.coreMemorySystem;
//	}
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
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(core.getExecutionEngineIn().getNoOfMemRequests(), core.getCore_number());
		Statistics.setNoOfLoads(core.getExecutionEngineIn().getNoOfLd(), core.getCore_number());
		Statistics.setNoOfStores(core.getExecutionEngineIn().getNoOfSt(), core.getCore_number());
//		Statistics.setNoOfMemRequests(core.getExecutionEngineIn().coreMemorySystem.getLsqueue().noOfMemRequests, core.getCore_number());
//		Statistics.setNoOfLoads(core.getExecutionEngineIn().coreMemorySystem.getLsqueue().NoOfLd, core.getCore_number());
//		Statistics.setNoOfStores(core.getExecutionEngineIn().coreMemorySystem.getLsqueue().NoOfSt, core.getCore_number());
//		Statistics.setNoOfValueForwards(core.getExecutionEngineIn().coreMemorySystem.getLsqueue().NoOfForwards, core.getCore_number());
//		Statistics.setNoOfTLBRequests(core.getExecutionEngineIn().coreMemorySystem.getTLBuffer().getTlbRequests(), core.getCore_number());
//		Statistics.setNoOfTLBHits(core.getExecutionEngineIn().coreMemorySystem.getTLBuffer().getTlbHits(), core.getCore_number());
//		Statistics.setNoOfTLBMisses(core.getExecutionEngineIn().coreMemorySystem.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(core.getExecutionEngineIn().coreMemorySystem.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(core.getExecutionEngineIn().coreMemorySystem.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(core.getExecutionEngineIn().coreMemorySystem.getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(core.getExecutionEngineIn().coreMemorySystem.getiCache().noOfRequests, core.getCore_number());
		Statistics.setNoOfIHits(core.getExecutionEngineIn().coreMemorySystem.getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(core.getExecutionEngineIn().coreMemorySystem.getiCache().misses, core.getCore_number());
	}

	private long getNoOfSt() {
		// TODO Auto-generated method stub
		return noOfSt;
	}

	private long getNoOfLd() {
		// TODO Auto-generated method stub
		return noOfLd;
	}

	private long getNoOfMemRequests() {
		// TODO Auto-generated method stub
		return noOfMemRequests;
	}

	public void updateNoOfLd(int i) {
		// TODO Auto-generated method stub
		this.noOfLd += i;
	}

	public void updateNoOfMemRequests(int i) {
		// TODO Auto-generated method stub
		this.noOfMemRequests += i;
	}

	public void updateNoOfSt(int i) {
		// TODO Auto-generated method stub
		this.noOfSt += i;
	}

	public void incrementNumCycles(int numCycles) {
		this.numCycles += numCycles;
	}

	public int getNumCycles() {
		return numCycles;
	}
}
