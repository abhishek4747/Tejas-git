package config;

import generic.GlobalClock;

import java.io.FileWriter;
import java.io.IOException;

public class PowerConfigNew {
	public double leakagePower;
	public double dynamicPower;
	public long numAccesses = 0;
	
	public PowerConfigNew(double leakagePower, double dynamicPower) {
		this.leakagePower = leakagePower;
		this.dynamicPower = dynamicPower;
	}
	
	public PowerConfigNew(PowerConfigNew power, long numAccesses) {
		this.leakagePower = power.leakagePower * (GlobalClock.getCurrentTime());
		this.dynamicPower = power.dynamicPower*numAccesses;
		this.numAccesses = numAccesses;
	}
	
	public String toString()
	{
		return " " + leakagePower
				+ "\t" + dynamicPower
				+ "\t" + (leakagePower + dynamicPower);
	}
	
	public void add(PowerConfigNew a, PowerConfigNew b)
	{
		leakagePower = a.leakagePower + b.leakagePower;
		dynamicPower = a.dynamicPower + b.dynamicPower;
	}
	
	public void add(PowerConfigNew a)
	{
		leakagePower += a.leakagePower;
		dynamicPower += a.dynamicPower;
	}

	public void printPowerStats(FileWriter outputFileWriter, String componentName) throws IOException {
		outputFileWriter.write(componentName + "\t" + leakagePower + "\t" + dynamicPower 
				+ "\t" + (leakagePower + dynamicPower) + "\t" + numAccesses + "\n");
	}
}
