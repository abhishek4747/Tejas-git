package net;

import memorysystem.nuca.NucaCacheBank;

public class NOC{
	public static enum TOPOLOGY{
		MESH,
		TORUS,
		TWODMESH,
		HYPERCUBE,
		BUS,
		RING,
		STAR
	}
	
	public void ConnectBanks(NucaCacheBank cacheBank[][],int bankRows,int bankColumns,TOPOLOGY topology)
	{
		switch (topology) {
		case MESH:
			ConnectBanksMesh(cacheBank, bankRows, bankColumns);
		case TORUS:
			ConnectBanksTorus(cacheBank, bankRows, bankColumns);
		}
			
	}
	public void ConnectBanks(NucaCacheBank cacheBank[],int numOfBanks,TOPOLOGY topology){
		switch (topology){
		case BUS :
			ConnectBanksBus(cacheBank, numOfBanks);
		case RING :
			ConnectBanksRing(cacheBank, numOfBanks);
		}
	}
	public void ConnectBanksMesh(NucaCacheBank cacheBank[][],int bankRows,int bankColumns)  //connect bank in MESH fashion
	{
		int i,j;
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				if(i==0)                        //setting null for 0th raw up connection
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, cacheBank[i-1][j]);
				
				if(j==bankColumns-1)             //right connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, cacheBank[i][j+1]);
				
				if(i==bankRows-1)             //down connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, cacheBank[i+1][j]);
				
				if(j==0)			            //left connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, cacheBank[i][j-1]);
			}
	    }
	}
	public void ConnectBanksTorus(NucaCacheBank cacheBank[][],int bankRows,int bankColumns) //torus connection
	{
		int i,j;
		for(i=0;i<bankRows;i++)
		{
			for(j=0;j<bankColumns;j++)
			{
				if(i==0)                        //setting null for 0th raw up connection
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP,cacheBank[bankRows-1][j]);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP, cacheBank[i-1][j]);
				
				if(j==bankColumns-1)             //right connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, cacheBank[i][0]);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, cacheBank[i][j+1]);
				
				if(i==bankRows-1)             //down connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, cacheBank[0][j]);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN, cacheBank[i+1][j]);
				
				if(j==0)			            //left connections
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, cacheBank[i][bankColumns-1]);
				else
					cacheBank[i][j].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT, cacheBank[i][j-1]);
			}
	    }
	}
	public void ConnectBanksBus(NucaCacheBank cacheBank[], int numOfBanks)
	{
		int i;
		for(i=0;i<numOfBanks;i++)
		{
			if(i != numOfBanks-1)
				cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, cacheBank[i+1]);
			else
				cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT);
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);  //setting other dir null
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
		}
	}
	public void ConnectBanksRing(NucaCacheBank cacheBank[], int numOfBanks)
	{
		int i;
		for(i=0;i<numOfBanks;i++)
		{
			if(i != numOfBanks-1)
				cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT, cacheBank[i+1]);
			else
				cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.RIGHT,cacheBank[0]);
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.LEFT);  //setting other dir null
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.UP);
			cacheBank[i].router.SetConnectedBanks(RoutingAlgo.DIRECTION.DOWN);
		}
	}
}