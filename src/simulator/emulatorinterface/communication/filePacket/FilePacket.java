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
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;

public class FilePacket extends IpcBase implements Encoding {

	BufferedReader inputBufferedReader[];
	int maxApplicationThreads = -1;
	
	public FilePacket(int maxApplicationThreads) {
		this.maxApplicationThreads = maxApplicationThreads;
		
		inputBufferedReader = new BufferedReader[maxApplicationThreads];
		
		for (int i=0; i<maxApplicationThreads; i++) {
			String inputFileName = SimulationConfig.InstructionsFilename + "_" + i;
			try {
				inputBufferedReader[i] = new BufferedReader(
					new FileReader(	new File(inputFileName)));
			} catch (FileNotFoundException e) {
				misc.Error.showErrorAndExit("Error in reading input packet file " + inputFileName);
			}
		}
	}

	public void initIpc() {
		// this does nothing
	}

	public int fetchManyPackets(int tidApp, ArrayList<Packet> fromEmulator) {
		
		if(tidApp>=maxApplicationThreads) {
			return 0;
		}
		
		int maxSize = fromEmulator.size();
		
		for(int i=0; i<maxSize; i++) {
			
			try {
				
				String inputLine = inputBufferedReader[tidApp].readLine();
				
				if(inputLine != null) {
					
					long ip = -1, value = -1, tgt = -1;
					StringTokenizer stringTokenizer = new StringTokenizer(inputLine);
					
					ip = Long.parseLong(stringTokenizer.nextToken());
					value = Long.parseLong(stringTokenizer.nextToken());
					
					if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
					
						tgt = Long.parseLong(stringTokenizer.nextToken());
					
					} else if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
						
						if(value!=ASSEMBLY) {
							tgt = Long.parseLong(stringTokenizer.nextToken());
						} else {
							tgt = -1;
							CustomObjectPool.getCustomAsmCharPool().enqueue(tidApp, inputLine.getBytes(), 0);
						}
						
					} else {
						misc.Error.showErrorAndExit("Invalid emulator type : " + 
								EmulatorConfig.EmulatorType + "!!");
					}
					
					fromEmulator.get(i).set(ip, value, tgt);
					
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
				inputBufferedReader[i].close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
