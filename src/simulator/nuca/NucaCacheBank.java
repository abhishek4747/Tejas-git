package nuca;

public class NucaCacheBank
{
    private NucaCacheLine bank[];//array of all the cacheLines present in a cache bank
    private double timestamp;//used when LRU replacement policy is used for LLC
    
    NucaCacheBank(int bankSize)
    {
        this.timestamp = 0;
    	bank = new NucaCacheLine[bankSize];
        for(int i=0;i<bankSize;i++)
        {
            bank[i] = new NucaCacheLine(i);
        }
    }
    
    public boolean lookup(long tag)//looks for tag in cache lines present in the bank sequentially
    {
        for(int i=0;i<bank.length;i++)
        {
            if(tag ==bank[i].getTag())
                return true;
        }
        return false;
    }

    public Object clone()
    {
        try
        {
            // call clone in Object.
            return super.clone();
        } catch(CloneNotSupportedException e)
        {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }
	
    public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}
	
    public double getTimestamp() {
		return timestamp;
	}
}