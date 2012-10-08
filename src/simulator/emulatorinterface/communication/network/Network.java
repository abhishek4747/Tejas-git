package emulatorinterface.communication.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import main.CustomObjectPool;
import config.EmulatorConfig;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;

public class Network extends IpcBase implements Encoding {
	
	static final int portStart = 9000;
	ServerSocket serverSocket[];
	Socket clientSocket[];
	BufferedInputStream inputStream[];
	int maxApplicationThreads;

	// 2KB buffer for network data
	final int bufferSize = 2 * 1024;
	byte inputBytes[][];
		
	public Network(int maxApplicationThreads) {
		
		this.maxApplicationThreads = maxApplicationThreads;
		inputBytes = new byte[maxApplicationThreads][bufferSize];
		serverSocket = new ServerSocket[maxApplicationThreads];
		clientSocket = new Socket[maxApplicationThreads];
		inputStream = new BufferedInputStream[maxApplicationThreads];
		
		for(int tidApp = 0; tidApp<maxApplicationThreads; tidApp++) {
			try {
				serverSocket[tidApp] = new ServerSocket(portStart+tidApp);
				clientSocket[tidApp] = null;
			} catch (IOException e) {
				e.printStackTrace();
				misc.Error.showErrorAndExit("error in opening socket on server side for tidApp : " + tidApp);
			}
		}
	}

	@Override
	public void initIpc() {
	}

	@Override
	public int fetchManyPackets(int tidApp, ArrayList<Packet> fromEmulator) {
		
		// If you are reading from a thread for the first time, open the connection with thread first
		if(clientSocket[tidApp]==null) {
			try {
				clientSocket[tidApp] = serverSocket[tidApp].accept();
				String address = clientSocket[tidApp].getInetAddress().getHostName();
				System.out.println("tidApp : "+ tidApp +" received connection request from " + address);
				inputStream[tidApp] = new BufferedInputStream(clientSocket[tidApp].getInputStream());
			} catch (IOException ioe) {
				ioe.printStackTrace();
				misc.Error.showErrorAndExit("error in accepting connection for tidApp : " + tidApp);
			}
		}


		int numPacketsRead = 0;

		try {
			// asynchronously determine the number of bytes available
			if(inputStream[tidApp].available() == 0) {
				return 0;
			}
		
			int numBytesRead = inputStream[tidApp].read(inputBytes[tidApp]);
			int numBytesConsumed = 0;
			
			if(EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_PIN) {
				
			} else if (EmulatorConfig.EmulatorType==EmulatorConfig.EMULATOR_QEMU) {
				
				for(int index=0; ; index++) {
					// we must be able to read at-least 3 longs
					if((numBytesRead-numBytesConsumed) < (3*8)) {
						inputStream[tidApp].mark(numBytesConsumed); 
						break;
					} else {
						long ip = getLong(inputBytes[tidApp], numBytesConsumed);
						numBytesConsumed += 8;
						long value = getLong(inputBytes[tidApp], numBytesConsumed);
						numBytesConsumed += 8;
						long tgt = -1;

						if(value==ASSEMBLY) {
							if((numBytesRead-numBytesConsumed)<64) {
								numBytesConsumed -= 16; // return two longs
								inputStream[tidApp].mark(numBytesConsumed);
								break;
							} else {
								CustomObjectPool.getCustomAsmCharPool().push(tidApp, inputBytes[tidApp]);
								numBytesConsumed += 64;
							}
						} else {
							tgt = getLong(inputBytes[tidApp], numBytesConsumed);
							numBytesConsumed += 8;
						}
						
						numPacketsRead++;
						fromEmulator.get(index).set(ip, value, tgt);
					}
				}
				
			} else {
				misc.Error.showErrorAndExit("Invalid emulator type : " + EmulatorConfig.EmulatorType);
			}
		} catch (IOException e) {
			e.printStackTrace();
			misc.Error.showErrorAndExit("error in fetching packet for tidApp : " + tidApp);
		}
		
		// print debug messages
		for(int i=0; i<numPacketsRead; i++) {
			System.out.println(fromEmulator.get(i));
		}
		
		return numPacketsRead;
	}

	private long getLong(byte[] inputBytes, int offset) {
		long value = 0;
		//for (int i = 0; i < 8; i++)
		for (int i = 7; i >= 0; i--)
		{
		   value = (value << 8) + (inputBytes[i+offset] & 0xff);
		}
		return value;
	}

	@Override
	public void errorCheck(int tidApp, long totalReads) {
		// Error check not required for network code.
	}
}