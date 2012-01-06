package nuca;
import memorysystem.CacheLine;

public class NucaCacheLine extends CacheLine {

	public NucaCacheLine(int lineNum) {
		super(lineNum);
		// TODO Auto-generated constructor stub
	}
	public long getTag()
	{
		return super.getTag();
	}
	public double getTimestamp()
	{
		return super.getTimestamp();
	}
	public void setTimestamp(double timestamp)
	{
		super.setTimestamp(timestamp);
	}
}
