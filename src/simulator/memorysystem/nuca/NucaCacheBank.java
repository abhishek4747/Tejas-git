/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

				Contributor: Anuj Arora, Mayur Harne
*****************************************************************************/
package memorysystem.nuca;
import generic.CommunicationInterface;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;
import net.*;
import net.NOC.CONNECTIONTYPE;
import net.NOC.TOPOLOGY;
import java.util.Vector;
import config.CacheConfig;
import config.Interconnect;
import config.SystemConfig;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import memorysystem.nuca.NucaCache.NucaType;

public class NucaCacheBank extends Cache
{
	public CommunicationInterface comInterface;
	CacheConfig cacheParameters;
	boolean isLastLevel;
	boolean isFirstLevel;
	NucaType nucaType;
	TOPOLOGY topology;
	public Policy policy;
	int cacheBankRows;
	int cacheBankColumns;
	NucaCache nucaCache;

	NucaCacheBank(ID bankId,CacheConfig cacheParameters, CoreMemorySystem containingMemSys,NucaCache nucaCache, NucaType nucaType)
    {
		//TODO : cache id can be more intuitive
        super("NucaCacheBank", 0, cacheParameters,containingMemSys);
    	this.timestamp = 0;
    	this.cacheParameters = cacheParameters;
//    	if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
    	if(SystemConfig.interconnect == Interconnect.Bus)
		{
			comInterface = new BusInterface(this);
		}
		else if(SystemConfig.interconnect == Interconnect.Noc)
		{
			comInterface = new NocInterface(SystemConfig.nocConfig, this);
		}
        isLastLevel = false;
        isFirstLevel = false;
        this.nucaType = nucaType;
        topology = SystemConfig.nocConfig.topology;
        policy = new Policy(nucaCache);
        this.nucaCache = nucaCache;
        this.cacheBankColumns = SystemConfig.nocConfig.getNumberOfBankColumns();
        this.cacheBankRows = SystemConfig.nocConfig.getNumberOfBankRows();
        ((NocInterface) this.comInterface).setId(bankId);
    }
    
    @Override
	public void handleEvent(EventQueue eventQ, Event event)
    {
    	if (event.getRequestType() == RequestType.Cache_Read
				|| event.getRequestType() == RequestType.Cache_Write ) 
    	{
			this.handleAccess(eventQ, (AddressCarryingEvent)event);
    	}
		else if (event.getRequestType() == RequestType.Mem_Response)
		{
			this.handleMemResponse(eventQ, event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Read ||
				  event.getRequestType() == RequestType.Main_Mem_Write )
		{
			this.handleMemoryReadWrite(eventQ,event);
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Response )
		{
			handleMainMemoryResponse(eventQ, event);
		}
		else {
			misc.Error.showErrorAndExit(" unexpected request came to cache bank");
		}
	}
	protected void handleMainMemoryResponse(EventQueue eventQ, Event event) 
	{
	}
	

	
	protected void handleMemoryReadWrite(EventQueue eventQ, Event event)
	{
	}

	public void handleAccess(EventQueue eventQ, AddressCarryingEvent event)
	{
	}
}