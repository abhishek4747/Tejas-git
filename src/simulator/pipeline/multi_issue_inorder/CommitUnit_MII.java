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

	public void performCommit(MultiIssueInorderPipeline inorderPipeline) {
		if (containingExecutionEngine.getMispredStall() > 0)
			return;
		this.containingExecutionEngine.getROB().performCommit();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub

	}

}
