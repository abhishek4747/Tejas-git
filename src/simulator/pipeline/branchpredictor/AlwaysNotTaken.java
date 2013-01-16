package pipeline.branchpredictor;

public class AlwaysNotTaken implements BranchPredictor {

	@Override
	public void Train(long address, boolean outcome, boolean predict) {
		

	}

	@Override
	public boolean predict(long address, boolean outcome) {
		
		return false;
	}

}
