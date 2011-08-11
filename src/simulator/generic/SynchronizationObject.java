package generic;

public class SynchronizationObject {
	
	boolean flag;

	public SynchronizationObject()
	{
		flag = false;
	}
	
	synchronized public boolean isFlag() {
		return flag;
	}

	synchronized public void setFlag(boolean flag) {
		this.flag = flag;
	}

}
