package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Hashtable;

import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.DestinationBankEvent;
import misc.Util;
import config.CacheConfig;
import net.RoutingAlgo;
import config.SystemConfig;

public abstract class NucaCache extends Cache
{
	public enum NucaType{
		S_NUCA,
		D_NUCA,
		NONE
	}
	
    /*cache is assumed to in the form of a 2 dimensional array*/
    NucaCacheBank cacheBank[][];
    int cacheRows;
    int cacheColumns;
    int numOfCores;// number of cores present on system
    int cacheSize;//cache size in bytes
    int associativity;
    int blockSizeBits;
    NucaCache(CacheConfig cacheParameters, CoreMemorySystem containingMemSys)
    {
    	super(cacheParameters, containingMemSys);
    	this.cacheRows = cacheParameters.getNumberOfBankRows();
        this.cacheColumns = cacheParameters.getNumberOfBankColumns();
        this.cacheBank=new NucaCacheBank[cacheRows][cacheColumns];
        this.numOfCores = SystemConfig.NoOfCores;
        this.cacheSize = cacheParameters.getSize();
        this.associativity = cacheParameters.getAssoc();
        this.blockSizeBits = Util.logbase2(cacheParameters.getBlockSize());
        for(int i=0;i<cacheRows;i++)
        {
            for(int j=0;j<cacheColumns;j++)
            {
            	Vector<Integer> bankId = new Vector<Integer>(2);
            	bankId.add(i);
            	bankId.add(j);
            	cacheBank[i][j] = new NucaCacheBank(bankId,cacheParameters,containingMemSys);
            }
        }
        makeCacheBanks(cacheParameters, containingMemSys);
    }

    private void ConnectBanks(int bankRows,int bankColumns)  //connect bank in MESH fashion
	{
		int i,j;
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				if(i==0)                        //setting null for 0th raw up connection
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
				else
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, this.cacheBank[i-1][j]);
				
				if(j==bankColumns-1)             //right connections
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
				else
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, this.cacheBank[i][j+1]);
				
				if(i==bankRows-1)             //down connections
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
				else
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, this.cacheBank[i+1][j]);
				
				if(j==0)			            //left connections
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
				else
					this.cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, cacheBank[i][j-1]);
			}
		}
	}

    private void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys)
	{
		int bankColumns,bankRows,i,j;
		
		bankColumns = cacheParameters.getNumberOfBankColumns();  //number banks should be power of 2 otherwise truncated
		bankRows = cacheParameters.getNumberOfBankRows();  //number banks should be power of 2 otherwise truncated
		this.cacheBank = new NucaCacheBank[bankRows][bankColumns];
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				Vector<Integer> bankId = new Vector<Integer>(2);
				bankId.clear();
				bankId.add(i);
				bankId.add(j);
				this.cacheBank[i][j] = new NucaCacheBank(bankId,cacheParameters,containingMemSys);
			}
		}
		ConnectBanks(bankRows,bankColumns);
	}

    public Vector<Integer> integerToBankId(int bankNumber)
	{
		Vector<Integer> id = new Vector<Integer>(2);
		id.add((bankNumber/cacheColumns));
		id.add((bankNumber%cacheColumns));
		return id;
	}
	
	public int bankIdtoInteger(Vector<Integer> bankId)
	{
		int bankNumber = bankId.get(0)*cacheColumns + bankId.get(1);
		return bankNumber;
	}
	
	public int getNumOfBanks()
	{
		return cacheRows*cacheColumns;		
	}
	
	public void setStatistics()
	{
		for(int i=0;i<cacheRows;i++)
		{
			for(int j=0;j<cacheColumns;j++)
			{
				noOfRequests = noOfRequests + cacheBank[i][j].noOfRequests;
				hits = hits + cacheBank[i][j].hits;
				misses = misses + cacheBank[i][j].misses;
			}			
		}
	}

	public abstract long getTag(long addr);
	public abstract Vector<Integer> getDestinationBankId(long addr);
	public abstract Vector<Integer> getSourceBankId(long addr);
}