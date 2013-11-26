package pipeline.multi_issue_inorder;

import java.io.FileWriter;
import java.io.IOException;

import pipeline.outoforder.OutOrderExecutionEngine;
import generic.Instruction;

import config.PowerConfigNew;
import config.SimulationConfig;
import main.CustomObjectPool;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;

public class WriteBackUnitIn_MII extends SimulationElement{
	
	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	StageLatch_MII memWbLatch;
	
	long instCtr; //for debug
	
	long numIntRegFileAccesses;
	long numFloatRegFileAccesses;
	
	public WriteBackUnitIn_MII(Core core, MultiIssueInorderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, core.getNoOfRegFilePorts(), -1 ,core.getEventQueue(), -1, -1);
		this.core = core;
		containingExecutionEngine = execEngine;
		memWbLatch = execEngine.getMemWbLatch();
		
		instCtr = 0;
	}
	
	public void performWriteBack(MultiIssueInorderPipeline inorderPipeline)
	{
		if(containingExecutionEngine.getMispredStall() > 0)
		{
			return;
		}
		
		Instruction ins = null;
		
		while(memWbLatch.isEmpty() == false)
		{
			ins = memWbLatch.peek(0);
			if(ins != null)
			{
				OperationType opType = ins.getOperationType(); 
			
				//check if simulation complete
				if(ins.getOperationType()==OperationType.inValid)
				{
					this.core.currentThreads--;
					
					if(this.core.currentThreads == 0){   //set exec complete only if there are n other thread already 
														  //assigned to this pipeline	
						containingExecutionEngine.setExecutionComplete(true);
						if(SimulationConfig.pinpointsSimulation == false)
						{					
							containingExecutionEngine.setTimingStatistics();			
							containingExecutionEngine.setPerCoreMemorySystemStatistics();
						}
						else
						{
							Statistics.processEndOfSlice();
						}
					}
				}
				else
				{
					//issue store
					if(ins.getOperationType() == OperationType.store)
					{
						if(!SimulationConfig.detachMemSys)
						{
							//System.out.println(" store issue at time  "+ GlobalClock.getCurrentTime() +" for address " + ins.getSourceOperand1().getValue());
							boolean memReqIssued = containingExecutionEngine.multiIssueInorderCoreMemorySystem.issueRequestToL1Cache(
									RequestType.Cache_Write,
									ins.getSourceOperand1MemValue());
							
							if(memReqIssued == false)
							{
								break;
							}
						}
					}
					
					if(core.getNoOfInstructionsExecuted()%1000000==0)
					{
						System.out.println(core.getNoOfInstructionsExecuted()/1000000 + " million done" + " by core "+core.getCore_number() 
								+ " global clock cycle " + GlobalClock.getCurrentTime());
					}
					core.incrementNoOfInstructionsExecuted();
				}
				
				if(ins.getSerialNo() != instCtr && ins.getOperationType() != OperationType.inValid)
				{
					misc.Error.showErrorAndExit("wb out of order!!");
				}
				instCtr++;	
				
				if(SimulationConfig.debugMode)
				{
					System.out.println("write back : " + GlobalClock.getCurrentTime()/core.getStepSize() + "\n"  + ins + "\n");
				}
				
				memWbLatch.poll();				
				try
				{
					CustomObjectPool.getInstructionPool().returnObject(ins);
					core.numReturns++;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
			{
				break;
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {

	}
	
	void incrementIntNumRegFileAccesses(int incrementBy)
	{
		numIntRegFileAccesses += incrementBy * core.getStepSize();
	}
	
	void incrementFloatNumRegFileAccesses(int incrementBy)
	{
		numFloatRegFileAccesses += incrementBy * core.getStepSize();
	}
	
	public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double intLeakagePower = core.getIntRegFilePower().leakagePower;
		double intDynamicPower = core.getIntRegFilePower().dynamicPower;
		double floatLeakagePower = core.getFpRegFilePower().leakagePower;
		double floatDynamicPower = core.getFpRegFilePower().dynamicPower;
		
		double intActivityFactor = (double)numIntRegFileAccesses
									/(double)core.getCoreCyclesTaken()
									/(containingExecutionEngine.getIssueWidth() * 2);		
		double floatActivityFactor = (double)numFloatRegFileAccesses
									/(double)core.getCoreCyclesTaken()
									/(containingExecutionEngine.getIssueWidth() * 2);
										//register file accessed at rename and write-back
		
		PowerConfigNew totalPower = new PowerConfigNew(0, 0);
		PowerConfigNew intRegFilePower = new PowerConfigNew(intLeakagePower,
																intDynamicPower * intActivityFactor);
		totalPower.add(totalPower, intRegFilePower);
		PowerConfigNew floatRegFilePower = new PowerConfigNew(floatLeakagePower,
																floatDynamicPower * floatActivityFactor);
		totalPower.add(totalPower, floatRegFilePower);
		
		intRegFilePower.printPowerStats(outputFileWriter, componentName + ".int");
		floatRegFilePower.printPowerStats(outputFileWriter, componentName + ".float");
		
		return totalPower;
	}
}
