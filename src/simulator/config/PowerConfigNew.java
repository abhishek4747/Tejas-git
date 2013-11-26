package config;

public class PowerConfigNew {
	public double leakagePower;
	public double dynamicPower;
	
	public PowerConfigNew(double leakagePower, double dynamicPower) {
		this.leakagePower = leakagePower;
		this.dynamicPower = dynamicPower;
	}
	
	public String toString()
	{
		return "leakage power = " + leakagePower
				+ "\nruntime dynamic power = " + dynamicPower
				+ "\ntotal power = " + (leakagePower + dynamicPower);
	}
	
	public void add(PowerConfigNew a, PowerConfigNew b)
	{
		leakagePower = a.leakagePower + b.leakagePower;
		dynamicPower = a.dynamicPower + b.dynamicPower;
	}
}
