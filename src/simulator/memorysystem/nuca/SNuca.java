package memorysystem.nuca;


import java.util.Vector;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;
import net.NOC.CONNECTIONTYPE;
import net.optical.TopLevelTokenBus;
import config.SystemConfig;

public class SNuca extends NucaCache
{
    public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys, TopLevelTokenBus tokenbus,NucaType nucaType)
    {
    	super(cacheParameters, containingMemSys, tokenbus,nucaType);
    	makeCacheBanks(cacheParameters, containingMemSys, tokenbus,nucaType,this);
    }
    protected void makeCacheBanks(CacheConfig cacheParameters,CoreMemorySystem containingMemSys, TopLevelTokenBus tokenBus, NucaType nucaType, SNuca nucaCache)
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
   					cacheBank.add(new SNucaBank(bankId, cacheParameters, containingMemSys, this, nucaType));
   				}
   			}
   		}
   	}
    void putEventToRouter(AddressCarryingEvent addrEvent)
	{
		long address = addrEvent.getAddress();
		Vector<Integer> sourceId = getCoreId(addrEvent.coreId);
		Vector<Integer> destinationId = getBankId(address);
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
}		