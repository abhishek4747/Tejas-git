package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;

import java.util.Hashtable;
import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;
import config.SystemConfig;

public class DNuca extends NucaCache {

	DNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys,SystemConfig sysConfig) 
	{
		super(cacheParameters,containingMemSys,sysConfig);
	}

	public void migrateCacheBank(int cacheBankNumber,int coreId)//migrate the given cache bank to
                                                               //corresponding Core's nearset cache banks
    {
        Hashtable coreNetHash = coreNetworkHash.get(coreId);
        int level = Integer.parseInt(coreNetHash.get(cacheBankNumber).toString()) -1;
        Vector<Integer> coreNetVector =coreNetworkVector.get(coreId).get(level);
        for(int i=0;i<coreNetVector.size();i++)
        {
            if(coreNetVector.get(i) == cacheBankNumber)
            {
                int lruCacheBank = getLRUCacheBank(coreId, level);//get cache bank number from level 1 that is LRU
                int row1 = lruCacheBank/cacheRows;
                int column1 = lruCacheBank%cacheRows;
                int row2 = cacheBankNumber/cacheRows;
                int column2 = cacheBankNumber%cacheRows;
                /*swap the two cache banks */
                NucaCacheBank temp   = (NucaCacheBank) cacheBank[row1][column1].clone();
                cacheBank[row1][column1] = (NucaCacheBank) cacheBank[row2][column2].clone() ;
                cacheBank[row2][column2] = (NucaCacheBank) temp.clone();
                break;
            }
        }
    }
    
	int getLRUCacheBank(int coreId,int level)//returns the cache bank number of bank that is LRU in a bank cluster
    {
        Vector<Integer> coreNetCluster = coreNetworkVector.get(coreId).get(level);//cluster of banks at specified at level
        int bankNumber = coreNetCluster.get(0);
        int row = bankNumber/cacheRows;
        int column = bankNumber%cacheRows;
        double minTimestamp = cacheBank[row][column].getTimestamp();
        int cacheBankNumber = coreNetCluster.get(0);
        for(int i=0;i<coreNetCluster.size();i++)
        {
            bankNumber = coreNetCluster.get(i);
            row = bankNumber/ cacheRows;
            column = bankNumber%cacheRows;
            if(minTimestamp > cacheBank[row][column].getTimestamp() )
            {
                minTimestamp = cacheBank[row][column].getTimestamp();
                cacheBankNumber = bankNumber;
            }
        }
        return cacheBankNumber;
    }

	@Override
	public long getTag(long addr) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		long address = ((AddressCarryingEvent)(event)).getAddress();
		int currentlevel = ((AddressCarryingEvent)(event)).getCurrentLevel();
		boolean flag = ((AddressCarryingEvent)(event)).isFlag();
		Vector<Integer> sourceBankId = getSourceBankId(address);
		Vector<Integer> destinationBankId = getDestinationBankId(address);
		sourceBankId.add(currentlevel);
		if(currentlevel == 0 && !flag)
			destinationBankId.add(currentlevel);
		else
			destinationBankId.add(currentlevel+1);
		this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)].
									getPort().put(((AddressCarryingEvent)event).
															updateEvent(eventQ, 
																		0, 
																		this, 
																		this.cacheBank[sourceBankId.get(0)][sourceBankId.get(1)], 
																		event.getRequestType(), 
																		sourceBankId, 
																		destinationBankId,
																		currentlevel+1));

	}


	@Override
	public Vector<Integer> getDestinationBankId(long addr) {
		// TODO Auto-generated method stub
		return getSourceBankId(addr); 
	}

	@Override
	public Vector<Integer> getSourceBankId(long addr) {
		// TODO Auto-generated method stub
		int numOfBanks = getNumOfBanks();
		long laddr = addr >>> blockSizeBits;
		laddr = laddr%numOfBanks;
		laddr = laddr - (laddr%associativity);
		Vector<Integer> sourceBankId = new Vector<Integer>();
		sourceBankId.add((int) (laddr/cacheColumns));
		return null;
	}
	
}