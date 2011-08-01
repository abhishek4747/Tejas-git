package generic;

public class Time_t
{
	private long time;
	
	//FIXME: lots of things are gonna come here.
	public Time_t(long time)
	{
		this.time=time;
	}
	
	public boolean equals(Time_t time)
	{
		return(this.time == time.time);
	}
	

	public boolean lessThan(Time_t time)
	{
		return(this.time < time.time);
	}
	

	public boolean greaterThan(Time_t time)
	{
		return(this.time > time.time);
	}

	public void add(int noOfSlots) 
	{
		this.time += noOfSlots;		
	}
}