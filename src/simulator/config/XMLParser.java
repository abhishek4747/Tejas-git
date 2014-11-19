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

import generic.PortType;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import main.Emulator;
import main.Main;
import memorysystem.nuca.NucaCache.Mapping;
import memorysystem.nuca.NucaCache.NucaType;
import net.NOC;
import net.NOC.CONNECTIONTYPE;
import net.NOC.TOPOLOGY;
import net.RoutingAlgo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import config.BranchPredictorConfig.BP;

//<Cache name="iCache" nextLevel="L2" nextLevelId="$i/4" firstLevel="true" type="ICache_32K_4"/>
//<Cache name="l1Cache" nextLevel="L2" nextLevelId="$i/4" firstLevel="true" type="L1Cache_32K_4"/>
//<Cache name="L2" numComponents="2" nextLevel="L3" type="L2Cache_1M_8"/>

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
			
			createSharedCacheConfigs();
			setSimulationParameters();
			setEmulatorParameters();
			
			setSystemParameters();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			misc.Error.showErrorAndExit("Error in reading config file : " + e);
		}
 	}
	
	private static void createSharedCacheConfigs() throws Exception {
		Element sharedCachesNode = (Element)doc.getElementsByTagName("SharedCaches").item(0);
		
		NodeList nodeLst = sharedCachesNode.getElementsByTagName("Cache");
		if (nodeLst.item(0) == null) {
			System.out.println("Shared caches not found !!");
		}
		
		for(int i=0; i<nodeLst.getLength(); i++) {
			Element cacheNode = (Element)nodeLst.item(i);			
			CacheConfig config = createCacheConfig(cacheNode);
			SystemConfig.sharedCacheConfigs.add(config);
		}
	}
	
	private static CacheConfig createCacheConfig(Element cacheNode) {
		CacheConfig config = new CacheConfig();
		
		config.cacheName = cacheNode.getAttribute("name");
		
		if(isAttributePresent(cacheNode, "firstLevel")) {
			config.firstLevel = 
				Boolean.parseBoolean(cacheNode.getAttribute("firstLevel"));
		} else {
			config.firstLevel = false;
		}
		
		if(isAttributePresent(cacheNode, "numComponents")) {
			config.numComponents = 
				Integer.parseInt(cacheNode.getAttribute("numComponents"));
		} else {
			config.numComponents = 1;
		}
		
		config.nextLevel=cacheNode.getAttribute("nextLevel");
		if(isAttributePresent(cacheNode, "nextLevelId")) {
			config.nextLevelId = cacheNode.getAttribute("nextLevelId"); 
		}
	
		String cacheType = cacheNode.getAttribute("type");
		
		Element cacheTypeElmnt = searchLibraryForItem(cacheType);
		setCacheProperties(cacheTypeElmnt, config);
		
		return config;
	}
	
	private static boolean isAttributePresent(Element element, String str) {
		return (element.getAttribute(str)!="");
	}
	
	private static boolean isElementPresent(String tagName, Element parent) // Get the immediate string value of a particular tag name under a particular parent tag
	{
		NodeList nodeLst = parent.getElementsByTagName(tagName);
		if (nodeLst.item(0) == null) {
			return false;
		} else {
			return true;
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
	
	static EmulatorType getEmulatorType(String emulatorType) {
		EmulatorType t = null;
		
		try {
			t = EmulatorType.valueOf(emulatorType);
		} catch (Exception e) {
			misc.Error.showErrorAndExit("Error in setting the emulator type argument." +
					"\nExpected values : pin, or qemu");
		}
		
		return t;
	}
	
	static CommunicationType getCommunicationType(String communicationType) {
		CommunicationType t = null;
		
		try {
			t = CommunicationType.valueOf(communicationType);
		} catch (Exception e) {
			misc.Error.showErrorAndExit("Error in setting the communication type argument." +
					"\nExpected values : sharedMemory, network, or file");
		}
		
		return t;
	}
	
	private static void setEmulatorParameters() {
		NodeList nodeLst = doc.getElementsByTagName("Emulator");
		Node emulatorNode = nodeLst.item(0);
		Element emulatorElmnt = (Element) emulatorNode;
		
		EmulatorConfig.emulatorType = getEmulatorType(getImmediateString("EmulatorType", emulatorElmnt));
		EmulatorConfig.communicationType = getCommunicationType(getImmediateString("CommunicationType", emulatorElmnt));
				
		EmulatorConfig.PinTool = getImmediateString("PinTool", emulatorElmnt);
		EmulatorConfig.PinInstrumentor = getImmediateString("PinInstrumentor", emulatorElmnt);
		EmulatorConfig.QemuTool = getImmediateString("QemuTool", emulatorElmnt);
		EmulatorConfig.ShmLibDirectory = getImmediateString("ShmLibDirectory", emulatorElmnt);
		EmulatorConfig.KillEmulatorScript = getImmediateString("KillEmulatorScript", emulatorElmnt);
		
		EmulatorConfig.storeExecutionTraceInAFile = Boolean.parseBoolean(getImmediateString("StoreExecutionTraceInAFile", emulatorElmnt));
		EmulatorConfig.basenameForTraceFiles = getImmediateString("BasenameForTraceFiles", emulatorElmnt);
		
		if(EmulatorConfig.storeExecutionTraceInAFile==true) {
			runEmulatorForTracing();
			System.exit(0);
		}
	}
	
	static void checkIfTraceFileAlreadyExists() {
		// Check if a trace file was already present
		for(int i=0; i<EmulatorConfig.maxThreadsForTraceCollection; i++) {
			String fileName = EmulatorConfig.basenameForTraceFiles + "_" + i + ".gz";
			
			File f = new File(fileName);
			if(f!=null && f.exists()) {
				misc.Error.showErrorAndExit("Trace file already present : " + fileName + " !!" + 
					"\nKindly rename the trace file and start collecting trace again.");
			}
		}
	}
	
	private static void runEmulatorForTracing() {
		// Strict condition : Emulator is pin, and communication type is file.
		if(EmulatorConfig.emulatorType==EmulatorType.pin && 
			EmulatorConfig.communicationType==CommunicationType.file) {
			
		} else {
			misc.Error.showErrorAndExit("Invalid emulator/communication-type combination !!");
		}
		
		checkIfTraceFileAlreadyExists();
		
		Emulator emulator = new Emulator(EmulatorConfig.PinTool, EmulatorConfig.PinInstrumentor, 
			Main.getBenchmarkArguments(), EmulatorConfig.basenameForTraceFiles);
		
		emulator.waitForEmulator();
		
		long endTime = System.currentTimeMillis();
		float timeElapsedInMinutes = (float)(endTime-Main.getStartTime())/(1000.0f*60.0f);
		
		System.out.println("Completed trace collection successfully in " + timeElapsedInMinutes + " minutes.");
	}

	private static void setSimulationParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("Simulation");
		Node simulationNode = nodeLst.item(0);
		Element simulationElmnt = (Element) simulationNode;
		SimulationConfig.NumTempIntReg = Integer.parseInt(getImmediateString("NumTempIntReg", simulationElmnt));
		SimulationConfig.NumInsToIgnore = Long.parseLong(getImmediateString("NumInsToIgnore", simulationElmnt));
		
		SimulationConfig.collectInsnWorkingSetInfo = 
				Boolean.parseBoolean(getImmediateString("CollectInsnWorkingSet", simulationElmnt));
		
		SimulationConfig.insnWorkingSetChunkSize = 
				Long.parseLong(getImmediateString("InsnWorkingSetChunkSize", simulationElmnt));
		
		SimulationConfig.collectDataWorkingSetInfo = 
				Boolean.parseBoolean(getImmediateString("CollectDataWorkingSet", simulationElmnt));
		
		SimulationConfig.dataWorkingSetChunkSize = 
				Long.parseLong(getImmediateString("DataWorkingSetChunkSize", simulationElmnt));
		
		//Read number of cores and define the array of core configurations
		//Note that number of Cores specified in config.xml is deprecated and is instead done as follows
		SystemConfig.maxNumJavaThreads = 1;
		SystemConfig.numEmuThreadsPerJavaThread = Integer.parseInt(getImmediateString("NumCores", simulationElmnt));
		SystemConfig.NoOfCores = SystemConfig.maxNumJavaThreads*SystemConfig.numEmuThreadsPerJavaThread;
		
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
		
		SimulationConfig.detachMemSysData = Boolean.parseBoolean(getImmediateString("DetachMemSysData", simulationElmnt));
		SimulationConfig.detachMemSysInsn = Boolean.parseBoolean(getImmediateString("DetachMemSysInsn", simulationElmnt));
		
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
		
		if(getImmediateString("markerFunctions", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("markerFunctions", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.markerFunctionsSimulation = true;
			SimulationConfig.startMarker = getImmediateString("startSimMarker", simulationElmnt);
			SimulationConfig.endMarker = getImmediateString("endSimMarker", simulationElmnt);
		}
		else
		{
			SimulationConfig.markerFunctionsSimulation = false;
			SimulationConfig.startMarker = "";
			SimulationConfig.endMarker = "";
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
	}
	
	private static EnergyConfig getEnergyConfig(Element parent)
	{
		double leakageEnergy = Double.parseDouble(getImmediateString("LeakageEnergy", parent));
		double dynamicEnergy = Double.parseDouble(getImmediateString("DynamicEnergy", parent));
		
		EnergyConfig energyConfig = new EnergyConfig(leakageEnergy, dynamicEnergy);
		return energyConfig;
	}
	
	private static CacheEnergyConfig getCacheEnergyConfig(Element parent)
	{
		CacheEnergyConfig powerConfig = new CacheEnergyConfig();
		powerConfig.leakageEnergy = Double.parseDouble(getImmediateString("LeakageEnergy", parent));
		powerConfig.readDynamicEnergy = Double.parseDouble(getImmediateString("ReadDynamicEnergy", parent));
		powerConfig.writeDynamicEnergy = Double.parseDouble(getImmediateString("WriteDynamicEnergy", parent));
		return powerConfig;
	}
	
	private static void setSystemParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("System");
		Node systemNode = nodeLst.item(0);
		Element systemElmnt = (Element) systemNode;
		
		SystemConfig.mainMemoryLatency = Integer.parseInt(getImmediateString("MainMemoryLatency", systemElmnt));
		SystemConfig.mainMemoryFrequency = Long.parseLong(getImmediateString("MainMemoryFrequency", systemElmnt));
		SystemConfig.mainMemPortType = setPortType(getImmediateString("MainMemoryPortType", systemElmnt));
		SystemConfig.mainMemoryAccessPorts = Integer.parseInt(getImmediateString("MainMemoryAccessPorts", systemElmnt));
		SystemConfig.mainMemoryPortOccupancy = Integer.parseInt(getImmediateString("MainMemoryPortOccupancy", systemElmnt));
		
		Element mainMemElmnt = (Element)(systemElmnt.getElementsByTagName("MainMemory")).item(0);
		SystemConfig.mainMemoryControllerPower = getEnergyConfig(mainMemElmnt);
		
		Element globalClockElmnt = (Element)(systemElmnt.getElementsByTagName("GlobalClock")).item(0);
		SystemConfig.globalClockPower = getEnergyConfig(globalClockElmnt);
		
		SystemConfig.cacheBusLatency = Integer.parseInt(getImmediateString("CacheBusLatency", systemElmnt));

		SystemConfig.core = new CoreConfig[SystemConfig.NoOfCores];
		
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
			core.lsqPower = getEnergyConfig(lsqElmnt);
			
			Element iTLBElmnt = (Element)(coreElmnt.getElementsByTagName("ITLB")).item(0);
			core.ITLBSize = Integer.parseInt(getImmediateString("Size", iTLBElmnt));
			core.ITLBLatency = Integer.parseInt(getImmediateString("Latency", iTLBElmnt));
			core.ITLBMissPenalty = Integer.parseInt(getImmediateString("MissPenalty", iTLBElmnt));
			core.ITLBPortType = setPortType(getImmediateString("PortType", iTLBElmnt));
			core.ITLBAccessPorts = Integer.parseInt(getImmediateString("AccessPorts", iTLBElmnt));
			core.ITLBPortOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", iTLBElmnt));
			core.iTLBPower = getEnergyConfig(iTLBElmnt);
			
			Element dTLBElmnt = (Element)(coreElmnt.getElementsByTagName("DTLB")).item(0);
			core.DTLBSize = Integer.parseInt(getImmediateString("Size", dTLBElmnt));
			core.DTLBLatency = Integer.parseInt(getImmediateString("Latency", dTLBElmnt));
			core.DTLBMissPenalty = Integer.parseInt(getImmediateString("MissPenalty", dTLBElmnt));
			core.DTLBPortType = setPortType(getImmediateString("PortType", dTLBElmnt));
			core.DTLBAccessPorts = Integer.parseInt(getImmediateString("AccessPorts", dTLBElmnt));
			core.DTLBPortOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", dTLBElmnt));
			core.dTLBPower = getEnergyConfig(dTLBElmnt);

			Element decodeElmnt = (Element)(coreElmnt.getElementsByTagName("Decode")).item(0);
			core.DecodeWidth = Integer.parseInt(getImmediateString("Width", decodeElmnt));
			core.decodePower = getEnergyConfig(decodeElmnt);
			
			Element instructionWindowElmnt = (Element)(coreElmnt.getElementsByTagName("InstructionWindow")).item(0);
			core.IssueWidth = Integer.parseInt(getImmediateString("IssueWidth", instructionWindowElmnt));			
			core.IWSize = Integer.parseInt(getImmediateString("IWSize", instructionWindowElmnt));
			core.iwPower = getEnergyConfig(instructionWindowElmnt);

			Element robElmnt = (Element)(coreElmnt.getElementsByTagName("ROB")).item(0);
			core.RetireWidth = Integer.parseInt(getImmediateString("RetireWidth", coreElmnt));
			core.ROBSize = Integer.parseInt(getImmediateString("ROBSize", coreElmnt));
			core.robPower = getEnergyConfig(robElmnt);
			
			Element resultsBroadcastBusElmnt = (Element)(coreElmnt.getElementsByTagName("ResultsBroadcastBus")).item(0);
			core.resultsBroadcastBusPower = getEnergyConfig(resultsBroadcastBusElmnt);
			
			Element renameElmnt = (Element)(coreElmnt.getElementsByTagName("Rename")).item(0);
			
			Element ratElmnt = (Element)(renameElmnt.getElementsByTagName("RAT")).item(0);
			core.intRATPower = getEnergyConfig((Element)ratElmnt.getElementsByTagName("Integer").item(0));
			core.floatRATPower = getEnergyConfig((Element)ratElmnt.getElementsByTagName("Float").item(0));
			
			Element freelistElmnt = (Element)(renameElmnt.getElementsByTagName("FreeList")).item(0);
			core.intFreeListPower = getEnergyConfig((Element)freelistElmnt.getElementsByTagName("Integer").item(0));
			core.floatFreeListPower = getEnergyConfig((Element)freelistElmnt.getElementsByTagName("Float").item(0));			
			
			Element registerFileElmnt = (Element)(coreElmnt.getElementsByTagName("RegisterFile")).item(0);
			
			Element integerRegisterFileElmnt = (Element)(registerFileElmnt.getElementsByTagName("Integer")).item(0);
			core.IntRegFileSize = Integer.parseInt(getImmediateString("IntRegFileSize", integerRegisterFileElmnt));
			core.IntArchRegNum = Integer.parseInt(getImmediateString("IntArchRegNum", integerRegisterFileElmnt));
			core.intRegFilePower = getEnergyConfig(integerRegisterFileElmnt);
			
			Element floatRegisterFileElmnt = (Element)(registerFileElmnt.getElementsByTagName("Float")).item(0);
			core.FloatRegFileSize = Integer.parseInt(getImmediateString("FloatRegFileSize", floatRegisterFileElmnt));
			core.FloatArchRegNum = Integer.parseInt(getImmediateString("FloatArchRegNum", floatRegisterFileElmnt));
			core.floatRegFilePower = getEnergyConfig(floatRegisterFileElmnt);
			
			core.ExecutionCoreNumPorts = Integer.parseInt(coreElmnt.getElementsByTagName("ExecutionCoreNumPorts").item(0).getFirstChild().getNodeValue());
			
			Element intALUElmnt = (Element)(coreElmnt.getElementsByTagName("IntALU")).item(0);
			core.IntALUNum = Integer.parseInt(getImmediateString("Num", intALUElmnt));
			core.IntALULatency = Integer.parseInt(getImmediateString("Latency", intALUElmnt));
			core.IntALUReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", intALUElmnt));
			core.IntALUPortNumbers = new int[core.IntALUNum];
			for(int j = 0; j < core.IntALUNum; j++)
			{
				core.IntALUPortNumbers[j] = Integer.parseInt(intALUElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element intMulElmnt = (Element)(coreElmnt.getElementsByTagName("IntMul")).item(0);
			core.IntMulNum = Integer.parseInt(getImmediateString("Num", intMulElmnt));
			core.IntMulLatency = Integer.parseInt(getImmediateString("Latency", intMulElmnt));
			core.IntMulReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", intMulElmnt));
			core.IntMulPortNumbers = new int[core.IntMulNum];
			for(int j = 0; j < core.IntMulNum; j++)
			{
				core.IntMulPortNumbers[j] = Integer.parseInt(intMulElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element intDivElmnt = (Element)(coreElmnt.getElementsByTagName("IntDiv")).item(0);
			core.IntDivNum = Integer.parseInt(getImmediateString("Num", intDivElmnt));
			core.IntDivLatency = Integer.parseInt(getImmediateString("Latency", intDivElmnt));
			core.IntDivReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", intDivElmnt));
			core.IntDivPortNumbers = new int[core.IntDivNum];
			for(int j = 0; j < core.IntDivNum; j++)
			{
				core.IntDivPortNumbers[j] = Integer.parseInt(intDivElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element floatALUElmnt = (Element)(coreElmnt.getElementsByTagName("FloatALU")).item(0);
			core.FloatALUNum = Integer.parseInt(getImmediateString("Num", floatALUElmnt));
			core.FloatALULatency = Integer.parseInt(getImmediateString("Latency", floatALUElmnt));
			core.FloatALUReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", floatALUElmnt));
			core.FloatALUPortNumbers = new int[core.FloatALUNum];
			for(int j = 0; j < core.FloatALUNum; j++)
			{
				core.FloatALUPortNumbers[j] = Integer.parseInt(floatALUElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element floatMulElmnt = (Element)(coreElmnt.getElementsByTagName("FloatMul")).item(0);
			core.FloatMulNum = Integer.parseInt(getImmediateString("Num", floatMulElmnt));
			core.FloatMulLatency = Integer.parseInt(getImmediateString("Latency", floatMulElmnt));
			core.FloatMulReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", floatMulElmnt));
			core.FloatMulPortNumbers = new int[core.FloatMulNum];
			for(int j = 0; j < core.FloatMulNum; j++)
			{
				core.FloatMulPortNumbers[j] = Integer.parseInt(floatMulElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element floatDivElmnt = (Element)(coreElmnt.getElementsByTagName("FloatDiv")).item(0);
			core.FloatDivNum = Integer.parseInt(getImmediateString("Num", floatDivElmnt));
			core.FloatDivLatency = Integer.parseInt(getImmediateString("Latency", floatDivElmnt));
			core.FloatDivReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", floatDivElmnt));
			core.FloatDivPortNumbers = new int[core.FloatDivNum];
			for(int j = 0; j < core.FloatDivNum; j++)
			{
				core.FloatDivPortNumbers[j] = Integer.parseInt(floatDivElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element jumpElmnt = (Element)(coreElmnt.getElementsByTagName("Jump")).item(0);
			core.JumpNum = Integer.parseInt(getImmediateString("Num", jumpElmnt));
			core.JumpLatency = Integer.parseInt(getImmediateString("Latency", jumpElmnt));
			core.JumpReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", jumpElmnt));
			core.JumpPortNumbers = new int[core.JumpNum];
			for(int j = 0; j < core.JumpNum; j++)
			{
				core.JumpPortNumbers[j] = Integer.parseInt(jumpElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			Element memoryElmnt = (Element)(coreElmnt.getElementsByTagName("Memory")).item(0);
			core.MemoryNum = Integer.parseInt(getImmediateString("Num", memoryElmnt));
			core.MemoryLatency = Integer.parseInt(getImmediateString("Latency", memoryElmnt));
			core.MemoryReciprocalOfThroughput = Integer.parseInt(getImmediateString("ReciprocalOfThroughput", memoryElmnt));
			core.MemoryPortNumbers = new int[core.MemoryNum];
			for(int j = 0; j < core.MemoryNum; j++)
			{
				core.MemoryPortNumbers[j] = Integer.parseInt(memoryElmnt.getElementsByTagName("PortNumber").item(j).getFirstChild().getNodeValue());
			}
			
			core.intALUPower = getEnergyConfig(intALUElmnt);
			core.floatALUPower = getEnergyConfig(floatALUElmnt);
			core.complexALUPower = getEnergyConfig(intMulElmnt);
						
			//set Branch Predictor Parameters
			core.branchPredictor = new BranchPredictorConfig();
			Element predictorElmnt = (Element)(coreElmnt.getElementsByTagName("BranchPredictor").item(0));
			Element BTBElmnt = (Element) predictorElmnt.getElementsByTagName("BTB").item(0);
			setBranchPredictorProperties(predictorElmnt, BTBElmnt, core.branchPredictor);
			core.BranchMispredPenalty = Integer.parseInt(getImmediateString("BranchMispredPenalty", predictorElmnt));
			core.bPredPower = getEnergyConfig(predictorElmnt);
			
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
			String interconnect = getImmediateString("Interconnect",systemElmnt); 
			if(interconnect.equalsIgnoreCase("Bus"))
			{
				SystemConfig.interconnect = SystemConfig.Interconnect.Bus;
			}
			else if(interconnect.equalsIgnoreCase("Noc"))
			{
				SystemConfig.interconnect = SystemConfig.Interconnect.Noc;
			}
			else
			{
				System.err.println("XML Configuration error : Invalid Interconnect Type");
				System.exit(1);
			}
			
			
			NodeList coreCacheList = coreElmnt.getElementsByTagName("Cache");
			if (coreCacheList.item(0) == null) {
				System.out.println("No core cache not found !!");
			} else {
				for(int coreCacheIndex=0; coreCacheIndex<coreCacheList.getLength(); coreCacheIndex++) {
					Element cacheNode = (Element)coreCacheList.item(coreCacheIndex);
					CacheConfig config = createCacheConfig(cacheNode);
					core.coreCacheList.add(config);
					
					// icache config
					if(SimulationConfig.collectInsnWorkingSetInfo &&
						config.firstLevel==true && 
						config.cacheDataType==CacheDataType.Instruction)
					{
						config.collectWorkingSetData = true;
						config.workingSetChunkSize = SimulationConfig.insnWorkingSetChunkSize; 
					}
					
					// l1cache config
					if(SimulationConfig.collectInsnWorkingSetInfo &&
						config.firstLevel==true && 
						config.cacheDataType==CacheDataType.Data)
					{
						config.collectWorkingSetData = true;
						config.workingSetChunkSize = SimulationConfig.dataWorkingSetChunkSize; 
					}
				}
			}
		}
		
		//set NOC Parameters
		SystemConfig.nocConfig = new NocConfig();
		NodeList NocLst = systemElmnt.getElementsByTagName("NOC");
		Element nocElmnt = (Element) NocLst.item(0);
		SystemConfig.nocConfig.power = getEnergyConfig(nocElmnt);
		setNocProperties(nocElmnt, SystemConfig.nocConfig);

		//set Bus Parameters
		NodeList busLst = systemElmnt.getElementsByTagName("BUS");
		Element busElmnt = (Element) busLst.item(0);
		SystemConfig.busEnergy = getEnergyConfig(busElmnt);
		SystemConfig.busConfig = new BusConfig();
		SystemConfig.busConfig.setLatency(Integer.parseInt(getImmediateString("Latency", busElmnt)));
	}

	private static void setNocProperties(Element NocType, NocConfig nocConfig)
	{
		if(SystemConfig.interconnect==SystemConfig.Interconnect.Noc) {
			String nocConfigFilename = getImmediateString("NocConfigFile", NocType);
			nocConfig.NocTopologyFile = nocConfigFilename;
		}
		
		nocConfig.numberOfBuffers = Integer.parseInt(getImmediateString("NocNumberOfBuffers", NocType));
		nocConfig.portType = setPortType(getImmediateString("NocPortType", NocType));
		nocConfig.accessPorts = Integer.parseInt(getImmediateString("NocAccessPorts", NocType));
		nocConfig.portOccupancy = Integer.parseInt(getImmediateString("NocPortOccupancy", NocType));
		nocConfig.latency = Integer.parseInt(getImmediateString("NocLatency", NocType));
		nocConfig.operatingFreq = Integer.parseInt(getImmediateString("NocOperatingFreq", NocType));
		nocConfig.latencyBetweenNOCElements = Integer.parseInt(getImmediateString("NocLatencyBetweenNOCElements", NocType));
		
		String tempStr = getImmediateString("NocTopology", NocType);
		nocConfig.topology = TOPOLOGY.valueOf(tempStr);
		
		tempStr = getImmediateString("NocRoutingAlgorithm", NocType);
		nocConfig.rAlgo = RoutingAlgo.ALGO.valueOf(tempStr);
				
		tempStr = getImmediateString("NocSelScheme", NocType);
		nocConfig.selScheme = RoutingAlgo.SELSCHEME.valueOf(tempStr);
		
		tempStr = getImmediateString("NocRouterArbiter", NocType);
		nocConfig.arbiterType = RoutingAlgo.ARBITER.valueOf(tempStr);
				
		nocConfig.technologyPoint = Integer.parseInt(getImmediateString("TechPoint", NocType));
		
		tempStr = getImmediateString("NocConnection", NocType);
		nocConfig.ConnType = CONNECTIONTYPE.valueOf(tempStr);
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
		
		if(isElementPresent("Size", CacheType)) {
			cache.size = Integer.parseInt(getImmediateString("Size", CacheType));
		} else {
			cache.size = 0;
		}
		
		NodeList nodeLst = CacheType.getElementsByTagName("IsDirectory");
		if (nodeLst.item(0) == null) {
			cache.isDirectory = false;
		} else {
			cache.isDirectory = Boolean.parseBoolean(getImmediateString("IsDirectory", CacheType));
		}
		
		if(isElementPresent("NumEntries", CacheType)) {
			cache.numEntries = Integer.parseInt(getImmediateString("NumEntries", CacheType));
			cache.size = cache.numEntries*cache.blockSize;
		} else {
			cache.numEntries = 0;
		}
		
		if(cache.size==0 && cache.numEntries==0) {
			misc.Error.showErrorAndExit("Invalid cache configuration : size=0 and numEntries=0 !!");
		}
		
		cache.latency = Integer.parseInt(getImmediateString("Latency", CacheType));
		cache.portType = setPortType(getImmediateString("PortType", CacheType));
		cache.accessPorts = Integer.parseInt(getImmediateString("AccessPorts", CacheType));
		cache.portOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", CacheType));
		
		cache.mshrSize = Integer.parseInt(getImmediateString("MSHRSize", CacheType));
				
		cache.coherenceName = getImmediateString("Coherence", CacheType);
		
		cache.numberOfBuses = Integer.parseInt(getImmediateString("NumBuses", CacheType));
		cache.busOccupancy = Integer.parseInt(getImmediateString("BusOccupancy", CacheType));
		
		tempStr = getImmediateString("Nuca", CacheType);
		cache.nucaType = NucaType.valueOf(tempStr); // TODO : We are not using NUCA right now.
		
		tempStr = getImmediateString("NucaMapping", CacheType);
		if (tempStr.equalsIgnoreCase("S"))
			cache.mapping = Mapping.SET_ASSOCIATIVE;
		else if (tempStr.equalsIgnoreCase("A"))
			cache.mapping = Mapping.ADDRESS;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'NucaMapping' (please enter 'S'or 'A')");
			System.exit(1);
		}
		
		cache.cacheDataType = CacheDataType.valueOf(getImmediateString("CacheType", CacheType));
		
		cache.power = getCacheEnergyConfig(CacheType);
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
		else if(tempStr.equalsIgnoreCase("TAGE"))
		{
                        branchPredictor.predictorMode = BP.TAGE;
		}
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
