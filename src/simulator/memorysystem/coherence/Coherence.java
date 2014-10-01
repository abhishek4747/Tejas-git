package memorysystem.coherence;

import java.io.FileWriter;
import java.io.IOException;

import config.EnergyConfig;

import memorysystem.Cache;

public interface Coherence {
	public abstract void readMiss(long addr, Cache c);
	public abstract void writeHit(long addr, Cache c);
	public abstract void writeMiss(long addr, Cache c);
	public abstract void evictedFromCoherentCache(long addr, Cache c);
	public abstract void evictedFromSharedCache(long addr, Cache c);
	public abstract void unlock(long addr, Cache c);
	public abstract void printStatistics(FileWriter outputFileWriter) throws IOException;
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException;
}
