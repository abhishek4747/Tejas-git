package config;

public class EmulatorConfig {
	
	// constants
	public static final int COMMUNICATION_FILE_MICROOPS = 0;
	public static final int COMMUNICATION_SHM = 1;
	public static final int COMMUNICATION_NETWORK = 2;
	public static final int COMMUNICATION_FILE_PACKET = 3;
	
	public static final int EMULATOR_PIN = 0;
	public static final int EMULATOR_QEMU = 1;
	
	
	public static int CommunicationType = -1;  // Communication - 0-fileMicroOps, 1-shmem, 2-socket, 3-filePacket
	public static int EmulatorType = -1; // Emulator - pin, qemu ??
	public static String PinTool = null;
	public static String PinInstrumentor = null;
	public static String QemuTool = null;
	public static String ShmLibDirectory;
}
