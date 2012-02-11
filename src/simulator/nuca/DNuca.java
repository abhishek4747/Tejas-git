package nuca;
import java.util.Hashtable;
import java.util.Vector;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class DNuca extends NucaCache {

	DNuca(int numOfRows, int numOfColumns, int bankSize,int numOfCores,CacheConfig cacheParameters, CoreMemorySystem containingMemSys) 
	{
		super(numOfRows, numOfColumns, bankSize, numOfCores,cacheParameters,containingMemSys);
	}

	public boolean lookup(long addr,int coreId)
    {
        Vector<Vector<Integer>> coreNet = coreNetworkVector.get(coreId);
        long bankNumber = getBankNumber(addr);
        int row = (int) (bankNumber/cacheRows);
        int column = (int) (bankNumber % cacheRows);
        long tag = getTag(addr);
        for(int i=0;i<coreNet.size();i++)
        {
            Vector<Integer> cluster = coreNet.get(i);
            for(int j=0;j<cluster.size();j++)
            {
                if(cluster.get(j)==bankNumber)
                {
                    if(banks[row][column].lookup(tag))
                    {
                        return true;
                        
                    } else
                    {
                        return false;
                    }
                }
            }
        }
        return false;
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
                NucaCacheBank temp   = (NucaCacheBank) banks[row1][column1].clone();
                banks[row1][column1] = (NucaCacheBank) banks[row2][column2].clone() ;
                banks[row2][column2] = (NucaCacheBank) temp.clone();
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
        double minTimestamp = banks[row][column].getTimestamp();
        int cacheBankNumber = coreNetCluster.get(0);
        for(int i=0;i<coreNetCluster.size();i++)
        {
            bankNumber = coreNetCluster.get(i);
            row = bankNumber/ cacheRows;
            column = bankNumber%cacheRows;
            if(minTimestamp > banks[row][column].getTimestamp() )
            {
                minTimestamp = banks[row][column].getTimestamp();
                cacheBankNumber = bankNumber;
            }
        }
        return cacheBankNumber;
    }
}