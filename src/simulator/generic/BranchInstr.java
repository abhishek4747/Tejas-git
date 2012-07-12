package generic;

public class BranchInstr 
{
	public boolean branchTaken;
	public long branchAddress;
	
	public BranchInstr(boolean branchTaken, long branchAddress)
	{
		this.branchTaken = branchTaken;
		this.branchAddress = branchAddress;
	}
	
}
