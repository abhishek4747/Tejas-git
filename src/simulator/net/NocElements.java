package net;

import generic.Core;

import java.util.Vector;

import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;

import config.SystemConfig;
import main.ArchitecturalComponent;
import memorysystem.nuca.NucaCache;
import net.NOC;
import net.NOC.CONNECTIONTYPE;
import net.NocInterface;
import net.optical.OpticalNOC;
import net.optical.TopLevelTokenBus;

public class NocElements 
{
	public int rows;
	public int columns;
	public int noOfCores;
	public int noOfCacheBanks;
	public NocInterface[][] nocElements;
	public Vector<Vector<Integer>> coresCacheLocations;
	public NOC noc;
	public NocElements(int r, int c)
	{
		nocElements = new NocInterface[r][c]; 
		coresCacheLocations = new Vector<Vector<Integer>>();
		rows = r;
		columns = c;
	}
	public void makeNocElements(TopLevelTokenBus tokenbus, NucaCache nucaCache)
	{
		if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.ELECTRICAL)
            noc = new NOC();
        else
            noc = new OpticalNOC();
		int coreNumber = 0;
		int cacheNumber =0;
		for(int i=0;i<rows;i++)
		{
			nocElements[i] = new NocInterface[columns];
			for(int j=0;j<columns;j++)
			{
				if(coresCacheLocations.get(i).get(j)==1)
				{
					nocElements[i][j] = (NocInterface) ArchitecturalComponent.getCores()[coreNumber];
					Vector<Integer> id = new Vector<Integer>();
					id.add(i);
					id.add(j);
					((Core)nocElements[i][j]).setId(id);
					coreNumber++;
				}
				else if(coresCacheLocations.get(i).get(j)==0)
				{
					nocElements[i][j] = nucaCache.cacheBank.get(cacheNumber);
					cacheNumber++;
				}
			}
		}
		noc.ConnectBanks(nocElements,rows,columns,SystemConfig.nocConfig,tokenbus);
	}
}
