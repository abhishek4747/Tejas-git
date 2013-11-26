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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.*;
import net.NOC.CONNECTIONTYPE;
import memorysystem.Cache;
import memorysystem.Cache.CacheType;
import memorysystem.Cache.CoherenceType;

import org.w3c.dom.*;

import config.BranchPredictorConfig.*;

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
			
			setSystemParameters();
		} 
		catch (Exception e) 
		{
			misc.Error.showErrorAndExit("Error in reading config file : " + e);
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
	
	private static PowerConfigNew getPowerConfig(Element parent)
	{
		double leakagePower = Double.parseDouble(getImmediateString("LeakagePower", parent));
		double dynamicPower = Double.parseDouble(getImmediateString("DynamicPower", parent));
		
		PowerConfigNew powerConfig = new PowerConfigNew(leakagePower, dynamicPower);
		return powerConfig;
	}
	
	private static CachePowerConfig getCachePowerConfig(Element parent)
	{
		CachePowerConfig powerConfig = new CachePowerConfig();
		powerConfig.leakagePower = Double.parseDouble(getImmediateString("LeakagePower", parent));
		powerConfig.readDynamicPower = Double.parseDouble(getImmediateString("ReadDynamicPower", parent));
		powerConfig.writeDynamicPower = Double.parseDouble(getImmediateString("WriteDynamicPower", parent));
		return powerConfig;
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
		
		Element mainMemElmnt = (Element)(systemElmnt.getElementsByTagName("MainMemory")).item(0);
		SystemConfig.mainMemoryControllerPower = getPowerConfig(mainMemElmnt);
		
		Element globalClockElmnt = (Element)(systemElmnt.getElementsByTagName("GlobalClock")).item(0);
		SystemConfig.globalClockPower = getPowerConfig(globalClockElmnt);
		
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

		//Set core parameters
		NodeList coreLst = systemElmnt.getElementsByTagName("Core");
		//for (int i = 0; i < SystemConfig.NoOfCores; i++)
		for (int i = 0; i < SystemConfig.NoOfCores; i++)
		{
			SystemConfig.core[i] = new CoreConfig();
			CoreConfig core = SystemConfig.core[i]; //To be locally used for assignments
		
			Element coreElmnt = (Element) coreLst.item(0);
			
			core.frequency = Long.parseLong(getImmediateString("CoreFrequency", coreElmnt));
			
			core.pipelineType = PipelineType.valueOf(getImmediateString("PipelineType", coreElmnt));
			
			Element lsqElmnt = (Element)(coreElmnt.getElementsByTagName("LSQ")).item(0);
			core.LSQSize = Integer.parseInt(getImmediateString("LSQSize", lsqElmnt));
			core.LSQLatency = Integer.parseInt(getImmediateString("LSQLatency", lsqElmnt));
			core.LSQPortType = setPortType(getImmediateString("LSQPortType", lsqElmnt));
			core.LSQAccessPorts = Integer.parseInt(getImmediateString("LSQAccessPorts", lsqElmnt));
			core.LSQPortOccupancy = Integer.parseInt(getImmediateString("LSQPortOccupancy", lsqElmnt));
			core.LSQMultiportType = setMultiPortingType(getImmediateString("LSQMultiPortingType", lsqElmnt));
			core.lsqPower = getPowerConfig(lsqElmnt);
			
			Element iTLBElmnt = (Element)(coreElmnt.getElementsByTagName("ITLB")).item(0);
			core.ITLBSize = Integer.parseInt(getImmediateString("Size", iTLBElmnt));
			core.ITLBLatency = Integer.parseInt(getImmediateString("Latency", iTLBElmnt));
			core.ITLBMissPenalty = Integer.parseInt(getImmediateString("MissPenalty", iTLBElmnt));
			core.ITLBPortType = setPortType(getImmediateString("PortType", iTLBElmnt));
			core.ITLBAccessPorts = Integer.parseInt(getImmediateString("AccessPorts", iTLBElmnt));
			core.ITLBPortOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", iTLBElmnt));
			core.iTLBPower = getPowerConfig(iTLBElmnt);
			
			Element dTLBElmnt = (Element)(coreElmnt.getElementsByTagName("DTLB")).item(0);
			core.DTLBSize = Integer.parseInt(getImmediateString("Size", dTLBElmnt));
			core.DTLBLatency = Integer.parseInt(getImmediateString("Latency", dTLBElmnt));
			core.DTLBMissPenalty = Integer.parseInt(getImmediateString("MissPenalty", dTLBElmnt));
			core.DTLBPortType = setPortType(getImmediateString("PortType", dTLBElmnt));
			core.DTLBAccessPorts = Integer.parseInt(getImmediateString("AccessPorts", dTLBElmnt));
			core.DTLBPortOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", dTLBElmnt));
			core.dTLBPower = getPowerConfig(dTLBElmnt);

			Element decodeElmnt = (Element)(coreElmnt.getElementsByTagName("Decode")).item(0);
			core.DecodeWidth = Integer.parseInt(getImmediateString("Width", decodeElmnt));
			core.decodePower = getPowerConfig(decodeElmnt);
			
			Element instructionWindowElmnt = (Element)(coreElmnt.getElementsByTagName("InstructionWindow")).item(0);
			core.IssueWidth = Integer.parseInt(getImmediateString("IssueWidth", instructionWindowElmnt));			
			core.IWSize = Integer.parseInt(getImmediateString("IWSize", instructionWindowElmnt));
			core.iwPower = getPowerConfig(instructionWindowElmnt);

			Element robElmnt = (Element)(coreElmnt.getElementsByTagName("ROB")).item(0);
			core.RetireWidth = Integer.parseInt(getImmediateString("RetireWidth", coreElmnt));
			core.ROBSize = Integer.parseInt(getImmediateString("ROBSize", coreElmnt));
			core.robPower = getPowerConfig(robElmnt);
			
			Element resultsBroadcastBusElmnt = (Element)(coreElmnt.getElementsByTagName("ResultsBroadcastBus")).item(0);
			core.resultsBroadcastBusPower = getPowerConfig(resultsBroadcastBusElmnt);
			
			Element renameElmnt = (Element)(coreElmnt.getElementsByTagName("Rename")).item(0);
			
			Element ratElmnt = (Element)(renameElmnt.getElementsByTagName("RAT")).item(0);
			core.intRATPower = getPowerConfig((Element)ratElmnt.getElementsByTagName("Integer").item(0));
			core.floatRATPower = getPowerConfig((Element)ratElmnt.getElementsByTagName("Float").item(0));
			
			Element freelistElmnt = (Element)(renameElmnt.getElementsByTagName("FreeList")).item(0);
			core.intFreeListPower = getPowerConfig((Element)freelistElmnt.getElementsByTagName("Integer").item(0));
			core.floatFreeListPower = getPowerConfig((Element)freelistElmnt.getElementsByTagName("Float").item(0));			
			
			Element registerFileElmnt = (Element)(coreElmnt.getElementsByTagName("RegisterFile")).item(0);
			
			Element integerRegisterFileElmnt = (Element)(registerFileElmnt.getElementsByTagName("Integer")).item(0);
			core.IntRegFileSize = Integer.parseInt(getImmediateString("IntRegFileSize", integerRegisterFileElmnt));
			core.IntArchRegNum = Integer.parseInt(getImmediateString("IntArchRegNum", integerRegisterFileElmnt));
			core.intRegFilePower = getPowerConfig(integerRegisterFileElmnt);
			
			Element floatRegisterFileElmnt = (Element)(registerFileElmnt.getElementsByTagName("Float")).item(0);
			core.FloatRegFileSize = Integer.parseInt(getImmediateString("FloatRegFileSize", floatRegisterFileElmnt));
			core.FloatArchRegNum = Integer.parseInt(getImmediateString("FloatArchRegNum", floatRegisterFileElmnt));
			core.floatRegFilePower = getPowerConfig(floatRegisterFileElmnt);
			
			Element intALUElmnt = (Element)(coreElmnt.getElementsByTagName("IntALU")).item(0);
			core.IntALUNum = Integer.parseInt(getImmediateString("IntALUNum", intALUElmnt));
			core.IntALULatency = Integer.parseInt(getImmediateString("IntALULatency", intALUElmnt));
			core.intALUPower = getPowerConfig(intALUElmnt);

			Element floatALUElmnt = (Element)(coreElmnt.getElementsByTagName("FloatALU")).item(0);
			core.FloatALUNum = Integer.parseInt(getImmediateString("FloatALUNum", floatALUElmnt));
			core.FloatALULatency = Integer.parseInt(getImmediateString("FloatALULatency", floatALUElmnt));
			core.floatALUPower = getPowerConfig(floatALUElmnt);
			
			Element complexALUElmnt = (Element)(coreElmnt.getElementsByTagName("ComplexALU")).item(0);
			core.IntMulNum = Integer.parseInt(getImmediateString("IntMulNum", complexALUElmnt));
			core.IntDivNum = Integer.parseInt(getImmediateString("IntDivNum", complexALUElmnt));
			core.FloatMulNum = Integer.parseInt(getImmediateString("FloatMulNum", complexALUElmnt));
			core.FloatDivNum = Integer.parseInt(getImmediateString("FloatDivNum", complexALUElmnt));
			core.IntMulLatency = Integer.parseInt(getImmediateString("IntMulLatency", complexALUElmnt));
			core.IntDivLatency = Integer.parseInt(getImmediateString("IntDivLatency", complexALUElmnt));
			core.FloatMulLatency = Integer.parseInt(getImmediateString("FloatMulLatency", complexALUElmnt));
			core.FloatDivLatency = Integer.parseInt(getImmediateString("FloatDivLatency", complexALUElmnt));
			core.complexALUPower = getPowerConfig(complexALUElmnt);
						
			//set Branch Predictor Parameters
			core.branchPredictor = new BranchPredictorConfig();
			Element predictorElmnt = (Element)(coreElmnt.getElementsByTagName("BranchPredictor").item(0));
			Element BTBElmnt = (Element) predictorElmnt.getElementsByTagName("BTB").item(0);
			setBranchPredictorProperties(predictorElmnt, BTBElmnt, core.branchPredictor);
			core.BranchMispredPenalty = Integer.parseInt(getImmediateString("BranchMispredPenalty", predictorElmnt));
			core.bPredPower = getPowerConfig(predictorElmnt);
			
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
			core.iCache.power = getCachePowerConfig(typeElmnt);
			
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
			core.l1Cache.power = getCachePowerConfig(typeElmnt);
			
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
		SystemConfig.directoryConfig.power = getCachePowerConfig(dirElmnt);
		
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
				newCacheConfigEntry.power = getCachePowerConfig(cacheTypeElmnt);
				SystemConfig.declaredCaches.put(cacheName, newCacheConfigEntry);
			}
		}
				
		//set NOC Parameters
		SystemConfig.nocConfig = new NocConfig();
		NodeList NocLst = systemElmnt.getElementsByTagName("NOC");
		Element nocElmnt = (Element) NocLst.item(0);
		SystemConfig.nocConfig.power = getPowerConfig(nocElmnt);
		setNocProperties(nocElmnt, SystemConfig.nocConfig);
		
	}

	private static void setNocProperties(Element NocType, NocConfig nocConfig)
	{
		NodeList NocLst = NocType.getElementsByTagName("L2");
		Element NocElmnt = (Element) NocLst.item(0);
		setL2NocProperties(NocElmnt, nocConfig);
	}
	private static void setL2NocProperties(Element NocType, NocConfig nocConfig)
	{
		if(SimulationConfig.nucaType!=NucaType.NONE)
		{
			String nocConfigFilename = getImmediateString("NocConfigFile", NocType);
			try 
			{
				File outputFile = new File(nocConfigFilename);
				if(!outputFile.exists()) 
				{
					System.err.println("XML Configuration error : NocConfigFile doesnot exist");
					System.exit(1);
				}
				
				BufferedReader readNocConfig = new BufferedReader(new FileReader(outputFile));
				String str;
				StringTokenizer st;
				str=readNocConfig.readLine();
				st = new StringTokenizer(str," ");
			
				nocConfig.nocElements = new NocElements(
						Integer.parseInt((String)st.nextElement()),
						Integer.parseInt((String)st.nextElement()));
				 
				nocConfig.numberOfBankColumns = nocConfig.nocElements.columns; 
				nocConfig.numberOfBankRows = nocConfig.nocElements.rows;
				
				for(int i=0;i<nocConfig.nocElements.rows;i++)
				{
					str=readNocConfig.readLine();
					st = new StringTokenizer(str," ");
					nocConfig.nocElements.coresCacheLocations.add(new Vector<Integer>());
					for(int j=0;j<nocConfig.nocElements.columns;j++)
					{
						nocConfig.nocElements.coresCacheLocations.get(i).add(Integer.parseInt((String)st.nextElement()));
						if(nocConfig.nocElements.coresCacheLocations.get(i).get(j)==1)
							nocConfig.nocElements.noOfCores++;
						else if(nocConfig.nocElements.coresCacheLocations.get(i).get(j)==0)
							nocConfig.nocElements.noOfCacheBanks++;
					}
				}
				str=readNocConfig.readLine();
				st = new StringTokenizer(str," ");
				int numberOfmemoryControllers = Integer.parseInt((String)st.nextElement());
				str=readNocConfig.readLine();
				st = new StringTokenizer(str," ");
				SystemConfig.memoryControllersLocations = new int[numberOfmemoryControllers];
				for(int i=0;i<numberOfmemoryControllers;i++)
				{
					SystemConfig.memoryControllersLocations[i] = Integer.parseInt((String)st.nextElement());
				}	
			}
			catch(Exception e)
			{
				System.err.println(e);
				System.exit(0);
			}
		}
		nocConfig.numberOfBuffers = Integer.parseInt(getImmediateString("NocNumberOfBuffers", NocType));
		nocConfig.portType = setPortType(getImmediateString("NocPortType", NocType));
		nocConfig.accessPorts = Integer.parseInt(getImmediateString("NocAccessPorts", NocType));
		nocConfig.portOccupancy = Integer.parseInt(getImmediateString("NocPortOccupancy", NocType));
		nocConfig.latency = Integer.parseInt(getImmediateString("NocLatency", NocType));
		nocConfig.operatingFreq = Integer.parseInt(getImmediateString("NocOperatingFreq", NocType));
		nocConfig.latencyBetweenBanks = Integer.parseInt(getImmediateString("NocLatencyBetweenBanks", NocType));
		
		String tempStr = getImmediateString("NucaMapping", NocType);
		if (tempStr.equalsIgnoreCase("S"))
			nocConfig.mapping = Mapping.SET_ASSOCIATIVE;
		else if (tempStr.equalsIgnoreCase("A"))
			nocConfig.mapping = Mapping.ADDRESS;
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
		branchPredictor.saturating_bits = Integer.parseInt(getImmediateString("SaturatingBits", predictorElmnt));
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
