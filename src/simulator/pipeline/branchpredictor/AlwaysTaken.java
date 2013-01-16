package pipeline.branchpredictor;

public class AlwaysTaken implements BranchPredictor {

	@Override
	public void Train(long address, boolean outcome, boolean predict) {
		

	}

	@Override
	public boolean predict(long address, boolean outcome) {
		
		return true;
	}

}
