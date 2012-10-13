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
	
	public void push(int tidApp, byte inputBytes[], int offset)
	{
		if(top[tidApp]==bufferSize) {
			misc.Error.showErrorAndExit("unable to handle new asm bytes");
		} else {
			top[tidApp]++;
			for(int i=0; i<64; i++) {
				
//				char ch = (char)inputBytes[offset+i];
//				System.out.print(ch);
				
				pool[tidApp][top[tidApp]][i] = inputBytes[offset+i];
			}
//			System.out.println();
		}
	}
	
	public byte[] pop(int tidApp)
	{
		if(top[tidApp]==-1) {
			misc.Error.showErrorAndExit("pool underflow !!");
		} else {
			return (pool[tidApp][top[tidApp]--]);
		}
		
		// We will never reach this statement
		return null;
	}
	
	public byte[] top(int tidApp)
	{
		if(top[tidApp]==-1) {
			misc.Error.showErrorAndExit("pool underflow !!");
		} else {
			return (pool[tidApp][top[tidApp]]);
		}
		
		// We will never reach this statement
		return null;
	}
}
