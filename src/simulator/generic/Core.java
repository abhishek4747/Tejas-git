package generic;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCacheBank;
import net.NocInterface;
import net.Router;
import net.NOC.CONNECTIONTYPE;
import pipeline.ExecutionEngine;
import pipeline.multi_issue_inorder.MultiIssueInorderExecutionEngine;
import pipeline.multi_issue_inorder.MultiIssueInorderPipeline;
import pipeline.outoforder.OutOrderExecutionEngine;
import pipeline.outoforder.OutOfOrderPipeline;
import config.CoreConfig;
import config.PipelineType;
import config.PowerConfigNew;
import config.SystemConfig;
import config.CacheConfig;

/**
 * represents a single core
 * has it's own clock, and comprises of an execution engine and an event queue
 * all core parameters are defined here
 */

public class Core extends SimulationElement implements NocInterface{
	
	//long clock;
	Router router;
	public static NucaCache nucaCache;
	Vector<Integer> nocElementId;
	Port port;
	int stepSize;
	long frequency;
	ExecutionEngine execEngine;
	public EventQueue eventQueue;
	public int currentThreads;
	
	public boolean isPipelineInOrder() {
		return (SystemConfig.core[this.core_number].pipelineType==PipelineType.inOrder);
	}
	
	public boolean isPipelineOutOfOrder() {
		return (SystemConfig.core[this.core_number].pipelineType==PipelineType.outOfOrder);
	}
	

	//core parameters
	private int decodeWidth;
	private int issueWidth;
	private int retireWidth;
	private int reorderBufferSize;
	private int IWSize;
	private int integerRegisterFileSize;
	private int floatingPointRegisterFileSize;
	private int nIntegerArchitecturalRegisters;
	private int nFloatingPointArchitecturalRegisters;
	private int nMachineSpecificRegisters;
	private int noOfRegFilePorts;
	private int regFileOccupancy;
	private int branchMispredictionPenalty;
	private int[] nUnits;
	private int[] latencies;
	
	//core power parameters
	private PowerConfigNew bPredPower;
	private PowerConfigNew decodePower;
	private PowerConfigNew intRATPower;
	private PowerConfigNew floatRATPower;
	private PowerConfigNew intFreeListPower;
	private PowerConfigNew floatFreeListPower;
	private PowerConfigNew lsqPower;
	private PowerConfigNew intRegFilePower;
	private PowerConfigNew floatRegFilePower;
	private PowerConfigNew iwPower;
	private PowerConfigNew robPower;
	private PowerConfigNew intALUPower;
	private PowerConfigNew floatALUPower;
	private PowerConfigNew complexALUPower;
	private PowerConfigNew resultsBroadcastBusPower;
	private PowerConfigNew iTLBPower;
	private PowerConfigNew dTLBPower;
	
	private int core_number;
	private int no_of_input_pipes;
	private int no_of_threads;
	private long coreCyclesTaken;
	
	private int[] threadIDs;
	
	private long noOfInstructionsExecuted;
	
	private pipeline.PipelineInterface pipelineInterface;
	public int numReturns;
	private int numInorderPipelines;
	public CoreBcastBus coreBcastBus;
	public int barrier_latency;
	public boolean TreeBarrier;
	public int barrierUnit; //0=>central 1=>distributed

//	private InorderPipeline inorderPipeline;

	
	public Core(int core_number,
			int no_of_input_pipes,
			int no_of_threads,
			InstructionLinkedList[] incomingInstructionLists,
			int[] threadIDs)
	{
		super(PortType.Unlimited, -1, -1, -1, SystemConfig.core[core_number].frequency);	
		//TODO frequency from config file
		this.router = new Router(SystemConfig.nocConfig, this);
		this.port = new Port(PortType.Unlimited, -1, -1);
		this.eventQueue = new EventQueue();
		this.frequency = SystemConfig.core[core_number].frequency;
		initializeCoreParameters(SystemConfig.core[core_number]);
		
		this.core_number = core_number;
		this.no_of_input_pipes = no_of_input_pipes;
		this.no_of_threads = no_of_threads;
		this.threadIDs = threadIDs;
		this.currentThreads =0;
		if(this.isPipelineInOrder()) {
			this.execEngine = new MultiIssueInorderExecutionEngine(this, issueWidth);
		} else if (isPipelineOutOfOrder()){
			this.execEngine = new OutOrderExecutionEngine(this);
		} else {
			misc.Error.showErrorAndExit("pipeline type not identified : " + 
				SystemConfig.core[core_number].pipelineType);
		}
		
		
		this.noOfInstructionsExecuted = 0;
		this.numReturns=0;
		
		if(isPipelineInOrder()) {
			this.pipelineInterface = new MultiIssueInorderPipeline(this, eventQueue);
		} else if (isPipelineOutOfOrder()) {
			this.pipelineInterface = new OutOfOrderPipeline(this, eventQueue);
		} else {
			misc.Error.showErrorAndExit("pipeline type not identified : " + 
				SystemConfig.core[core_number].pipelineType);
		}
		
		setPowerConfigs();
	}
	
	private void setPowerConfigs()
	{
		CoreConfig coreConfig = SystemConfig.core[getCore_number()];
		bPredPower = coreConfig.bPredPower;
		decodePower = coreConfig.decodePower;
		intRATPower = coreConfig.intRATPower;
		floatRATPower = coreConfig.floatRATPower;
		intFreeListPower = coreConfig.intFreeListPower;
		floatFreeListPower = coreConfig.floatFreeListPower;
		lsqPower = coreConfig.lsqPower;
		intRegFilePower = coreConfig.intRegFilePower;
		floatRegFilePower = coreConfig.floatRegFilePower;
		iwPower = coreConfig.iwPower;
		robPower = coreConfig.robPower;
		intALUPower = coreConfig.intALUPower;
		floatALUPower = coreConfig.floatALUPower;
		complexALUPower = coreConfig.complexALUPower;
		resultsBroadcastBusPower = coreConfig.resultsBroadcastBusPower;
		iTLBPower = coreConfig.iTLBPower;
		dTLBPower = coreConfig.dTLBPower;
	}
	
	public void setCoreBcastBus(CoreBcastBus coreBcastBus){
		this.coreBcastBus = coreBcastBus;
	}
	private void initializeCoreParameters(CoreConfig coreConfig)
	{
		//TODO parameters to be set according to contents of an XML configuration file
		setDecodeWidth(coreConfig.DecodeWidth);
		setIssueWidth(coreConfig.IssueWidth);
		setRetireWidth(coreConfig.RetireWidth);
		setReorderBufferSize(coreConfig.ROBSize);
		setIWSize(coreConfig.IWSize);
		setIntegerRegisterFileSize(coreConfig.IntRegFileSize);
		setFloatingPointRegisterFileSize(coreConfig.FloatRegFileSize);
		setNIntegerArchitecturalRegisters(coreConfig.IntArchRegNum);
		setNFloatingPointArchitecturalRegisters(coreConfig.FloatArchRegNum);
		
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		setBranchMispredictionPenalty(coreConfig.BranchMispredPenalty);
		setNumInorderPipelines(coreConfig.IssueWidth);
		setTreeBarrier(coreConfig.TreeBarrier);
		setBarrierLatency(coreConfig.barrierLatency);
		setBarrierUnit(coreConfig.barrierUnit);
		
		nUnits = new int[FunctionalUnitType.no_of_types.ordinal()];
		latencies = new int[FunctionalUnitType.no_of_types.ordinal() + 2];
					// +2 because memory unit has L1 latency, L2 latency, main memory latency
		
		nUnits[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALUNum;
		nUnits[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulNum;
		nUnits[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivNum;
		nUnits[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALUNum;
		nUnits[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulNum;
		nUnits[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivNum;
		
		latencies[FunctionalUnitType.integerALU.ordinal()] = coreConfig.IntALULatency;
		latencies[FunctionalUnitType.integerMul.ordinal()] = coreConfig.IntMulLatency;
		latencies[FunctionalUnitType.integerDiv.ordinal()] = coreConfig.IntDivLatency;
		latencies[FunctionalUnitType.floatALU.ordinal()] = coreConfig.FloatALULatency;
		latencies[FunctionalUnitType.floatMul.ordinal()] = coreConfig.FloatMulLatency;
		latencies[FunctionalUnitType.floatDiv.ordinal()] = coreConfig.FloatDivLatency;
	}
	
	/*public void boot()
	{
		//set up initial events in the queue
		eventQueue.addEvent(new PerformDecodeEvent(GlobalClock.getCurrentTime(), this, 0));
//TODO commented only for perfect pipeline		
		if (perfectPipeline == false)
			eventQueue.addEvent(new PerformCommitsEvent(GlobalClock.getCurrentTime(), this));
	}*/
	
	/*public void work()
	{
		execEngine.work();
	}*/

	/*public long getClock() {
		return clock;
	}

	public void setClock(long clock) {
		this.clock = clock;
	}
	
	public void incrementClock()
	{
		this.clock++;
	}*/
	
	private void setBarrierLatency(int barrierLatency) {
		this.barrier_latency = barrierLatency;
		
	}
	private void setBarrierUnit(int barrierUnit){
		this.barrierUnit = barrierUnit;
	}
	public void activatePipeline(){
		this.pipelineInterface.resumePipeline();
	}
	public void sleepPipeline(){
		
		((MultiIssueInorderExecutionEngine)this.getExecEngine()).getFetchUnitIn().inputToPipeline.enqueue(Instruction.getSyncInstruction());
	}

	public void setTreeBarrier(boolean bar)
	{
		TreeBarrier = bar;
	}
	public int getIssueWidth() {
		return issueWidth;
	}

	public int getNumInorderPipelines() {
		return numInorderPipelines;
	}

	public void setNumInorderPipelines(int numInorderPipelines) {
		this.numInorderPipelines = numInorderPipelines;
	}

	public void setIssueWidth(int issueWidth) {
		this.issueWidth = issueWidth;
	}

	public int getRetireWidth() {
		return retireWidth;
	}

	public void setRetireWidth(int retireWidth) {
		this.retireWidth = retireWidth;
	}

	public EventQueue getEventQueue() {
		return eventQueue;
	}
	
	public void setEventQueue(EventQueue _eventQueue) {
		eventQueue = _eventQueue;
	}

	public ExecutionEngine getExecEngine() {
		return execEngine;
	}

	public int getBranchMispredictionPenalty() {
		return branchMispredictionPenalty;
	}

	public void setBranchMispredictionPenalty(int branchMispredictionPenalty) {
		this.branchMispredictionPenalty = branchMispredictionPenalty;
	}

	public int getDecodeWidth() {
		return decodeWidth;
	}

	public void setDecodeWidth(int decodeWidth) {
		this.decodeWidth = decodeWidth;
	}

	public int getFloatingPointRegisterFileSize() {
		return floatingPointRegisterFileSize;
	}

	public void setFloatingPointRegisterFileSize(int floatingPointRegisterFileSize) {
		this.floatingPointRegisterFileSize = floatingPointRegisterFileSize;
	}

	public int getIntegerRegisterFileSize() {
		return integerRegisterFileSize;
	}

	public void setIntegerRegisterFileSize(int integerRegisterFileSize) {
		this.integerRegisterFileSize = integerRegisterFileSize;
	}

	public int getNFloatingPointArchitecturalRegisters() {
		return nFloatingPointArchitecturalRegisters;
	}

	public void setNFloatingPointArchitecturalRegisters(
			int floatingPointArchitecturalRegisters) {
		nFloatingPointArchitecturalRegisters = floatingPointArchitecturalRegisters;
	}

	public int getNIntegerArchitecturalRegisters() {
		return nIntegerArchitecturalRegisters;
	}

	public void setNIntegerArchitecturalRegisters(int integerArchitecturalRegisters) {
		nIntegerArchitecturalRegisters = integerArchitecturalRegisters;
	}

	public int getNMachineSpecificRegisters() {
		return nMachineSpecificRegisters;
	}

	public void setNMachineSpecificRegisters(int machineSpecificRegisters) {
		nMachineSpecificRegisters = machineSpecificRegisters;
	}

	public int getReorderBufferSize() {
		return reorderBufferSize;
	}

	public void setReorderBufferSize(int reorderBufferSize) {
		this.reorderBufferSize = reorderBufferSize;
	}
	
	public int[] getAllNUnits()
	{
		return nUnits;
	}
	
	public int[] getAllLatencies()
	{
		return latencies;
	}
	
	public int getLatency(int FUType)
	{
		return latencies[FUType];
	}

	public int getIWSize() {
		return IWSize;
	}

	public void setIWSize(int size) {
		IWSize = size;
	}
	
	public int[] getThreadIDs() {
		return threadIDs;
	}

	public int getNo_of_input_pipes() {
		return no_of_input_pipes;
	}
	
	public int getNo_of_threads() {
		return no_of_threads;
	}
	
	public int getCore_number() {
		return core_number;
	}

	public int getNoOfRegFilePorts() {
		return noOfRegFilePorts;
	}

	public void setNoOfRegFilePorts(int noOfRegFilePorts) {
		this.noOfRegFilePorts = noOfRegFilePorts;
	}

	public int getRegFileOccupancy() {
		return regFileOccupancy;
	}

	public void setRegFileOccupancy(int regFileOccupancy) {
		this.regFileOccupancy = regFileOccupancy;
	}
	
	public long getNoOfInstructionsExecuted() {
		return noOfInstructionsExecuted;
	}

	public void setNoOfInstructionsExecuted(long noOfInstructionsExecuted) {
		this.noOfInstructionsExecuted = noOfInstructionsExecuted;
	}
	
	public void incrementNoOfInstructionsExecuted()
	{
		this.noOfInstructionsExecuted++;
	}
	
	
//	public InorderPipeline getInorderPipeline(){
//		return this.inorderPipeline;
//	}
	
	

	
	public pipeline.PipelineInterface getPipelineInterface() {
		return pipelineInterface;
	}
//	public void setInorderPipeline(InorderPipeline _inorderPipeline){
//		this.inorderPipeline = _inorderPipeline;
//	}
	public void setPipelineInterface(OutOfOrderPipeline pipelineInterface) {
		this.pipelineInterface = pipelineInterface;
	}
	
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inputsToPipeline)
	{
		this.getExecEngine().setInputToPipeline(inputsToPipeline);
	}
	
	public void setStepSize(int stepSize)
	{
		this.stepSize = stepSize;
		this.pipelineInterface.setcoreStepSize(stepSize);
	}

	public long getCoreCyclesTaken() {
		return coreCyclesTaken;
	}

	public void setCoreCyclesTaken(long coreCyclesTaken) {
		this.coreCyclesTaken = coreCyclesTaken;
	}
	
	public long getFrequency()
	{
		return this.frequency;
	}
	
	public void setFrequency(long frequency)
	{
		this.frequency = frequency;
	}
	
	public int getStepSize()
	{
		return stepSize;
	}
	@Override
	public Router getRouter() {
		// TODO Auto-generated method stub
		return router;
	}
	@Override
	public Vector<Integer> getId() {
		// TODO Auto-generated method stub
		return nocElementId;
	}
	public void setId(Vector<Integer> id) {
		// TODO Auto-generated method stub
		nocElementId = id;
	}
	@Override
	public Port getPort() {
		// TODO Auto-generated method stub
		return port;
	}
	@Override
	public SimulationElement getSimulationElement() {
		// TODO Auto-generated method stub
		return this;
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) 
	{
		if (event.getRequestType() == RequestType.Main_Mem_Response )
		{
			handleMainMemoryResponse(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Mem_Response)
		{
			handleMemResponse(eventQ, (AddressCarryingEvent)event);
		}
		else if (event.getRequestType() == RequestType.Cache_Read_Writeback || 
				event.getRequestType() == RequestType.Send_Mem_Response ||
				event.getRequestType() == RequestType.Send_Mem_Response_Invalidate)
		{
			handleAccessWithDirectoryUpdates(eventQ, (AddressCarryingEvent)event);
		}
		else if (event.getRequestType() == RequestType.MESI_Invalidate)
		{
			this.handleInvalidate((AddressCarryingEvent) event);
		}
		else 
		{
			System.err.println(event.getRequestType());
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}	
	private void handleInvalidate(AddressCarryingEvent event)
	{
		this.getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						this.getExecEngine().getCoreMemorySystem().getL1Cache().getLatency(),
						this, 
						this.getExecEngine().getCoreMemorySystem().getL1Cache(),
						event.getRequestType(), 
						event.getAddress(),
						(event).coreId));
	}

	private void handleAccessWithDirectoryUpdates(EventQueue eventQ, AddressCarryingEvent event) 
	{
		this.getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
				new AddressCarryingEvent(
						event.getEventQ(),
						this.getExecEngine().getCoreMemorySystem().getL1Cache().getLatency(),
						this, 
						this.getExecEngine().getCoreMemorySystem().getL1Cache(),
						event.getRequestType(), 
						event.getAddress(),
						(event).coreId));
	}

	private void handleMemResponse(EventQueue eventQ, AddressCarryingEvent event) 
	{
		
		Vector<Integer> id = event.getSourceId();
		Class nocElementClass = SystemConfig.nocConfig.nocElements.nocElements[id.get(0)][id.get(1)].getClass();
		
		if(nocElementClass==Core.class)//memory response from another core's l1 cache
		{
			this.getExecEngine().getCoreMemorySystem().getL1Cache().getPort().put(
					new AddressCarryingEvent(
							event.getEventQ(),
							this.getExecEngine().getCoreMemorySystem().getL1Cache().getLatency(),
							this, 
							this.getExecEngine().getCoreMemorySystem().getL1Cache(),
							event.getRequestType(), 
							event.getAddress(),
							event.coreId));
		}
		else //memory response coming from L2
		{
			//System.err.println("Mem Response");
			AddressCarryingEvent addrEvent = ((AddressCarryingEvent)event);
			if(SystemConfig.nocConfig.ConnType==CONNECTIONTYPE.ELECTRICAL)
			{
				nucaCache.updateMaxHopLength(addrEvent.hopLength,(AddressCarryingEvent)event);
				nucaCache.updateMinHopLength(addrEvent.hopLength);
				nucaCache.updateAverageHopLength(addrEvent.hopLength);
			}
			ArrayList<AddressCarryingEvent> eventsToBeServed = nucaCache.missStatusHoldingRegister.removeRequestsByAddressIfAvailable(addrEvent);
			this.sendResponseToWaitingEvent(eventsToBeServed);
		}
	}
	protected void sendResponseToWaitingEvent(ArrayList<AddressCarryingEvent> outstandingRequestList)
	{
		while (!outstandingRequestList.isEmpty())
		{	
			AddressCarryingEvent eventPoppedOut = (AddressCarryingEvent) outstandingRequestList.remove(0);
			if (eventPoppedOut.getRequestType() == RequestType.Cache_Read)
			{
				sendMemResponse(eventPoppedOut);
			}
			else if (eventPoppedOut.getRequestType() == RequestType.Cache_Write)
			{
				if (nucaCache.writePolicy == CacheConfig.WritePolicy.WRITE_THROUGH)
				{
					MemorySystem.mainMemoryController.getPort().put(eventPoppedOut.updateEvent(eventPoppedOut.getEventQ(), 
							MemorySystem.mainMemoryController.getLatencyDelay(), this, 
							MemorySystem.mainMemoryController, RequestType.Main_Mem_Write,eventPoppedOut.getAddress(),eventPoppedOut.coreId));
				}
			}
		}
	}
	public void sendMemResponse(AddressCarryingEvent eventToRespondTo)
    {
		nucaCache.noOfResponsesSent++;
		eventToRespondTo.getRequestingElement().getPort().put(
											eventToRespondTo.update(
											eventToRespondTo.getEventQ(),
											1,
											eventToRespondTo.getProcessingElement(),
											eventToRespondTo.getRequestingElement(),
											RequestType.Mem_Response));
    }
	protected void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
		AddressCarryingEvent addrEvent = (AddressCarryingEvent) event;
		
		nucaCache.updateMaxHopLength(addrEvent.hopLength,addrEvent);
		nucaCache.updateMinHopLength(addrEvent.hopLength);
		nucaCache.updateAverageHopLength(addrEvent.hopLength);
		
		long addr = addrEvent.getAddress();
		Vector<Integer> sourceId;
		Vector<Integer> destinationId;
		
		if(event.getRequestingElement().getClass() == MainMemoryController.class)
		{
			sourceId = this.getId();
			destinationId = nucaCache.getBankId(addr);
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(event.getEventQ(),
																		0,this, this.getRouter(), 
																		RequestType.Main_Mem_Response, 
																		addr,((AddressCarryingEvent)event).coreId,
																		sourceId,destinationId);
			this.getRouter().getPort().put(addressEvent);
		}
	}
	
	public PowerConfigNew getbPredPower() {
		return bPredPower;
	}

	public void setbPredPower(PowerConfigNew bPredPower) {
		this.bPredPower = bPredPower;
	}

	public PowerConfigNew getDecodePower() {
		return decodePower;
	}

	public void setDecodePower(PowerConfigNew decodePower) {
		this.decodePower = decodePower;
	}

	public PowerConfigNew getIntRATPower() {
		return intRATPower;
	}

	public void setIntRATPower(PowerConfigNew intRATPower) {
		this.intRATPower = intRATPower;
	}

	public PowerConfigNew getFpRATPower() {
		return floatRATPower;
	}

	public void setFpRATPower(PowerConfigNew fpRATPower) {
		this.floatRATPower = fpRATPower;
	}

	public PowerConfigNew getIntFreeListPower() {
		return intFreeListPower;
	}

	public void setIntFreeListPower(PowerConfigNew intFreeListPower) {
		this.intFreeListPower = intFreeListPower;
	}

	public PowerConfigNew getFpFreeListPower() {
		return floatFreeListPower;
	}

	public void setFpFreeListPower(PowerConfigNew fpFreeListPower) {
		this.floatFreeListPower = fpFreeListPower;
	}

	public PowerConfigNew getLsqPower() {
		return lsqPower;
	}

	public void setLsqPower(PowerConfigNew lsqPower) {
		this.lsqPower = lsqPower;
	}

	public PowerConfigNew getIntRegFilePower() {
		return intRegFilePower;
	}

	public void setIntRegFilePower(PowerConfigNew intRegFilePower) {
		this.intRegFilePower = intRegFilePower;
	}

	public PowerConfigNew getFpRegFilePower() {
		return floatRegFilePower;
	}

	public void setFpRegFilePower(PowerConfigNew fpRegFilePower) {
		this.floatRegFilePower = fpRegFilePower;
	}

	public PowerConfigNew getIwPower() {
		return iwPower;
	}

	public void setIwPower(PowerConfigNew iwPower) {
		this.iwPower = iwPower;
	}

	public PowerConfigNew getRobPower() {
		return robPower;
	}

	public void setRobPower(PowerConfigNew robPower) {
		this.robPower = robPower;
	}

	public PowerConfigNew getIntALUPower() {
		return intALUPower;
	}

	public void setIntALUPower(PowerConfigNew intALUPower) {
		this.intALUPower = intALUPower;
	}

	public PowerConfigNew getFloatALUPower() {
		return floatALUPower;
	}

	public void setFloatALUPower(PowerConfigNew floatALUPower) {
		this.floatALUPower = floatALUPower;
	}

	public PowerConfigNew getComplexALUPower() {
		return complexALUPower;
	}

	public void setComplexALUPower(PowerConfigNew complexALUPower) {
		this.complexALUPower = complexALUPower;
	}

	public PowerConfigNew getResultsBroadcastBusPower() {
		return resultsBroadcastBusPower;
	}

	public void setResultsBroadcastBusPower(PowerConfigNew resultsBroadcastBusPower) {
		this.resultsBroadcastBusPower = resultsBroadcastBusPower;
	}

	public PowerConfigNew getiTLBPower() {
		return iTLBPower;
	}

	public void setiTLBPower(PowerConfigNew iTLBPower) {
		this.iTLBPower = iTLBPower;
	}

	public PowerConfigNew getdTLBPower() {
		return dTLBPower;
	}

	public void setdTLBPower(PowerConfigNew dTLBPower) {
		this.dTLBPower = dTLBPower;
	}

	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		PowerConfigNew totalPower = new PowerConfigNew(0, 0);
		
		if(coreCyclesTaken == 0)
		{
			return totalPower;
		}
		
		outputFileWriter.write("\n\n");
		
		// --------- Core Memory System -------------------------
		PowerConfigNew iCachePower =  this.execEngine.getCoreMemorySystem().getiCache().calculateAndPrintPower(outputFileWriter, componentName + ".iCache");
		totalPower.add(totalPower, iCachePower);
		PowerConfigNew iTLBPower =  this.execEngine.getCoreMemorySystem().getiTLB().calculateAndPrintPower(outputFileWriter, componentName + ".iTLB");
		totalPower.add(totalPower, iTLBPower);
		
		PowerConfigNew dCachePower =  this.execEngine.getCoreMemorySystem().getL1Cache().calculateAndPrintPower(outputFileWriter, componentName + ".dCache");
		totalPower.add(totalPower, dCachePower);
		
		PowerConfigNew dTLBPower =  this.execEngine.getCoreMemorySystem().getdTLB().calculateAndPrintPower(outputFileWriter, componentName + ".dTLB");
		totalPower.add(totalPower, dTLBPower);
		
		// -------- Pipeline -----------------------------------
		PowerConfigNew pipelinePower =  this.execEngine.calculateAndPrintPower(outputFileWriter, componentName + ".pipeline");
		totalPower.add(totalPower, pipelinePower);
		
		totalPower.printPowerStats(outputFileWriter, componentName + ".total");
		
		return totalPower;
	}
}
