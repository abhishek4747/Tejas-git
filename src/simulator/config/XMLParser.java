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
import memorysystem.Cache;
import memorysystem.Cache.CacheType;
import memorysystem.Cache.CoherenceType;

import org.w3c.dom.*;

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
			
			setSimulationParameters();
			
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
	
	private static void setSimulationParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("Simulation");
		Node simulationNode = nodeLst.item(0);
		Element simulationElmnt = (Element) simulationNode;
		SimulationConfig.PinTool = getImmediateString("PinTool", simulationElmnt);
		SimulationConfig.PinInstrumentor = getImmediateString("PinInstrumentor", simulationElmnt);
		SimulationConfig.Mode = Integer.parseInt(getImmediateString("Mode", simulationElmnt));
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
		
		if(getImmediateString("StatisticalPipeline", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("StatisticalPipeline", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.isPipelineStatistical = true;
		}
		else 
		{
			SimulationConfig.isPipelineStatistical = false;
		}
		
		if(getImmediateString("InorderPipeline", simulationElmnt).compareTo("true") == 0 ||
				getImmediateString("InorderPipeline", simulationElmnt).compareTo("True") == 0)
		{
			SimulationConfig.isPipelineInorder = true;
		}
		else 
		{
			SimulationConfig.isPipelineInorder = false;
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

	}
	
	private static void setSystemParameters()
	{
		SystemConfig.declaredCaches = new Hashtable<String, CacheConfig>(); //Declare the hash table for declared caches
		
		NodeList nodeLst = doc.getElementsByTagName("System");
		Node systemNode = nodeLst.item(0);
		Element systemElmnt = (Element) systemNode;
		
		//Read number of cores and define the array of core configurations
		//Note that number of Cores specified in config.xml is deprecated and is instead done as follows
		SystemConfig.NoOfCores = IpcBase.MaxNumJavaThreads*IpcBase.EmuThreadsPerJavaThread;
		SystemConfig.mainMemoryLatency = Integer.parseInt(getImmediateString("MainMemoryLatency", systemElmnt));
		SystemConfig.mainMemoryFrequency = Long.parseLong(getImmediateString("MainMemoryFrequency", systemElmnt));
		SystemConfig.mainMemPortType = setPortType(getImmediateString("MainMemoryPortType", systemElmnt));
		SystemConfig.mainMemoryAccessPorts = Integer.parseInt(getImmediateString("MainMemoryAccessPorts", systemElmnt));
		SystemConfig.mainMemoryPortOccupancy = Integer.parseInt(getImmediateString("MainMemoryPortOccupancy", systemElmnt));
		SystemConfig.cacheBusLatency = Integer.parseInt(getImmediateString("CacheBusLatency", systemElmnt));
		//SystemConfig.core = new CoreConfig[SystemConfig.NoOfCores];
		StringTokenizer coreNucaMapping = new StringTokenizer((getImmediateString("NearestBankToCores", systemElmnt)));
		SystemConfig.coreCacheMapping = new int[SystemConfig.NoOfCores][2];
		
		for(int i=0;coreNucaMapping.hasMoreTokens();i++)
		{
			StringTokenizer tempTok = new StringTokenizer(coreNucaMapping.nextToken(),",");
			for(int j=0;tempTok.hasMoreTokens();j++)
			{
				SystemConfig.coreCacheMapping[i][j] = Integer.parseInt(tempTok.nextToken());
			}
		}
		/*for(int i=0;i<numOfCores;i++)
		{
			for(int j=0;j<2;j++)
			System.out.print("\t"+ SystemConfig.coreNucaMapping[i][j]);
			System.out.println();
		}*/

		SystemConfig.core = new CoreConfig[SystemConfig.NoOfCores];
		SystemConfig.directoryAccessLatency = Integer.parseInt(getImmediateString("directoryAccessLatency", systemElmnt));
		SystemConfig.memWBDelay = Integer.parseInt(getImmediateString("memWBDelay", systemElmnt));
		SystemConfig.dataTransferDelay = Integer.parseInt(getImmediateString("dataTransferDelay", systemElmnt));
		SystemConfig.invalidationSendDelay = Integer.parseInt(getImmediateString("invalidationSendDelay", systemElmnt));
		SystemConfig.invalidationAckCollectDelay = Integer.parseInt(getImmediateString("invalidationAckCollectDelay", systemElmnt));
		SystemConfig.ownershipChangeDelay = Integer.parseInt(getImmediateString("ownershipChangeDelay", systemElmnt));
		
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
		
		//System.out.println(SystemConfig.NoOfCores + ", " + SystemConfig.core[0].ROBSize);
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
		cache.numberOfBankColumns = Integer.parseInt(getImmediateString("NumberOfBankColumns", CacheType));
		cache.numberOfBankRows = Integer.parseInt(getImmediateString("NumberOfBankRows", CacheType));		
		cache.nocConfig.numberOfBuffers = Integer.parseInt(getImmediateString("NocNumberOfBuffers", CacheType));
		cache.nocConfig.portType = setPortType(getImmediateString("NocPortType", CacheType));
		cache.nocConfig.accessPorts = Integer.parseInt(getImmediateString("NocAccessPorts", CacheType));
		cache.nocConfig.portOccupancy = Integer.parseInt(getImmediateString("NocPortOccupancy", CacheType));
		cache.nocConfig.latency = Integer.parseInt(getImmediateString("NocLatency", CacheType));
		cache.nocConfig.operatingFreq = Integer.parseInt(getImmediateString("NocOperatingFreq", CacheType));
		cache.nocConfig.numberOfRows = cache.numberOfBankRows;
		cache.nocConfig.numberOfColumns = cache.numberOfBankColumns;
		cache.nocConfig.latencyBetweenBanks = Integer.parseInt(getImmediateString("NocLatencyBetweenBanks", CacheType));

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
		if (tempStr.equalsIgnoreCase("N"))
			cache.nucaType = NucaType.NONE;
		else if (tempStr.equalsIgnoreCase("S"))
			cache.nucaType = NucaType.S_NUCA;
		else if (tempStr.equalsIgnoreCase("D"))
			cache.nucaType = NucaType.D_NUCA;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'Nuca' (please enter 'S', D' or 'N')");
			System.exit(1);
		}
		
		tempStr = getImmediateString("NucaMapping", CacheType);
		if (tempStr.equalsIgnoreCase("S"))
			cache.mapping = Mapping.SET_ASSOCIATIVE;
		else if (tempStr.equalsIgnoreCase("A"))
			cache.mapping = Mapping.ADDRESS;
		else if (tempStr.equalsIgnoreCase("B"))
			cache.mapping = Mapping.BOTH;
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
		tempStr = getImmediateString("NocTopology", CacheType);
		if(tempStr.equalsIgnoreCase("MESH"))
			cache.nocConfig.topology = NOC.TOPOLOGY.MESH;
		else if(tempStr.equalsIgnoreCase("TORUS"))
			cache.nocConfig.topology = NOC.TOPOLOGY.TORUS;
		else if(tempStr.equalsIgnoreCase("BUS"))
			cache.nocConfig.topology = NOC.TOPOLOGY.BUS;
		else if(tempStr.equalsIgnoreCase("RING"))
			cache.nocConfig.topology = NOC.TOPOLOGY.RING;
		else if(tempStr.equalsIgnoreCase("FATTREE"))
			cache.nocConfig.topology = NOC.TOPOLOGY.FATTREE;
		else if(tempStr.equalsIgnoreCase("OMEGA"))
			cache.nocConfig.topology = NOC.TOPOLOGY.OMEGA;
		else if(tempStr.equalsIgnoreCase("BUTTERFLY"))
			cache.nocConfig.topology = NOC.TOPOLOGY.BUTTERFLY;
		
		tempStr = getImmediateString("NocRoutingAlgorithm", CacheType);
		if(tempStr.equalsIgnoreCase("SIMPLE"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.SIMPLE;
		else if(tempStr.equalsIgnoreCase("WESTFIRST"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.WESTFIRST;
		else if(tempStr.equalsIgnoreCase("NORTHLAST"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.NORTHLAST;
		else if(tempStr.equalsIgnoreCase("NEGATIVEFIRST"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.NEGATIVEFIRST;
		else if(tempStr.equalsIgnoreCase("FATTREE"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.FATTREE;
		else if(tempStr.equalsIgnoreCase("OMEGA"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.OMEGA;
		else if(tempStr.equalsIgnoreCase("BUTTERFLY"))
			cache.nocConfig.rAlgo = RoutingAlgo.ALGO.BUTTERFLY;

		tempStr = getImmediateString("NocSelScheme", CacheType);
		if(tempStr.equalsIgnoreCase("STATIC"))
			cache.nocConfig.selScheme = RoutingAlgo.SELSCHEME.STATIC;
		else
			cache.nocConfig.selScheme = RoutingAlgo.SELSCHEME.ADAPTIVE;
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
