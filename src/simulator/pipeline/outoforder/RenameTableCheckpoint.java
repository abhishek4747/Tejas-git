package pipeline.outoforder;

/**
 * when roll back is required, the rename table is rolled back to the mapping stored in the
 * checkpoint
 */
public class RenameTableCheckpoint {
	
	int registerFileSize;
	int[] mapping;
	
	public RenameTableCheckpoint(int registerFileSize) {
		
		this.registerFileSize = registerFileSize;
		mapping = new int[registerFileSize];
		for(int i = 0; i < this.registerFileSize; i++)
		{
			mapping[i] = i;
		}
	}
	
	public boolean isInCheckpoint(int archReg, int phyReg)
	{
		if(mapping[archReg] == phyReg)
			return true;
		else
			return false;
	}

	public int getMapping(int index) {
		return mapping[index];
	}

	public void setMapping(int mapping, int index) {
		this.mapping[index] = mapping;
	}

}