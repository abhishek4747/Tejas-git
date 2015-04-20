package pipeline.multi_issue_inorder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class CommitUnit_MII extends SimulationElement {

	Core core;
	MultiIssueInorderExecutionEngine containingExecutionEngine;
	
	public CommitUnit_MII(Core core, MultiIssueInorderExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.containingExecutionEngine = execEngine;
	}
	
	public void performCommit(MultiIssueInorderPipeline inorderPipeline){
		if (containingExecutionEngine.getMispredStall() > 0)
			return;
		this.containingExecutionEngine.getROB().performCommit();
		// if Non branch instruction
			// Wait untill instruction reaches head of ROB
			// Update RF
			// Remove Instruction from ROB
		// else 
			// Wait untill instuction reaches head of ROB
			// if branch is mispredicted
				// Flush ROB
				// Restart Execution at correct successor of the branch instruction
			// else
				// Remove instruction from ROB
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

}
