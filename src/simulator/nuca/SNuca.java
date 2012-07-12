package nuca;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class SNuca extends NucaCache
{
    public boolean lookup(long addr,int coreId)
    {
        long tag = getTag(addr);//get the tag from the address
        long bankNumber = getBankNumber(addr);//get the Bank Number from the address
        int row = (int) (bankNumber /cacheRows);//row number of bank in 2-d array
        int column = (int) (bankNumber % cacheRows); ////column number of bank in 2-d array
        return banks[row][column].lookup(tag);
    }
    public SNuca(int numOfRows,int numOfColumns,int bankSize,int numOfCores,CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
        super(numOfRows, numOfColumns, bankSize,numOfCores,cacheParameters,containingMemSys);
    }
    /*
    public static void main(String args[])
    {
        Cache c = new Cache(2,2,16,64,4);
        SNuca s = new SNuca(c);
        long addr = (long) Math.pow(2, 9);
        

    }*/
}
