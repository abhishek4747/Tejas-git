package generic;

public class GlobalClock {
	
	static long currentTime;
	static int stepSize;
	
	public GlobalClock()
	{
		currentTime = 0;
		stepSize = 1;
	}
	
	public static void systemTimingSetUp()
	{
		
	}

	public static long getCurrentTime() {
		return GlobalClock.currentTime;
	}

	public static void setCurrentTime(long currentTime) {
		GlobalClock.currentTime = currentTime;
	}
	
	public static void incrementClock()
	{
		GlobalClock.currentTime += GlobalClock.stepSize;
	}

	public static int getStepSize() {
		return GlobalClock.stepSize;
	}

	public static void setStepSize(int stepSize) {
		GlobalClock.stepSize = stepSize;
	}

}
