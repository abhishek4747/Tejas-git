package emulatorinterface.communication.filePacket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import main.CustomObjectPool;

import config.EmulatorConfig;
import config.SimulationConfig;
import config.SystemConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import generic.CircularPacketQueue;

public class FilePacket extends IpcBase implements Encoding {

	BufferedReader inputBufferedReader[];
	int maxApplicationThreads = -1;
	long totalFetchedAssemblyPackets = 0;
	
	public FilePacket() {
		this.maxApplicationThreads = IpcBase.MaxNumJavaThreads*IpcBase.EmuThreadsPerJavaThread;
		
		inputBufferedReader = new BufferedReader[maxApplicationThreads];
		
		for (int i=0; i<maxApplicationThreads; i++) {
			String inputFileName = SimulationConfig.InstructionsFilename + "_" + i;
			try {
				inputBufferedReader[i] = new BufferedReader(
					new FileReader(	new File(inputFileName)));
			} catch (FileNotFoundException e) {
				if(i==0) {
					// not able to find first file is surely an error.
					misc.Error.showErrorAndExit("Error in reading input packet file " + inputFileName);
				} else {
					System.out.println("FilePacket : no trace file found for tidApp = " + i);
					continue;
				}
			}
		}
	}

	public void initIpc() {
		// this does nothing
	}

	public int fetchManyPackets(int tidApp, CircularPacketQueue fromEmulator) {
		
		if(tidApp>=maxApplicationThreads) {
			misc.Error.showErrorAndExit("FilePacket cannot handle tid = " + tidApp);
		}
		
		if(inputBufferedReader[tidApp]==null) {
			return 0;
		}
		
		int maxSize = fromEmulator.spaceLeft();
		
		for(int i=0; i<maxSize; i++) {
			
			try {
				//Subset Simulation
				if(SimulationConfig.subsetSimulation && totalFetchedAssemblyPackets >= (SimulationConfig.subsetSimSize + SimulationConfig.NumInsToIgnore)) {
					fromEmulator.enqueue(totalFetchedAssemblyPackets-SimulationConfig.NumInsToIgnore, -1, -1);
					return (i+1);
				}
				
				String inputLine = inputBufferedReader[tidApp].readLine();
				
				if(inputLine != null) {
					
					long ip = -1, value = -1, tgt = -1;
					StringTokenizer stringTokenizer = new StringTokenizer(inputLine);
					
					ip = Long.parseLong(stringTokenizer.nextToken());
					value = Long.parseLong(stringTokenizer.nextToken());
					
					if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
					
						tgt = Long.parseLong(stringTokenizer.nextToken());
						totalFetchedAssemblyPackets += 1;
					
					} else if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
						
						if(value!=ASSEMBLY) {
							tgt = Long.parseLong(stringTokenizer.nextToken());
						} else {
							totalFetchedAssemblyPackets += 1;
							tgt = -1;
							CustomObjectPool.getCustomAsmCharPool().enqueue(tidApp, stringTokenizer.nextToken("\n").getBytes(), 1);
						}
						
					} else {
						misc.Error.showErrorAndExit("Invalid emulator type : " + 
								EmulatorConfig.EmulatorType + "!!");
					}
					
					//TODO: implement NumInsToIgnore for PIN
					//NumsToIgnore implemented only for QEMU
					if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
						//ignore these many instructions: NumInsToIgnore 
						if(totalFetchedAssemblyPackets < SimulationConfig.NumInsToIgnore) {
							if(value == ASSEMBLY) {
								CustomObjectPool.getCustomAsmCharPool().dequeue(tidApp);
							}
							return 0;
						// totalFetchedAssemblyPackets just became equal to NumInsToIgnore, so 
						// we start setting fromEmulator packets
						} else if(totalFetchedAssemblyPackets == SimulationConfig.NumInsToIgnore && value==ASSEMBLY) {
							i=0;						
						}	
					}
					
					fromEmulator.enqueue(ip, value, tgt);
					
//					System.out.println("sending packet : " + fromEmulator.get(i));
					
				} else {
					return (i);
				}
			} catch (IOException e) {
				misc.Error.showErrorAndExit("error in reading from file for tid = " + tidApp);
			}
		}
		
		return maxSize;
	}

	public void errorCheck(int tidApp, long totalReads) {
		// we do not do any error checking for filePacket interface
	}
	
	public void finish() {
		for(int i=0; i<maxApplicationThreads; i++) {
			try {
				if(inputBufferedReader[i] != null) {
					inputBufferedReader[i].close();	
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
