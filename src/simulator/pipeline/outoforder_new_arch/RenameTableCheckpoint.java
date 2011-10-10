package pipeline.outoforder_new_arch;

/**
 * when roll back is required, the rename table is rolled back to the mapping stored in the
 * checkpoint
 */
public class RenameTableCheckpoint {
	
	int noOfThreads;
	int registerFileSize;
	int[][] mapping;
	
	public RenameTableCheckpoint(int noOfThreads, int registerFileSize) {
		
		this.noOfThreads = noOfThreads;
		this.registerFileSize = registerFileSize;
		
		mapping = new int[noOfThreads][registerFileSize];
		int temp;
		for(int j = 0; j < noOfThreads; j++)
		{
			temp = j * registerFileSize;
			for(int i = 0; i < this.registerFileSize; i++)
			{
				mapping[j][i] = temp + i;
			}
		}
	}
	
	public boolean isInCheckpoint(int threadID, int archReg, int phyReg)
	{
		if(mapping[threadID][archReg] == phyReg)
			return true;
		else
			return false;
	}

	public int getMapping(int threadID, int archReg) {
		return mapping[threadID][archReg];
	}

	public void setMapping(int threadID, int mapping, int archReg) {
		this.mapping[threadID][archReg] = mapping;
	}

}