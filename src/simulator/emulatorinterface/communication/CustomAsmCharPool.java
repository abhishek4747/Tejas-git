package emulatorinterface.communication;

public class CustomAsmCharPool {
	byte pool[][][];
	int top[];
	final int bufferSize = 2*1024;
	
	public CustomAsmCharPool(int maxApplicationThreads)
	{
		pool = new byte[maxApplicationThreads][bufferSize][64];
		top = new int[maxApplicationThreads];
		for(int tidApp=0; tidApp<maxApplicationThreads; tidApp++) {
			top[tidApp] = -1;
		}
	}
	
	public void push(int tidApp, byte inputBytes[])
	{
		if(top[tidApp]==bufferSize) {
			misc.Error.showErrorAndExit("unable to handle new asm bytes");
		} else {
			top[tidApp]++;
			for(int i=0; i<64; i++) {
				pool[tidApp][top[tidApp]][i] = inputBytes[i];
			}
		}
	}
	
	public void returnTop(int tidApp)
	{
		if(top[tidApp]==-1) {
			misc.Error.showErrorAndExit("pool underflow !!");
		} else {
			top[tidApp]--;
		}
	}
}
