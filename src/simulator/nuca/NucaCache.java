package nuca;
import java.util.Vector;
import java.util.Hashtable;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class NucaCache extends Cache
{
    /*cache is assumed to in the form of a 2 dimensional array*/
    NucaCacheBank banks[][];
    int cacheRows;
    int cacheColumns;
    int numOfCores;// number of cores present on system
    Vector<Hashtable> coreNetworkHash;
    Vector<Vector<Vector<Integer>>> coreNetworkVector;
    NucaCache(int numOfRows,int numOfColumns,int bankSize,int numOfCores,CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
        super(cacheParameters,containingMemSys);
    	this.cacheRows = numOfRows;
        this.cacheColumns = numOfColumns;
        this.banks=new NucaCacheBank[numOfRows][numOfColumns];
        this.numOfCores = numOfCores;
        for(int i=0;i<numOfRows;i++)
        {
            for(int j=0;j<numOfColumns;j++)
            {
                banks[i][j] = new NucaCacheBank(bankSize);
            }
        }
        this.coreNetworkVector = genrateCoreNetworkVector(numOfCores);
        this.coreNetworkHash = generateCoreNetworkHash(numOfCores);
    }

    private Vector<Vector<Vector<Integer>>> genrateCoreNetworkVector(int numOfCores)//generates the network of cachebanks for each core in the form of a vector
    {
        Vector<Vector<Vector<Integer>>> coreNetworks = new Vector<Vector<Vector<Integer>>>();
        Vector<Vector<Integer>> coreNet = new Vector<Vector<Integer>>();
        Vector<Integer> cluster = new Vector<Integer>();
        int numOfCacheBanks = cacheColumns*cacheRows;
        for(int l=0;l<numOfCores;l++)
        {
            for(int i=0;i<numOfCacheBanks;)
            {
                for(int j=0;j<cacheRows;j++)
                {
                    cluster.add(i++);
                }
                Vector<Integer> temp = (Vector<Integer>) cluster.clone();
                coreNet.add(temp);
            }
            Vector<Vector<Integer>> temp = (Vector<Vector<Integer>>) cluster.clone();
            coreNetworks.add(temp);            
        }
        return coreNetworks;
    }
    
    private Vector<Hashtable> generateCoreNetworkHash(int numOfCores)
    {
        Vector<Hashtable> coreNetworks = new Vector<Hashtable>();
        Hashtable coreNet = new Hashtable();
        int numOfCacheBanks = cacheColumns*cacheRows;

        for(int i=0;i<numOfCores;i++)
        {
            for(int j=0;j<numOfCacheBanks;)
            {
                for(int k=0;k<cacheRows;k++)
                {
                    coreNet.put(j++, k+1);
                }
            }
            Hashtable temp = (Hashtable) coreNet.clone();
            coreNetworks.add(temp);
        }
        return coreNetworks;
    }
    public long getTag(long addr)
    {
        int numOfBanks = cacheColumns*cacheRows;
        long numOfBits = (long)( Math.log10(numOfBanks)/Math.log10(2))+blockSizeBits;
        long tag = addr >>> numOfBits;
        return tag;
    }

    public long getBankNumber(long addr)
    {
        int numOfBanks = cacheColumns*cacheRows;
        long numOfBankBits = (long)( Math.log10(numOfBanks)/Math.log10(2));
        long bankNumber = (addr >>> blockSizeBits) & numOfBankBits;
        return bankNumber;
    }
    public int getBankLevel(int coreId,int bankNumber)
    {
    	Hashtable coreNet = coreNetworkHash.get(coreId);
    	return Integer.parseInt(coreNet.get(bankNumber).toString());
    }
}