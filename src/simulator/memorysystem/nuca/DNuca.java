package memorysystem.nuca;

import java.util.HashMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import memorysystem.nuca.NucaCache.Mapping;
import memorysystem.nuca.NucaCache.NucaType;
import net.NOC.CONNECTIONTYPE;
import net.optical.TopLevelTokenBus;
import config.CacheConfig;
import config.SystemConfig;

public class DNuca extends NucaCache{

	int numOfBankSets;
	Vector<Integer> bankSetnum;
	static long eventId;
	HashMap<Integer,Vector<Vector<Integer>>> bankSetNumToBankIds;
	public DNuca(CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,
			NucaType nucaType) 
	{
		super(cacheParameters, containingMemSys, tokenbus, nucaType);
		bankSetNumToBankIds = new HashMap<Integer,Vector<Vector<Integer>>>();
		bankSetnum = new Vector<Integer>();
		makeCacheBanks(cacheParameters, containingMemSys, tokenbus,nucaType,this);
		makeBankSets();
	}
	protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, DNuca nucaCache)
   	{
       	int rows = SystemConfig.nocConfig.getNumberOfBankRows();
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.coresCacheLocations.get(i).get(j)==0)
   				{
   					Vector<Integer> bankId = new Vector<Integer>();
   					bankId.add(i);
   					bankId.add(j);
   					cacheBank.add(new DNucaBank(bankId, cacheParameters, containingMemSys, this, nucaType));
   				}
   			}
   		}
   	}
	void makeBankSets()
	{
		int rows = SystemConfig.nocConfig.getNumberOfBankRows();
		
       	int cols = SystemConfig.nocConfig.getNumberOfBankColumns();
   		for(int i=0;i<rows;i++)
   		{
   			for(int j=0;j<cols;j++)
   			{
   				if(SystemConfig.nocConfig.nocElements.coresCacheLocations.get(i).get(j)==0)
   				{
   					Vector<Integer> bankId = new Vector<Integer>();
   					bankId.add(i);
   					bankId.add(j);
   					if(bankSetNumToBankIds.get(i)==null)
   					{
   						numOfBankSets++;
   						bankSetnum.add(i);
   						Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
   						temp.add(bankId);
   						bankSetNumToBankIds.put(i,temp);
   					}
   					else
   					{
   						Vector<Vector<Integer>> temp=bankSetNumToBankIds.get(i);
   						temp.add(bankId);
   						bankSetNumToBankIds.put(i,temp);
   					}
   				}
   			}
   		}
	}
	void putEventToRouter(AddressCarryingEvent addrEvent)
	{
		long address = addrEvent.getAddress();
		Vector<Integer> sourceId = getCoreId(addrEvent.coreId);
		int bankSet = getBankSetId(address);
		Vector<Integer> destinationId = getNearestBank(bankSet, sourceId);
		AddressCarryingEvent eventToBeSent = new AddressCarryingEvent(addrEvent.getEventQ(),
											 0,ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter(), 
											 SystemConfig.nocConfig.nocElements.nocElements[destinationId.get(0)][destinationId.get(1)].getSimulationElement(),
											 addrEvent.getRequestType(),
											 address,addrEvent.coreId,
											 sourceId,destinationId);
		//eventToBeSent.oldSourceBankId = new Vector<Integer>(sourceBankId);
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL) 
		{
			ArchitecturalComponent.getCores()[addrEvent.coreId].getRouter().
			getPort().put(eventToBeSent);
		}
	}
	int getBankSetId(long addr)
	{
		if(mapping == Mapping.SET_ASSOCIATIVE) 
		{
			long tag = (addr>>>(numSetsBits+blockSizeBits));
			//System.out.println("bankNumber long " + bankNumber + "bankNumberInt =" + (int)(bankNumber & (getNumOfBanks()-1)));
			return (int)(tag & (getNumOfBankSets()-1));
		}
		else if(mapping == Mapping.ADDRESS)
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			return (int)(tag & (getNumOfBankSets()-1));
		}
		else
		{
			long tag = (addr>>>(numLinesBits+blockSizeBits));
			return (int)(tag & (getNumOfBankSets()-1));
		}
	}
	Vector<Integer> getNearestBank(int bankSet,Vector<Integer> coreId)
	{
		//System.err.println(bankSetNumToBankIds);
		Vector<Vector<Integer>> bankIds = bankSetNumToBankIds.get(bankSetnum.get(bankSet));
		//System.err.println(bankIds);
		Vector<Integer> nearestBankId=null;
		int min=Integer.MAX_VALUE;
		for(Vector<Integer> bankId:bankIds)
		{
			int dist = (coreId.get(0) - bankId.get(0))*(coreId.get(0) - bankId.get(0)) + 
					   (coreId.get(1) - bankId.get(1))*(coreId.get(1) - bankId.get(1)) ;
			if(dist<min)
			{
				min=dist;
				nearestBankId = bankId;
			}
		}
		return nearestBankId;
	}
	private int getNumOfBankSets() {
		
		return numOfBankSets;
	}
}
