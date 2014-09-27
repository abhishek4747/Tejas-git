package memorysystem.coherence;

import memorysystem.Cache;

public interface Coherence {
	public abstract void readMiss(long addr, Cache c);
	public abstract void writeHit(long addr, Cache c);
	public abstract void writeMiss(long addr, Cache c);
	public abstract void evictedFromCoherentCache(long addr, Cache c);
	public abstract void evictedFromSharedCache(long addr, Cache c);
	public abstract void unlock(long addr, Cache c);
}
