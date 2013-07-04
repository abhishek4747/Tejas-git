package memorysystem.directory;

import java.util.Comparator;

public class DirectoryEntryComparator implements Comparator<DirectoryEntry> 
{
	public int compare(DirectoryEntry newEvent0, DirectoryEntry newEvent1)
	{
		if(newEvent0.getTimestamp() < newEvent1.getTimestamp())
		{
			return -1;
		}

		else if(newEvent0.getTimestamp() > newEvent1.getTimestamp())
		{
			return 1;
		}
		
		else
		{
			return 0;
		}
	}
}