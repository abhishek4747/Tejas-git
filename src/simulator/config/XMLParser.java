/*****************************************************************************
				Tejas Simulator
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

	Contributors:  Moksh Upadhyay, Abhishek Sagar
*****************************************************************************/
package config;

import emulatorinterface.communication.IpcBase;
import generic.MultiPortingType;

import java.io.File;
import java.math.RoundingMode;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.*;
import net.NOC.CONNECTIONTYPE;
import memorysystem.Cache;
import memorysystem.Cache.CacheType;
import memorysystem.Cache.CoherenceType;

import org.w3c.dom.*;

import config.BranchPredictorConfig.*;

import power.PowerConfig;

import memorysystem.nuca.NucaCache.Mapping;
import memorysystem.nuca.NucaCache.NucaType;

import generic.PortType;

public class XMLParser 
{
	private static Document doc;

	public static void parse(String fileName) 
	{ 
		try 
		{
			File file = new File(fileName);
			DocumentBuilderFactory DBFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder DBuilder = DBFactory.newDocumentBuilder();
			doc = DBuilder.parse(file);
			doc.getDocumentElement().normalize();
			//System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
			
			setEmulatorParameters();
			setSimulationParameters();
			setPowerParameters();
			
			setSystemParameters();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
 	}
	
	// For an ith core specified, mark the ith field of this long as 1. 
	private static long parseMapper (String s) 
	{
		long ret = 0;
		String delims = "[,]+";
		String[] tokens = s.split(delims);
		for (int i=0; i<tokens.length; i++) 
		{
			String delimsin = "[-]+";
			String[] tokensin = tokens[i].split(delimsin);
			if (tokensin.length == 1) 
			{
				ret = ret | (1 << Integer.parseInt(tokensin[0]));
			}
			else if (tokensin.length == 2) 
			{
				int start = Integer.parseInt(tokensin[0]);
				int end = Integer.parseInt(tokensin[1]);
				for (int j=start; j<=end; j++) 
				{
					ret = ret | (1 << j);
				}
			}
			else 
			{
				System.out.println("Physical Core mapping not correct in config.xml");
				System.exit(0);
			}
		}
		return ret;
	}
		private static void setPowerParameters(){
		NodeList nodeLst = doc.getElementsByTagName("Power");
		Node powerNode = nodeLst.item(0);
		Element powerElmnt = (Element) powerNode;
		 PowerConfig.totalPower=Double.parseDouble(getImmediateString("total_power", powerElmnt));
		  PowerConfig.totalPowerNodcache2=Double.parseDouble(getImmediateString("total_power_nodcache2", powerElmnt));
		  PowerConfig.ialuPower=Double.parseDouble(getImmediateString("ialu_power", powerElmnt));
		  PowerConfig.faluPower=Double.parseDouble(getImmediateString("falu_power", powerElmnt));
		  PowerConfig.bpredPower=Double.parseDouble(getImmediateString("bpred_power", powerElmnt));
		  PowerConfig.renamePower=Double.parseDouble(getImmediateString("rename_power", powerElmnt));
		  PowerConfig.ratPower=Double.parseDouble(getImmediateString("rat_power", powerElmnt));
		  PowerConfig.dclPower=Double.parseDouble(getImmediateString("dcl_power", powerElmnt));
		  PowerConfig.windowPower=Double.parseDouble(getImmediateString("window_power", powerElmnt));
		  PowerConfig.lsqPower=Double.parseDouble(getImmediateString("lsq_power", powerElmnt));
		  PowerConfig.wakeupPower=Double.parseDouble(getImmediateString("wakeup_power", powerElmnt));
		  PowerConfig.lsqWakeupPower=Double.parseDouble(getImmediateString("lsq_wakeup_power", powerElmnt));
		  PowerConfig.rsPower=Double.parseDouble(getImmediateString("rs_power", powerElmnt));
		  PowerConfig.rsPowerNobit=Double.parseDouble(getImmediateString("rs_power_nobit", powerElmnt));
		  PowerConfig.lsqRsPower=Double.parseDouble(getImmediateString("lsq_rs_power", powerElmnt));
		  PowerConfig.lsqRsPowerNobit=Double.parseDouble(getImmediateString("lsq_rs_power_nobit", powerElmnt));
		  PowerConfig.selectionPower=Double.parseDouble(getImmediateString("selection_power", powerElmnt));
		  PowerConfig.regfilePower=Double.parseDouble(getImmediateString("regfile_power", powerElmnt));
		  PowerConfig.regfilePowerNobit=Double.parseDouble(getImmediateString("regfile_power_nobit", powerElmnt));
		  PowerConfig.resultPower=Double.parseDouble(getImmediateString("result_power", powerElmnt));
		  PowerConfig.icachePower=Double.parseDouble(getImmediateString("icache_power", powerElmnt));
		  PowerConfig.dcachePower=Double.parseDouble(getImmediateString("dcache_power", powerElmnt));
		  PowerConfig.dcache2Power=Double.parseDouble(getImmediateString("dcache2_power", powerElmnt));
		  PowerConfig.clockPower=Double.parseDouble(getImmediateString("clock_power", powerElmnt));

		  PowerConfig.totalRouterEnergy = Double.parseDouble(getImmediateString("RouterEnergy", powerElmnt));
		  PowerConfig.bufferEnergy = Double.parseDouble(getImmediateString("BufferEnergy", powerElmnt));
		  PowerConfig.linkEnergy = Double.parseDouble(getImmediateString("LinkEnergy", powerElmnt));
		  
		  PowerConfig.itlb=Double.parseDouble(getImmediateString("itlb", powerElmnt));
		  PowerConfig.dtlb=Double.parseDouble(getImmediateString("dtlb", powerElmnt));
		  PowerConfig.resultbus=Double.parseDouble(getImmediateString("resultbus", powerElmnt));
		  PowerConfig.selection=Double.parseDouble(getImmediateString("selection", powerElmnt));
		  
		  PowerConfig.ruuDecodeWidth=Double.parseDouble(getImmediateString("ruu_decode_width", powerElmnt));
		  PowerConfig.ruuIssueWidth=Double.parseDouble(getImmediateString("ruu_issue_width", powerElmnt));
		  PowerConfig.ruuCommitWidth=Double.parseDouble(getImmediateString("ruu_commit_width", powerElmnt));
		  PowerConfig.resMemport=Double.parseDouble(getImmediateString("res_memport", powerElmnt));
		  PowerConfig.resIalu=Double.parseDouble(getImmediateString("res_ialu", powerElmnt));
		  PowerConfig.resFpalu=Double.parseDouble(getImmediateString("res_fpalu", powerElmnt));
		  PowerConfig.il1Port=Double.parseDouble(getImmediateString("il1_port", powerElmnt));
		  PowerConfig.dl1Port=Double.parseDouble(getImmediateString("dl1_port", powerElmnt));
		  PowerConfig.dl2Port=Double.parseDouble(getImmediateString("dl2_port", powerElmnt));
		  
	}
		
	private static void setEmulatorParameters() {
		NodeList nodeLst = doc.getElementsByTagName("Emulator");
		Node emulatorNode = nodeLst.item(0);
		Element emulatorElmnt = (Element) emulatorNode;
		
		EmulatorConfig.EmulatorType = Integer.parseInt(getImmediateString("EmulatorType", emulatorElmnt));
		EmulatorConfig.CommunicationType = Integer.parseInt(getImmediateString("CommunicationType", emulatorElmnt));
		EmulatorConfig.PinTool = getImmediateString("PinTool", emulatorElmnt);
		EmulatorConfig.PinInstrumentor = getImmediateString("PinInstrumentor", emulatorElmnt);
		EmulatorConfig.QemuTool = getImmediateString("QemuTool", emulatorElmnt);
		EmulatorConfig.ShmLibDirectory = getImmediateString("ShmLibDirectory", emulatorElmnt);
	}
	
	private static void setSimulationParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("Simulation");
		Node simulationNode = nodeLst.item(0);
		Element simulationElmnt = (Element) simulationNode;
		SimulationConfig.NumTempIntReg = Integer.parseInt(getImmediateString("NumTempIntReg", simulationElmnt));
		SimulationConfig.NumInsToIgnore = Long.parseLong(getImmediateString("NumInsToIgnore", simulationElmnt));
		
		int tempVal = Integer.parseInt(getImmediateString("IndexAddrModeEnable", simulationElmnt));
		if (tempVal == 0)
			SimulationConfig.IndexAddrModeEnable = false;
		else
			SimulationConfig.IndexAddrModeEnable = true;
		
		SimulationConfig.MapEmuCores = parseMapper(getImmediateString("EmuCores", simulationElmnt));
		SimulationConfig.MapJavaCores = parseMapper(getImmediateString("JavaCores", simulationElmnt));
		
		//System.out.println(SimulationConfig.NumTempIntReg + ", " + SimulationConfig.IndexAddrModeEnable);
		
		if(getImmediateString("DebugMode", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("DebugMode", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.debugMode = true;
		}
		else
		{
			SimulationConfig.debugMode = false;
		}
		if(getImmediateString("DetachMemSys", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("DetachMemSys", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.detachMemSys = true;
		}
		else
		{
			SimulationConfig.detachMemSys = false;
		}
		
		if(Integer.parseInt(getImmediateString("PipelineType", simulationElmnt))==1){
			SimulationConfig.isPipelineInorder = true;
			SimulationConfig.isPipelineMultiIssueInorder = false;
			SimulationConfig.isPipelineOutOfOrder = false;
		}
		else if(Integer.parseInt(getImmediateString("PipelineType", simulationElmnt))==2){
			SimulationConfig.isPipelineInorder = false;
			SimulationConfig.isPipelineMultiIssueInorder = true;
			SimulationConfig.isPipelineOutOfOrder = false;
		}
		else if(Integer.parseInt(getImmediateString("PipelineType", simulationElmnt))==3){
			SimulationConfig.isPipelineInorder = false;
			SimulationConfig.isPipelineMultiIssueInorder = false;
			SimulationConfig.isPipelineOutOfOrder = true;
		}
		else{
			System.err.println("Please specify any of the four pipeline types in the config file");
		}
		SimulationConfig.numInorderPipelines = Integer.parseInt(getImmediateString("NumInorderPipelines", simulationElmnt));
		
		if(getImmediateString("writeToFile", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("writeToFile", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.writeToFile = true;
		}
		else
		{
			SimulationConfig.writeToFile = false;
		}
		
		SimulationConfig.numInstructionsToBeWritten = Integer.parseInt(getImmediateString("numInstructionsToBeWritten", simulationElmnt));
		SimulationConfig.InstructionsFilename = getImmediateString("InstructionsFilename", simulationElmnt);
		
		if(getImmediateString("subsetSim", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("subsetSim", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.subsetSimulation = true;
			SimulationConfig.subsetSimSize = Long.parseLong(getImmediateString("subsetSimSize", simulationElmnt));
		}
		else
		{
			SimulationConfig.subsetSimulation = false;
			SimulationConfig.subsetSimSize = -1;
		}
		
		if(getImmediateString("pinpointsSim", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("pinpointsSim", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.pinpointsSimulation = true;
			SimulationConfig.pinpointsFile = getImmediateString("pinpointsFile", simulationElmnt);
		}
		else
		{
			SimulationConfig.pinpointsSimulation = false;
			SimulationConfig.pinpointsFile = "";
		}

		if(getImmediateString("PrintPowerStats", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("subsetSim", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.powerStats = true;
		}
		else
		{
			SimulationConfig.powerStats = false;
		}
		
		if(getImmediateString("Broadcast", simulationElmnt).toLowerCase().compareTo("true") == 0)
		{
			SimulationConfig.broadcast = true;
		}
		else
		{
			SimulationConfig.broadcast = false;
		}
		
		SimulationConfig.powerTrace = Integer.parseInt(getImmediateString("PowerTrace", simulationElmnt));
		SimulationConfig.numInsForTrace = Long.parseLong(getImmediateString("NumInsForTrace", simulationElmnt));
		SimulationConfig.numCyclesForTrace = Long.parseLong(getImmediateString("NumCyclesForTrace", simulationElmnt));

	}
	
	private static void setSystemParameters()
	{
		SystemConfig.declaredCaches = new Hashtable<String, CacheConfig>(); //Declare the hash table for declared caches
		
		NodeList nodeLst = doc.getElementsByTagName("System");
		Node systemNode = nodeLst.item(0);
		Element systemElmnt = (Element) systemNode;
		
		//Read number of cores and define the array of core configurations
		//Note that number of Cores specified in config.xml is deprecated and is instead done as follows
		SystemConfig.NoOfCores = IpcBase.MaxNumJavaThreads*IpcBase.getEmuThreadsPerJavaThread();
		SystemConfig.mainMemoryLatency = Integer.parseInt(getImmediateString("MainMemoryLatency", systemElmnt));
		SystemConfig.mainMemoryFrequency = Long.parseLong(getImmediateString("MainMemoryFrequency", systemElmnt));
		SystemConfig.mainMemPortType = setPortType(getImmediateString("MainMemoryPortType", systemElmnt));
		SystemConfig.mainMemoryAccessPorts = Integer.parseInt(getImmediateString("MainMemoryAccessPorts", systemElmnt));
		SystemConfig.mainMemoryPortOccupancy = Integer.parseInt(getImmediateString("MainMemoryPortOccupancy", systemElmnt));
		SystemConfig.cacheBusLatency = Integer.parseInt(getImmediateString("CacheBusLatency", systemElmnt));

		SystemConfig.core = new CoreConfig[SystemConfig.NoOfCores];
		SystemConfig.directoryAccessLatency = Integer.parseInt(getImmediateString("directoryAccessLatency", systemElmnt));
		SystemConfig.memWBDelay = Integer.parseInt(getImmediateString("memWBDelay", systemElmnt));
		SystemConfig.dataTransferDelay = Integer.parseInt(getImmediateString("dataTransferDelay", systemElmnt));
		SystemConfig.invalidationSendDelay = Integer.parseInt(getImmediateString("invalidationSendDelay", systemElmnt));
		SystemConfig.invalidationAckCollectDelay = Integer.parseInt(getImmediateString("invalidationAckCollectDelay", systemElmnt));
		SystemConfig.ownershipChangeDelay = Integer.parseInt(getImmediateString("ownershipChangeDelay", systemElmnt));
		SystemConfig.dirNetworkDelay = Integer.parseInt(getImmediateString("dirNetworkDelay", systemElmnt));
	
		NodeList powerLst = doc.getElementsByTagName("Power");
		Node powerNode = powerLst.item(0);
		Element powerElmnt = (Element) powerNode;
		SystemConfig.clockGatingStyle = Integer.parseInt(getImmediateString("clockGatingStyle", powerElmnt));

		//Set core parameters
		NodeList coreLst = systemElmnt.getElementsByTagName("Core");
		//for (int i = 0; i < SystemConfig.NoOfCores; i++)
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			SystemConfig.core[i] = new CoreConfig();
			CoreConfig core = SystemConfig.core[i]; //To be locally used for assignments
		
			Element coreElmnt = (Element) coreLst.item(0);
			
			core.frequency = Long.parseLong(getImmediateString("CoreFrequency", coreElmnt));
			
			core.LSQSize = Integer.parseInt(getImmediateString("LSQSize", coreElmnt));
			core.LSQLatency = Integer.parseInt(getImmediateString("LSQLatency", coreElmnt));
			core.LSQPortType = setPortType(getImmediateString("LSQPortType", coreElmnt));
			core.LSQAccessPorts = Integer.parseInt(getImmediateString("LSQAccessPorts", coreElmnt));
			core.LSQPortOccupancy = Integer.parseInt(getImmediateString("LSQPortOccupancy", coreElmnt));
			core.LSQMultiportType = setMultiPortingType(getImmediateString("LSQMultiPortingType", coreElmnt));
			
			core.TLBSize = Integer.parseInt(getImmediateString("TLBSize", coreElmnt));
			core.TLBLatency = Integer.parseInt(getImmediateString("TLBLatency", coreElmnt));
			core.TLBMissPenalty = Integer.parseInt(getImmediateString("TLBMissPenalty", coreElmnt));
			core.TLBPortType = setPortType(getImmediateString("TLBPortType", coreElmnt));
			core.TLBAccessPorts = Integer.parseInt(getImmediateString("TLBAccessPorts", coreElmnt));
			core.TLBPortOccupancy = Integer.parseInt(getImmediateString("TLBPortOccupancy", coreElmnt));

			core.DecodeWidth = Integer.parseInt(getImmediateString("DecodeWidth", coreElmnt));
			core.IssueWidth = Integer.parseInt(getImmediateString("IssueWidth", coreElmnt));
			core.RetireWidth = Integer.parseInt(getImmediateString("RetireWidth", coreElmnt));
			core.DecodeTime = Integer.parseInt(getImmediateString("DecodeTime", coreElmnt));
			core.RenamingTime = Integer.parseInt(getImmediateString("RenamingTime", coreElmnt));
			core.ROBSize = Integer.parseInt(getImmediateString("ROBSize", coreElmnt));
			core.IWSize = Integer.parseInt(getImmediateString("IWSize", coreElmnt));
			core.IntRegFileSize = Integer.parseInt(getImmediateString("IntRegFileSize", coreElmnt));
			core.FloatRegFileSize = Integer.parseInt(getImmediateString("FloatRegFileSize", coreElmnt));
			core.IntArchRegNum = Integer.parseInt(getImmediateString("IntArchRegNum", coreElmnt));
			core.FloatArchRegNum = Integer.parseInt(getImmediateString("FloatArchRegNum", coreElmnt));
			core.MSRegNum = Integer.parseInt(getImmediateString("MSRegNum", coreElmnt));
			core.RegFilePortType = setPortType(getImmediateString("RegFilePortType", coreElmnt));
			core.RegFilePorts = Integer.parseInt(getImmediateString("RegFilePorts", coreElmnt));
			core.RegFileOccupancy = Integer.parseInt(getImmediateString("RegFileOccupancy", coreElmnt));
			core.BranchMispredPenalty = Integer.parseInt(getImmediateString("BranchMispredPenalty", coreElmnt));
			
			core.IntALUNum = Integer.parseInt(getImmediateString("IntALUNum", coreElmnt));
			core.IntMulNum = Integer.parseInt(getImmediateString("IntMulNum", coreElmnt));
			core.IntDivNum = Integer.parseInt(getImmediateString("IntDivNum", coreElmnt));
			core.FloatALUNum = Integer.parseInt(getImmediateString("FloatALUNum", coreElmnt));
			core.FloatMulNum = Integer.parseInt(getImmediateString("FloatMulNum", coreElmnt));
			core.FloatDivNum = Integer.parseInt(getImmediateString("FloatDivNum", coreElmnt));
			core.AddressFUNum = Integer.parseInt(getImmediateString("AddressFUNum", coreElmnt));
			
			core.IntALULatency = Integer.parseInt(getImmediateString("IntALULatency", coreElmnt));
			core.IntMulLatency = Integer.parseInt(getImmediateString("IntMulLatency", coreElmnt));
			core.IntDivLatency = Integer.parseInt(getImmediateString("IntDivLatency", coreElmnt));
			core.FloatALULatency = Integer.parseInt(getImmediateString("FloatALULatency", coreElmnt));
			core.FloatMulLatency = Integer.parseInt(getImmediateString("FloatMulLatency", coreElmnt));
			core.FloatDivLatency = Integer.parseInt(getImmediateString("FloatDivLatency", coreElmnt));
			core.AddressFULatency = Integer.parseInt(getImmediateString("AddressFULatency", coreElmnt));
			//core.numInorderPipelines = Integer.parseInt(getImmediateString("NumInorderPipelines", coreElmnt));
		
			if(getImmediateString("TreeBarrier", coreElmnt).compareTo("true") == 0)
				core.TreeBarrier = true;
			else
				core.TreeBarrier = false;
			core.barrierLatency = Integer.parseInt(getImmediateString("BarrierLatency", coreElmnt));
			
			String tempStr = getImmediateString("BarrierUnit", coreElmnt);
			if (tempStr.equalsIgnoreCase("Central"))
				core.barrierUnit = 0;
			else if (tempStr.equalsIgnoreCase("Distributed"))
				core.barrierUnit = 1;
			else{
				System.err.println("Only Central and Distributed allowed as barrier unit");
				System.exit(0);
			}
			//Code for instruction cache configurations for each core
			NodeList iCacheList = coreElmnt.getElementsByTagName("iCache");
			Element iCacheElmnt = (Element) iCacheList.item(0);
			String cacheType = iCacheElmnt.getAttribute("type");
			Element typeElmnt = searchLibraryForItem(cacheType);
//			core.iCache.isFirstLevel = true;
			core.iCache.levelFromTop = CacheType.iCache;
			setCacheProperties(typeElmnt, core.iCache);
			core.iCache.nextLevel = iCacheElmnt.getAttribute("nextLevel");
			core.iCache.operatingFreq = core.frequency;
			
			//Code for L1 Data cache configurations for each core
			NodeList l1CacheList = coreElmnt.getElementsByTagName("L1Cache");
			Element l1Elmnt = (Element) l1CacheList.item(0);
			cacheType = l1Elmnt.getAttribute("type");
			typeElmnt = searchLibraryForItem(cacheType);
//			core.l1Cache.isFirstLevel = true;
			core.l1Cache.levelFromTop = CacheType.L1;
			setCacheProperties(typeElmnt, core.l1Cache);
			core.l1Cache.nextLevel = l1Elmnt.getAttribute("nextLevel");
			core.l1Cache.operatingFreq = core.frequency;
			
			//Code for L1 cache configurations for each core
			//NodeList l2CacheList = coreElmnt.getElementsByTagName("L2Cache");
			//Element l2Elmnt = (Element) l2CacheList.item(0);
			//cacheType = l2Elmnt.getAttribute("type");
			//typeElmnt = searchLibraryForItem(cacheType);
			//setCacheProperties(typeElmnt, core.l2Cache);
			//core.l1Cache.nextLevel = l1Elmnt.getAttribute("nextLevel");
			
			//Code for L1 cache configurations for each core
			//NodeList l3CacheList = coreElmnt.getElementsByTagName("L3Cache");
			//Element l3Elmnt = (Element) l3CacheList.item(0);
			//cacheType = l3Elmnt.getAttribute("type");
			//typeElmnt = searchLibraryForItem(cacheType);
			//setCacheProperties(typeElmnt, core.l3Cache);
			//core.l1Cache.nextLevel = l1Elmnt.getAttribute("nextLevel");
		}
		
		//Set Directory Parameters
		SystemConfig.directoryConfig = new CacheConfig();
		NodeList dirLst=systemElmnt.getElementsByTagName("Directory");
		Element dirElmnt = (Element) dirLst.item(0);
		setCacheProperties(dirElmnt, SystemConfig.directoryConfig);
		
		//Code for remaining Cache configurations
		NodeList cacheLst = systemElmnt.getElementsByTagName("Cache");
		for (int i = 0; i < cacheLst.getLength(); i++)
		{
			Element cacheElmnt = (Element) cacheLst.item(i);
			String cacheName = cacheElmnt.getAttribute("name");

			if (!(SystemConfig.declaredCaches.containsKey(cacheName)))	//If the identically named cache is not already present
			{
				CacheConfig newCacheConfigEntry = new CacheConfig();
//				newCacheConfigEntry.isFirstLevel = false;
				newCacheConfigEntry.levelFromTop = Cache.CacheType.Lower;
				String cacheType = cacheElmnt.getAttribute("type");
				Element cacheTypeElmnt = searchLibraryForItem(cacheType);
				setCacheProperties(cacheTypeElmnt, newCacheConfigEntry);
				newCacheConfigEntry.nextLevel = cacheElmnt.getAttribute("nextLevel");
				newCacheConfigEntry.operatingFreq = Long.parseLong(cacheElmnt.getAttribute("frequency"));
				SystemConfig.declaredCaches.put(cacheName, newCacheConfigEntry);
			}
		}
		
		
		//set Predictor Parameters
		SystemConfig.branchPredictor = new BranchPredictorConfig();
		NodeList predictorLst = systemElmnt.getElementsByTagName("Predictor");
		Element predictorElmnt = (Element) predictorLst.item(0);
		
		NodeList BTBLst = systemElmnt.getElementsByTagName("BTB");
		Element BTBElmnt = (Element) BTBLst.item(0);
		setBranchPredictorProperties(predictorElmnt, BTBElmnt, SystemConfig.branchPredictor);
		
		//set NOC Parameters
		SystemConfig.nocConfig = new NocConfig();
		NodeList NocLst = systemElmnt.getElementsByTagName("NOC");
		Element NocElmnt = (Element) NocLst.item(0);
		setNocProperties(NocElmnt, SystemConfig.nocConfig);
	}

	private static void setNocProperties(Element NocType, NocConfig nocConfig)
	{
		NodeList NocLst = NocType.getElementsByTagName("L2");
		Element NocElmnt = (Element) NocLst.item(0);
		setL2NocProperties(NocElmnt, nocConfig);
	}
	private static void setL2NocProperties(Element NocType, NocConfig nocConfig)
	{
		nocConfig.numberOfBuffers = Integer.parseInt(getImmediateString("NocNumberOfBuffers", NocType));
		nocConfig.portType = setPortType(getImmediateString("NocPortType", NocType));
		nocConfig.accessPorts = Integer.parseInt(getImmediateString("NocAccessPorts", NocType));
		nocConfig.portOccupancy = Integer.parseInt(getImmediateString("NocPortOccupancy", NocType));
		nocConfig.latency = Integer.parseInt(getImmediateString("NocLatency", NocType));
		nocConfig.operatingFreq = Integer.parseInt(getImmediateString("NocOperatingFreq", NocType));
		nocConfig.numberOfBankColumns = Integer.parseInt(getImmediateString("NumberOfBankColumns", NocType));
		nocConfig.numberOfBankRows = Integer.parseInt(getImmediateString("NumberOfBankRows", NocType));
		nocConfig.latencyBetweenBanks = Integer.parseInt(getImmediateString("NocLatencyBetweenBanks", NocType));
		
		String tempStr = getImmediateString("NucaMapping", NocType);
		if (tempStr.equalsIgnoreCase("S"))
			nocConfig.mapping = Mapping.SET_ASSOCIATIVE;
		else if (tempStr.equalsIgnoreCase("A"))
			nocConfig.mapping = Mapping.ADDRESS;
		else if (tempStr.equalsIgnoreCase("B"))
			nocConfig.mapping = Mapping.BOTH;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'Nuca' (please enter 'S', D' or 'N')");
			System.exit(1);
		}
		
		tempStr = getImmediateString("NocTopology", NocType);
		if(tempStr.equalsIgnoreCase("MESH"))
			nocConfig.topology = NOC.TOPOLOGY.MESH;
		else if(tempStr.equalsIgnoreCase("TORUS"))
			nocConfig.topology = NOC.TOPOLOGY.TORUS;
		else if(tempStr.equalsIgnoreCase("BUS"))
			nocConfig.topology = NOC.TOPOLOGY.BUS;
		else if(tempStr.equalsIgnoreCase("RING"))
			nocConfig.topology = NOC.TOPOLOGY.RING;
		else if(tempStr.equalsIgnoreCase("FATTREE"))
			nocConfig.topology = NOC.TOPOLOGY.FATTREE;
		else if(tempStr.equalsIgnoreCase("OMEGA"))
			nocConfig.topology = NOC.TOPOLOGY.OMEGA;
		else if(tempStr.equalsIgnoreCase("BUTTERFLY"))
			nocConfig.topology = NOC.TOPOLOGY.BUTTERFLY;
		
		tempStr = getImmediateString("NocRoutingAlgorithm", NocType);
		if(tempStr.equalsIgnoreCase("SIMPLE"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.SIMPLE;
		else if(tempStr.equalsIgnoreCase("WESTFIRST"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.WESTFIRST;
		else if(tempStr.equalsIgnoreCase("NORTHLAST"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.NORTHLAST;
		else if(tempStr.equalsIgnoreCase("NEGATIVEFIRST"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.NEGATIVEFIRST;
		else if(tempStr.equalsIgnoreCase("FATTREE"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.FATTREE;
		else if(tempStr.equalsIgnoreCase("OMEGA"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.OMEGA;
		else if(tempStr.equalsIgnoreCase("BUTTERFLY"))
			nocConfig.rAlgo = RoutingAlgo.ALGO.BUTTERFLY;

		tempStr = getImmediateString("NocSelScheme", NocType);
		if(tempStr.equalsIgnoreCase("STATIC"))
			nocConfig.selScheme = RoutingAlgo.SELSCHEME.STATIC;
		else
			nocConfig.selScheme = RoutingAlgo.SELSCHEME.ADAPTIVE;
		tempStr = getImmediateString("NocRouterArbiter", NocType);
		if(tempStr.equalsIgnoreCase("RR"))
			nocConfig.arbiterType = RoutingAlgo.ARBITER.RR_ARBITER;
		else if(tempStr.equalsIgnoreCase("MATRIX"))
			nocConfig.arbiterType = RoutingAlgo.ARBITER.MATRIX_ARBITER;
		else
			nocConfig.arbiterType = RoutingAlgo.ARBITER.QUEUE_ARBITER;
		nocConfig.technologyPoint = Integer.parseInt(getImmediateString("TechPoint", NocType));
		tempStr = getImmediateString("NocConnection", NocType);
		if(tempStr.equalsIgnoreCase("ELECTRICAL"))
			nocConfig.ConnType = CONNECTIONTYPE.ELECTRICAL;
		else
			nocConfig.ConnType = CONNECTIONTYPE.OPTICAL;	
	}
	private static void setCacheProperties(Element CacheType, CacheConfig cache)
	{
		String tempStr = getImmediateString("WriteMode", CacheType);
		if (tempStr.equalsIgnoreCase("WB"))
			cache.writePolicy = CacheConfig.WritePolicy.WRITE_BACK;
		else if (tempStr.equalsIgnoreCase("WT"))
			cache.writePolicy = CacheConfig.WritePolicy.WRITE_THROUGH;
		else
		{
			System.err.println("XML Configuration error : Invalid Write Mode (please enter WB for write-back or WT for write-through)");
			System.exit(1);
		}
		
		//System.out.println(cache.writeMode);
		
		cache.blockSize = Integer.parseInt(getImmediateString("BlockSize", CacheType));
		cache.assoc = Integer.parseInt(getImmediateString("Associativity", CacheType));
		cache.size = Integer.parseInt(getImmediateString("Size", CacheType));
		cache.latency = Integer.parseInt(getImmediateString("Latency", CacheType));
		cache.portType = setPortType(getImmediateString("PortType", CacheType));
		cache.accessPorts = Integer.parseInt(getImmediateString("AccessPorts", CacheType));
		cache.portOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", CacheType));
		cache.multiportType = setMultiPortingType(getImmediateString("MultiPortingType", CacheType));
		cache.mshrSize = Integer.parseInt(getImmediateString("MSHRSize", CacheType));
				
		tempStr = getImmediateString("Coherence", CacheType);
		if (tempStr.equalsIgnoreCase("N"))
			cache.coherence = CoherenceType.None;
		else if (tempStr.equalsIgnoreCase("S"))
			cache.coherence = CoherenceType.Snoopy;
		else if (tempStr.equalsIgnoreCase("D"))
			cache.coherence = CoherenceType.Directory;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'Coherence' (please enter 'S', D' or 'N')");
			System.exit(1);
		}
		cache.numberOfBuses = Integer.parseInt(getImmediateString("NumBuses", CacheType));
		cache.busOccupancy = Integer.parseInt(getImmediateString("BusOccupancy", CacheType));
		
		tempStr = getImmediateString("Nuca", CacheType);
		if (tempStr.equalsIgnoreCase("N")) {
			SimulationConfig.nucaType = NucaType.NONE;
			cache.nucaType = NucaType.NONE;
		}
		else if (tempStr.equalsIgnoreCase("S"))
		{
			SimulationConfig.nucaType = NucaType.S_NUCA;
			cache.nucaType = NucaType.S_NUCA;
		}
		else if (tempStr.equalsIgnoreCase("D"))
		{
			SimulationConfig.nucaType = NucaType.D_NUCA;
			cache.nucaType = NucaType.D_NUCA;
		}
		else if (tempStr.equalsIgnoreCase("CBD"))
		{
			SimulationConfig.nucaType = NucaType.CB_D_NUCA;
			cache.nucaType = NucaType.CB_D_NUCA;
		}
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'Nuca' (please enter 'S', D' or 'N')");
			System.exit(1);
		}
		
		
		
	tempStr = getImmediateString("LastLevel", CacheType);
		if (tempStr.equalsIgnoreCase("Y"))
			cache.isLastLevel = true;
		else if (tempStr.equalsIgnoreCase("N"))
			cache.isLastLevel = false;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'isLastLevel' (please enter 'Y' for yes or 'N' for no)");
			System.exit(1);
		}
		
	}
	private static void setBranchPredictorProperties(Element predictorElmnt, Element BTBElmnt, BranchPredictorConfig branchPredictor){
		
		String tempStr = getImmediateString("Predictor_Mode", predictorElmnt);
		if(tempStr.equalsIgnoreCase("NoPredictor"))
			branchPredictor.predictorMode = BP.NoPredictor;
		else if(tempStr.equalsIgnoreCase("PerfectPredictor"))
			branchPredictor.predictorMode = BP.PerfectPredictor;
		else if(tempStr.equalsIgnoreCase("AlwaysTaken"))
			branchPredictor.predictorMode = BP.AlwaysTaken;
		else if(tempStr.equalsIgnoreCase("AlwaysNotTaken"))
			branchPredictor.predictorMode = BP.AlwaysNotTaken;
		else if(tempStr.equalsIgnoreCase("Tournament"))
			branchPredictor.predictorMode = BP.Tournament;
		else if(tempStr.equalsIgnoreCase("Bimodal"))
			branchPredictor.predictorMode = BP.Bimodal;
		else if(tempStr.equalsIgnoreCase("GAg"))
			branchPredictor.predictorMode = BP.GAg;
		else if(tempStr.equalsIgnoreCase("GAp"))
			branchPredictor.predictorMode = BP.GAp;
		else if(tempStr.equalsIgnoreCase("GShare"))
			branchPredictor.predictorMode = BP.GShare;
		else if(tempStr.equalsIgnoreCase("PAg"))
			branchPredictor.predictorMode = BP.PAg;
		else if(tempStr.equalsIgnoreCase("PAp"))
			branchPredictor.predictorMode = BP.PAp;
		
		branchPredictor.PCBits = Integer.parseInt(getImmediateString("PCBits", predictorElmnt));
		branchPredictor.BHRsize = Integer.parseInt(getImmediateString("BHRsize", predictorElmnt));
		branchPredictor.saturating_bits = Integer.parseInt(getImmediateString("saturating_bits", predictorElmnt));
	}
	
	private static boolean setDirectoryCoherent(String immediateString) {
		if(immediateString==null)
			return false;
		if(immediateString.equalsIgnoreCase("T"))
			return true;
		else
			return false;
	}

	private static Element searchLibraryForItem(String tagName)	//Searches the <Library> section for a given tag name and returns it in Element form
	{															// Used mainly for cache types
		NodeList nodeLst = doc.getElementsByTagName("Library");
		Element libraryElmnt = (Element) nodeLst.item(0);
		NodeList libItemLst = libraryElmnt.getElementsByTagName(tagName);
		
		if (libItemLst.item(0) == null) //Item not found
		{
			System.err.println("XML Configuration error : Item type \"" + tagName + "\" not found in library section in the configuration file!!");
			System.exit(1);
		}
		
		if (libItemLst.item(1) != null) //Item found more than once
		{
			System.err.println("XML Configuration error : More than one definitions of item type \"" + tagName + "\" found in library section in the configuration file!!");
			System.exit(1);
		}
		
		Element resultElmnt = (Element) libItemLst.item(0);
		return resultElmnt;
	}
	
	private static String getImmediateString(String tagName, Element parent) // Get the immediate string value of a particular tag name under a particular parent tag
	{
		NodeList nodeLst = parent.getElementsByTagName(tagName);
		if (nodeLst.item(0) == null)
		{
			System.err.println("XML Configuration error : Item \"" + tagName + "\" not found inside the \"" + parent.getTagName() + "\" tag in the configuration file!!");
			System.exit(1);
		}
	    Element NodeElmnt = (Element) nodeLst.item(0);
	    NodeList resultNode = NodeElmnt.getChildNodes();
	    return ((Node) resultNode.item(0)).getNodeValue();
	}
	
	private static MultiPortingType setMultiPortingType(String inputStr)
	{
		MultiPortingType result = null;
		if (inputStr.equalsIgnoreCase("G"))
			result = MultiPortingType.GENUINE;
		else if (inputStr.equalsIgnoreCase("B"))
			result = MultiPortingType.BANKED;
		else
		{
			System.err.println("XML Configuration error : Invalid Multiporting type specified");
			System.exit(1);
		}
		return result;
	}
	
	private static PortType setPortType(String inputStr)
	{
		PortType result = null;
		if (inputStr.equalsIgnoreCase("UL"))
			result = PortType.Unlimited;
		else if (inputStr.equalsIgnoreCase("FCFS"))
			result = PortType.FirstComeFirstServe;
		else if (inputStr.equalsIgnoreCase("PR"))
			result = PortType.PriorityBased;
		else
		{
			System.err.println("XML Configuration error : Invalid Port Type type specified");
			System.exit(1);
		}
		return result;
	}
}
