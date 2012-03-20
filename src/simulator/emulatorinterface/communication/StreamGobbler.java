/* Threads need to eat up the streams generated by the Emulator which
 * is being run through process.runtime. Just prints whatever it reads.
 * NOTE currently inputstream for pintool(and outputstream for java)
 * has not been constructed as we never needed it.
 * TODO should ideally implement a StreamWriter as well which can be used to pass
 * arguments to the executable or the PIN tool.
 */

package emulatorinterface.communication;

import java.io.*;

public class StreamGobbler implements Runnable {

	String name;	//Threads name (stdin or stderr)
	InputStream is;
	Thread thread;

	public StreamGobbler (String name, InputStream is) {
		this.name = name;
		this.is = is;
	}

	public void start () {
		thread = new Thread (this);
		thread.start ();
	}

	public void run () {
		try {
			InputStreamReader isr = new InputStreamReader (is);
			BufferedReader br = new BufferedReader (isr);

			while (true) {
				String s = br.readLine ();
				if (s == null) break;
				System.out.println("[" + name + "] " + s);
			}

			is.close ();

		} catch (Exception ex) {
			System.out.println("Problem reading stream " + name + "... :" + ex);
			ex.printStackTrace ();
		}
	}
	
	public void join() throws InterruptedException{
		thread.join();
	}
}