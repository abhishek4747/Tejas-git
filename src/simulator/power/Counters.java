package power;

import config.SystemConfig;

public class Counters {

	/* options for Wattch */
	long dataWidth;

	/* counters added for Wattch */
	long renameAccess=0;
	long bpredAccess=0;
	private long bpredMisses = 0;
	long windowAccess=0;
	long lsqAccess=0;
	long regfileAccess=0;

	long IntegerRegfileAccess=0;
	long FloatRegfileAccess=0;
	long IntegerRenameAccess=0;
	long FloatRenameAccess=0;
	
	long icacheAccess=0;
	long dcacheAccess=0;

	static long dcache2Access=0;
	long aluAccess=0;
	long ialuAccess=0;
	long faluAccess=0;
	long resultbusAccess=0;

	long windowPregAccess=0;
	long windowSelectionAccess=0;
	long windowWakeupAccess=0;
	long lsqStoreDataAccess=0;
	long lsqLoadDataAccess=0;
	long lsqPregAccess=0;
	long lsqWakeupAccess=0;

	/*Following counters keep a track of whether there were non zero access in a given cycle for the corresponding counter*/
	long renameAccessCycle=0;
	long bpredAccessCycle=0;
	long windowAccessCycle=0;
	long lsqAccessCycle=0;
	long regfileAccessCycle=0;

	long IntegerRegfileAccessCycle=0;
	long FloatRegfileAccessCycle=0;
	long IntegerRenameAccessCycle=0;
	long FloatRenameAccessCycle=0;
	
	long icacheAccessCycle=0;
	long dcacheAccessCycle=0;

	static long dcache2AccessCycle=0;
	long aluAccessCycle=0;
	long ialuAccessCycle=0;
	long faluAccessCycle=0;
	long resultbusAccessCycle=0;

	long windowPregAccessCycle=0;
	long windowSelectionAccessCycle=0;
	long windowWakeupAccessCycle=0;
	long lsqStoreDataAccessCycle=0;
	long lsqLoadDataAccessCycle=0;
	long lsqPregAccessCycle=0;
	long lsqWakeupAccessCycle=0;


	/*Following counters keep a track of previous cycle value of the counter*/
	long prevrenameAccess=0;
	long prevbpredAccess=0;
	long prevwindowAccess=0;
	long prevlsqAccess=0;
	long prevregfileAccess=0;

	long prevIntegerRegfileAccess=0;
	long prevFloatRegfileAccess=0;
	long prevIntegerRenameAccess=0;
	long prevFloatRenameAccess=0;
	
	long previcacheAccess=0;
	long prevdcacheAccess=0;

	static long prevdcache2Access=0;
	long prevaluAccess=0;
	long previaluAccess=0;
	long prevfaluAccess=0;
	long prevresultbusAccess=0;

	long prevwindowPregAccess=0;
	long prevwindowSelectionAccess=0;
	long prevwindowWakeupAccess=0;
	long prevlsqStoreDataAccess=0;
	long prevlsqLoadDataAccess=0;
	long prevlsqPregAccess=0;
	long prevlsqWakeupAccess=0;

	long windowTotalPopCountCycle=0;
	long windowNumPopCountCycle=0;
	long lsqTotalPopCountCycle=0;
	long lsqNumPopCountCycle=0;
	long regfileTotalPopCountCycle=0;
	long regfileNumPopCountCycle=0;
	long resultbusTotalPopCountCycle=0;
	long resultbusNumPopCountCycle=0;
	
	/*Total Counts*/
	long totalRenameAccess=0;
	long totalBpredAccess=0;
	long totalWindowAccess=0;
	long totalLsqAccess=0;
	long totalRegfileAccess=0;

	long totalIcacheAccess=0;
	long totalDcacheAccess=0;

	long totalDcache2Access=0;
	long totalAluAccess=0;
	long totalIaluAccess=0;
	long totalFaluAccess=0;
	long totalResultbusAccess=0;

	long totalWindowPregAccess=0;
	long totalWindowSelectionAccess=0;
	long totalWindowWakeupAccess=0;
	long totalLsqStoreDataAccess=0;
	long totalLsqLoadDataAccess=0;
	long totalLsqPregAccess=0;
	long totalLsqWakeupAccess=0;

	long totalIntegerRegfileAccess=0;
	long totalFloatRegfileAccess=0;
	long totalIntegerRenameAccess=0;
	long totalFloatRenameAccess=0;

	
	/*Following counters keep a track of whether there were non zero access in a given cycle for the corresponding counter*/
	long totalRenameAccessCycle=0;
	long totalBpredAccessCycle=0;
	long totalWindowAccessCycle=0;
	long totalLsqAccessCycle=0;
	long totalRegfileAccessCycle=0;

	long totalIntegerRegfileAccessCycle=0;
	long totalFloatRegfileAccessCycle=0;
	long totalIntegerRenameAccessCycle=0;
	long totalFloatRenameAccessCycle=0;
	
	long totalIcacheAccessCycle=0;
	long totalDcacheAccessCycle=0;

	static long totalDcache2AccessCycle=0;
	long totalAluAccessCycle=0;
	long totalIaluAccessCycle=0;
	long totalFaluAccessCycle=0;
	long totalResultbusAccessCycle=0;

	long totalWindowPregAccessCycle=0;
	long totalWindowSelectionAccessCycle=0;
	long totalWindowWakeupAccessCycle=0;
	long totalLsqStoreDataAccessCycle=0;
	long totalLsqLoadDataAccessCycle=0;
	long totalLsqPregAccessCycle=0;
	long totalLsqWakeupAccessCycle=0;
	
	
	long maxRenameAccess=0;
	long maxBpredAccess=0;
	long maxWindowAccess=0;
	long maxLsqAccess=0;
	long maxRegfileAccess=0;
	long maxIcacheAccess=0;
	long maxDcacheAccess=0;
	long maxDcache2Access=0;
	long maxAluAccess=0;
	long maxResultbusAccess=0;
	
	long maxIntegerRenameAccess=0;
	long maxFloatRenameAccess=0;
	long maxIntegerRegfileAccess=0;
	long maxFloatRegfileAccess=0;

	double renamePower=0;
	double bpredPower=0;
	double windowPower=0;
	double lsqPower=0;
	double regfilePower=0;
	double icachePower=0;
	double dcachePower=0;
	double dcache2Power=0;
	double aluPower=0;
	double resultbusPower=0;
	double clockPower=0;


	double totalRenamePower=0;
	double totalBpredPower=0;
	double totalWindowPower=0;
	double totalLsqPower=0;
	double totalRegfilePower=0;
	double totalIcachePower=0;
	double totalDcachePower=0;
	double totalDcache2Power=0;
	double totalAluPower=0;
	double totalResultbusPower=0;
	double totalClockPower=0;

	double totalCyclePower;

	double lastSingleTotalCyclePower = 0.0;
	double currentTotalCyclePower;

	double maxCyclePower = 0.0;

	double turnoffFactor = 0.1;

	private double totaRegfilePower;

	
	public Counters(){
		/* options for Wattch *///FIXME 
		 dataWidth = 64;

		/* counters added for Wattch */
		 renameAccess=0;
		 bpredAccess=0;
		 windowAccess=0;
		 lsqAccess=0;
		 regfileAccess=0;
		 
		 //icacheAccess=new long[SystemConfig.NoOfCores];
		 //dcacheAccess=new long[SystemConfig.NoOfCores];
		 icacheAccess=0;
		 dcacheAccess=0;
//		 for(int i=0;i<SystemConfig.NoOfCores;i++){
//			 icacheAccess[i]=0;
//			 dcacheAccess[i]=0;
//		 }
//		 
		 icacheAccess=0;
		 dcacheAccess=0;
		 dcache2Access=0;
		 aluAccess=0;
		 ialuAccess=0;
		 faluAccess=0;
		 resultbusAccess=0;

		 windowPregAccess=0;
		 windowSelectionAccess=0;
		 windowWakeupAccess=0;
		 lsqStoreDataAccess=0;
		 lsqLoadDataAccess=0;
		 lsqPregAccess=0;
		 lsqWakeupAccess=0;

		 renameAccessCycle=0;
		 bpredAccessCycle=0;
		 windowAccessCycle=0;
		 lsqAccessCycle=0;
		 regfileAccessCycle=0;

		 IntegerRegfileAccessCycle=0;
		 FloatRegfileAccessCycle=0;
		 IntegerRenameAccessCycle=0;
		 FloatRenameAccessCycle=0;
		
		 icacheAccessCycle=0;
		 dcacheAccessCycle=0;

		 dcache2AccessCycle=0;
		 aluAccessCycle=0;
		 ialuAccessCycle=0;
		 faluAccessCycle=0;
		 resultbusAccessCycle=0;

		 windowPregAccessCycle=0;
		 windowSelectionAccessCycle=0;
		 windowWakeupAccessCycle=0;
		 lsqStoreDataAccessCycle=0;
		 lsqLoadDataAccessCycle=0;
		 lsqPregAccessCycle=0;
		 lsqWakeupAccessCycle=0;

			
		 prevrenameAccess=0;
		 prevbpredAccess=0;
		 prevwindowAccess=0;
		 prevlsqAccess=0;
		 prevregfileAccess=0;

		 prevIntegerRegfileAccess=0;
		 prevFloatRegfileAccess=0;
		 prevIntegerRenameAccess=0;
		 prevFloatRenameAccess=0;
		
		 previcacheAccess=0;
		 prevdcacheAccess=0;

		 prevdcache2Access=0;
		 prevaluAccess=0;
		 previaluAccess=0;
		 prevfaluAccess=0;
		 prevresultbusAccess=0;

		 prevwindowPregAccess=0;
		 prevwindowSelectionAccess=0;
		 prevwindowWakeupAccess=0;
		 prevlsqStoreDataAccess=0;
		 prevlsqLoadDataAccess=0;
		 prevlsqPregAccess=0;
		 prevlsqWakeupAccess=0;
		
		 windowTotalPopCountCycle=0;
		 windowNumPopCountCycle=0;
		 lsqTotalPopCountCycle=0;
		 lsqNumPopCountCycle=0;
		 regfileTotalPopCountCycle=0;
		 regfileNumPopCountCycle=0;
		 resultbusTotalPopCountCycle=0;
		 resultbusNumPopCountCycle=0;
		 
		 renamePower=0;
		 bpredPower=0;
		 windowPower=0;
		 lsqPower=0;
		 regfilePower=0;
		 icachePower=0;
		 dcachePower=0;
		 dcache2Power=0;
		 aluPower=0;
		 resultbusPower=0;
		 clockPower=0;

		 lastSingleTotalCyclePower = 0.0;

		 maxCyclePower = 0.0;

		IntegerRegfileAccess=0;
		FloatRegfileAccess=0;
		IntegerRenameAccess=0;
		FloatRenameAccess=0;

		totalRenameAccess=0;
		totalBpredAccess=0;
		totalWindowAccess=0;
		totalLsqAccess=0;
		totalRegfileAccess=0;
		totalIcacheAccess=0;
		totalDcacheAccess=0;
		totalDcache2Access=0;
		totalAluAccess=0;
		totalIaluAccess=0;
		totalFaluAccess=0;
		totalResultbusAccess=0;

		totalWindowPregAccess=0;
		totalWindowSelectionAccess=0;
		totalWindowWakeupAccess=0;
		totalLsqStoreDataAccess=0;
		totalLsqLoadDataAccess=0;
		totalLsqPregAccess=0;
		totalLsqWakeupAccess=0;

		totalIntegerRegfileAccess=0;
		totalFloatRegfileAccess=0;
		totalIntegerRenameAccess=0;
		totalFloatRenameAccess=0;
		
		totalRenameAccessCycle=0;
		totalBpredAccessCycle=0;
		totalWindowAccessCycle=0;
		totalLsqAccessCycle=0;
		totalRegfileAccessCycle=0;

		totalIntegerRegfileAccessCycle=0;
		totalFloatRegfileAccessCycle=0;
		totalIntegerRenameAccessCycle=0;
		totalFloatRenameAccessCycle=0;
		
		totalIcacheAccessCycle=0;
		totalDcacheAccessCycle=0;

		totalDcache2AccessCycle=0;
		totalAluAccessCycle=0;
		totalIaluAccessCycle=0;
		totalFaluAccessCycle=0;
		totalResultbusAccessCycle=0;

		totalWindowPregAccessCycle=0;
		totalWindowSelectionAccessCycle=0;
		totalWindowWakeupAccessCycle=0;
		totalLsqStoreDataAccessCycle=0;
		totalLsqLoadDataAccessCycle=0;
		totalLsqPregAccessCycle=0;
		totalLsqWakeupAccessCycle=0;
	}
	
	public void clearAccessStats(){

		totalRenameAccess += renameAccess;
		totalBpredAccess += bpredAccess;
		totalWindowAccess += windowAccess;
		totalLsqAccess += lsqAccess;
		totalRegfileAccess += regfileAccess;
		totalIntegerRegfileAccess += IntegerRegfileAccess;
		totalFloatRegfileAccess += FloatRegfileAccess;
		totalIntegerRenameAccess += IntegerRenameAccess;
		totalFloatRenameAccess += FloatRenameAccess;
		totalIcacheAccess += icacheAccess;
		totalDcacheAccess += dcacheAccess;
		totalDcache2Access += dcache2Access;
		totalAluAccess += aluAccess;
		totalIaluAccess += ialuAccess;
		totalFaluAccess += faluAccess;
		
		totalResultbusAccess += resultbusAccess;
		
		totalWindowPregAccess += windowPregAccess;
		totalWindowSelectionAccess += windowSelectionAccess;
		totalWindowWakeupAccess += windowWakeupAccess;
		totalLsqLoadDataAccess += lsqLoadDataAccess;
		totalLsqStoreDataAccess += lsqStoreDataAccess;
		totalLsqWakeupAccess += lsqWakeupAccess;
		totalLsqPregAccess += lsqWakeupAccess;
		
		/* counters added for Wattch */
		 renameAccess=0;
		 bpredAccess=0;
		 windowAccess=0;
		 lsqAccess=0;
		 regfileAccess=0;
	 
		IntegerRegfileAccess=0;
		FloatRegfileAccess=0;
		IntegerRenameAccess=0;
		FloatRenameAccess=0;
//		 for(int i=0;i<SystemConfig.NoOfCores;i++){
//			 icacheAccess[i]=0;
//			 dcacheAccess[i]=0;
//		 }
//		 
		 icacheAccess=0;
		 dcacheAccess=0;
		 dcache2Access=0;
		 aluAccess=0;
		 
		 ialuAccess=0;
		 faluAccess=0;
		 resultbusAccess=0;

		 windowPregAccess=0;
		 windowSelectionAccess=0;
		 windowWakeupAccess=0;
		 lsqStoreDataAccess=0;
		 lsqLoadDataAccess=0;
		 lsqPregAccess=0;
		 lsqWakeupAccess=0;

		 windowTotalPopCountCycle=0;
		 windowNumPopCountCycle=0;
		 lsqTotalPopCountCycle=0;
		 lsqNumPopCountCycle=0;
		 regfileTotalPopCountCycle=0;
		 regfileNumPopCountCycle=0;
		 resultbusTotalPopCountCycle=0;
		 resultbusNumPopCountCycle=0;

			totalRenameAccessCycle += renameAccessCycle;
			totalBpredAccessCycle += bpredAccessCycle;
			totalWindowAccessCycle += windowAccessCycle;
			totalLsqAccessCycle += lsqAccessCycle;
			totalRegfileAccessCycle += regfileAccessCycle;
			totalIntegerRegfileAccessCycle += IntegerRegfileAccessCycle;
			totalFloatRegfileAccessCycle += FloatRegfileAccessCycle;
			totalIntegerRenameAccessCycle += IntegerRenameAccessCycle;
			totalFloatRenameAccessCycle += FloatRenameAccessCycle;
			totalIcacheAccessCycle += icacheAccessCycle;
			totalDcacheAccessCycle += dcacheAccessCycle;
			totalDcache2AccessCycle += dcache2AccessCycle;
			totalAluAccessCycle += aluAccessCycle;
			totalIaluAccessCycle += ialuAccessCycle;
			totalFaluAccessCycle += faluAccessCycle;
			
			totalResultbusAccessCycle += resultbusAccessCycle;
			
			totalWindowPregAccessCycle += windowPregAccessCycle;
			totalWindowSelectionAccessCycle += windowSelectionAccessCycle;
			totalWindowWakeupAccessCycle += windowWakeupAccessCycle;
			totalLsqLoadDataAccessCycle += lsqLoadDataAccessCycle;
			totalLsqStoreDataAccessCycle += lsqStoreDataAccessCycle;
			totalLsqWakeupAccessCycle += lsqWakeupAccessCycle;
			totalLsqPregAccessCycle += lsqWakeupAccessCycle;
			
		 renameAccessCycle=0;
		 bpredAccessCycle=0;
		 windowAccessCycle=0;
		 lsqAccessCycle=0;
		 regfileAccessCycle=0;

		 IntegerRegfileAccessCycle=0;
		 FloatRegfileAccessCycle=0;
		 IntegerRenameAccessCycle=0;
		 FloatRenameAccessCycle=0;
		
		 icacheAccessCycle=0;
		 dcacheAccessCycle=0;

		 dcache2AccessCycle=0;
		 aluAccessCycle=0;
		 ialuAccessCycle=0;
		 faluAccessCycle=0;
		 resultbusAccessCycle=0;

		 windowPregAccessCycle=0;
		 windowSelectionAccessCycle=0;
		 windowWakeupAccessCycle=0;
		 lsqStoreDataAccessCycle=0;
		 lsqLoadDataAccessCycle=0;
		 lsqPregAccessCycle=0;
		 lsqWakeupAccessCycle=0;
		 prevrenameAccess=0;
		 prevbpredAccess=0;
		 prevwindowAccess=0;
		 prevlsqAccess=0;
		 prevregfileAccess=0;

		 prevIntegerRegfileAccess=0;
		 prevFloatRegfileAccess=0;
		 prevIntegerRenameAccess=0;
		 prevFloatRenameAccess=0;
		
		 previcacheAccess=0;
		 prevdcacheAccess=0;

		 prevdcache2Access=0;
		 prevaluAccess=0;
		 previaluAccess=0;
		 prevfaluAccess=0;
		 prevresultbusAccess=0;

		 prevwindowPregAccess=0;
		 prevwindowSelectionAccess=0;
		 prevwindowWakeupAccess=0;
		 prevlsqStoreDataAccess=0;
		 prevlsqLoadDataAccess=0;
		 prevlsqPregAccess=0;
		 prevlsqWakeupAccess=0;

		 renamePower=0;
		 bpredPower=0;
		 windowPower=0;
		 lsqPower=0;
		 regfilePower=0;
		 icachePower=0;
		 dcachePower=0;
		 dcache2Power=0;
		 aluPower=0;
		 resultbusPower=0;
		 clockPower=0;

	}
	
	public void perCycleAccessRecordUpdate(){
		if(renameAccess - prevrenameAccess > 0){
			renameAccessCycle++;
			prevrenameAccess = renameAccess;
		}
		if(bpredAccess - prevbpredAccess > 0){
			bpredAccessCycle++;
			prevbpredAccess = bpredAccess;
		}
		if(windowPregAccess - prevwindowPregAccess > 0){
			windowPregAccessCycle++;
			prevwindowPregAccess = windowPregAccess;
		}
		if(windowSelectionAccess - prevwindowSelectionAccess > 0){
			windowSelectionAccessCycle++;
			prevwindowSelectionAccess = windowSelectionAccess;
		}
		if(windowWakeupAccess - prevwindowWakeupAccess > 0){
			windowWakeupAccessCycle++;
			prevwindowWakeupAccess = windowWakeupAccess;
		}
		if(lsqWakeupAccess - prevlsqWakeupAccess > 0){
			lsqWakeupAccessCycle++;
			prevlsqWakeupAccess = lsqWakeupAccess;
		}
		if(lsqPregAccess - prevlsqPregAccess > 0){
			lsqPregAccessCycle++;
			prevlsqPregAccess = lsqPregAccess;
		}
		if(IntegerRegfileAccess - prevIntegerRegfileAccess > 0){
			IntegerRegfileAccessCycle++;
			prevIntegerRegfileAccess = IntegerRegfileAccess;
		}
		if(icacheAccess - previcacheAccess > 0){
			icacheAccessCycle++;
			previcacheAccess = icacheAccess;
		}
		if(dcacheAccess - prevdcacheAccess > 0){
			dcacheAccessCycle++;
			prevdcacheAccess = dcacheAccess;
		}
		if(dcache2Access - prevdcache2Access > 0){
			dcache2AccessCycle++;
			prevdcache2Access = dcache2Access;
		}
		if(ialuAccess - previaluAccess > 0){
			ialuAccessCycle++;
			previaluAccess = ialuAccess;
		}
		if(faluAccess - prevfaluAccess > 0){
			faluAccessCycle++;
			prevfaluAccess = faluAccess;
		}
		if(resultbusAccess - prevresultbusAccess > 0){
			resultbusAccessCycle++;
			prevresultbusAccess = resultbusAccess;
		}
	}
	
	/**
	 * Call this function at the end of the simulation to compute the total energy dissipation
	 *  -> Total energy = num of accesses * per unit access energy 
	 *  -> for idle cycles, turnofffactor * num idle cycles * per unit access energy also added
	 * */
	
	public void updatePowerPeriodically(long totalCycles){
		
			int clockGatingStyle=SystemConfig.clockGatingStyle;
			double temp=0;
			switch(clockGatingStyle){
			case 0:
				renamePower+=PowerConfig.renamePower*renameAccess;
				bpredPower+=(double)bpredAccessCycle * PowerConfig.bpredPower;
			    temp = max(0,bpredAccess - bpredAccessCycle*2);
				bpredPower+=((double)temp/2.0) * PowerConfig.bpredPower;
				windowPower+=((double)windowPregAccessCycle)*PowerConfig.rsPower;
			    temp = max(0,windowPregAccess - windowPregAccessCycle*3*PowerConfig.ruuIssueWidth);
			    windowPower+=((double)temp/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		        windowPower+=((double)windowSelectionAccessCycle)*PowerConfig.selection;
			    temp = max(0,windowSelectionAccess - windowSelectionAccessCycle*PowerConfig.ruuIssueWidth);
			    windowPower+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
			    windowPower+=((double)windowWakeupAccessCycle)*PowerConfig.wakeupPower;
			    temp = max(0,windowWakeupAccess - windowWakeupAccessCycle*PowerConfig.ruuIssueWidth);
			    windowPower+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
			    lsqPower+=(double)lsqWakeupAccessCycle*PowerConfig.lsqWakeupPower;
			    temp = max(0,lsqWakeupAccess - lsqWakeupAccessCycle*PowerConfig.resMemport);
			    lsqPower+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			    lsqPower+=(double)lsqPregAccessCycle*PowerConfig.lsqWakeupPower;
				temp = max(0,lsqPregAccess - lsqPregAccessCycle*PowerConfig.resMemport);
				lsqPower+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
				regfilePower+=((double)IntegerRegfileAccessCycle)*PowerConfig.regfilePower;
				temp = max(0,IntegerRegfileAccess - IntegerRegfileAccessCycle*3*PowerConfig.ruuCommitWidth);
				regfilePower+=((double)temp/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
				icachePower+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
				dcachePower+=((double)dcacheAccessCycle)*(PowerConfig.dcachePower+PowerConfig.dtlb);
				temp = max(0,dcacheAccess - dcacheAccessCycle*PowerConfig.dl1Port);
		        dcachePower+=((double)temp/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
		        dcache2Power+=((double)dcache2AccessCycle)*PowerConfig.dcache2Power;
				temp = max(0,dcache2Access - dcache2AccessCycle*PowerConfig.dl2Port);
		        dcache2Power+=((double)temp/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		        aluPower+=PowerConfig.ialuPower*ialuAccessCycle;
		        aluPower+=PowerConfig.faluPower*faluAccessCycle;
		        resultbusPower+=PowerConfig.resultbus*resultbusAccessCycle;
			    temp = max(0,resultbusAccess - PowerConfig.ruuIssueWidth*resultbusAccessCycle);
			    resultbusPower+=((double)temp/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
			               
			    break;
			case 1:
				renamePower+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
				bpredPower+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
				windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
				windowPower+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
				windowPower+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
				lsqPower+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			    lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
			    regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
			    icachePower+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
			    dcachePower+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
			    dcache2Power+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
			    aluPower+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
				        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
			    resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
			      
				break;
			default:
				renamePower+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
			    renamePower+=turnoffFactor*PowerConfig.renamePower*(totalCycles - renameAccessCycle);
			
			bpredPower+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
			bpredPower+=turnoffFactor*PowerConfig.bpredPower*(totalCycles - bpredAccessCycle);
			windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
			windowPower+=turnoffFactor*PowerConfig.rsPower*(totalCycles - windowPregAccessCycle);
			 windowPower+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
			    windowPower+=turnoffFactor*PowerConfig.selection*(totalCycles - windowSelectionAccessCycle);
			
			    windowPower+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
			    windowPower+=turnoffFactor*PowerConfig.wakeupPower*(totalCycles - windowWakeupAccessCycle);
			
			    lsqPower+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			    lsqPower+=turnoffFactor*PowerConfig.lsqWakeupPower*(totalCycles - lsqWakeupAccessCycle);
				lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
				lsqPower+=turnoffFactor*PowerConfig.lsqRsPower*(totalCycles-lsqPregAccessCycle);
				regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
			    regfilePower+=turnoffFactor*PowerConfig.regfilePower*(totalCycles-IntegerRenameAccessCycle);
			      icachePower+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
			      icachePower+=(totalCycles - icacheAccessCycle)*turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);
			
				  dcachePower+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
			      dcachePower+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb)*(totalCycles - dcacheAccessCycle);
			
				  dcache2Power+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
			      dcache2Power+=turnoffFactor*PowerConfig.dcache2Power*(totalCycles - dcache2AccessCycle);
			
			  	  aluPower+=turnoffFactor*PowerConfig.ialuPower*(totalCycles - ialuAccessCycle);
				  aluPower+=turnoffFactor*PowerConfig.faluPower*(totalCycles - faluAccessCycle);
			        aluPower+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
					        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
			       resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
			      resultbusPower+=turnoffFactor*PowerConfig.resultbus*(totalCycles - resultbusAccessCycle);

				break;
			}
		    
//		  #ifdef STATICAF
/*		  #elif defined(DYNAMICAF)
		    if(windowPregAccess) {
		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
		        windowPower+=PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline;
		      else
		        windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		    }
		    else
		      windowPower+=turnoffFactor*PowerConfig.rsPower;
		  #else
		    panic("no AF-style defined\n");
		  #endif
*/

		   
//		  #ifdef STATICAF
		    
/*		  #else
		    if(lsqPregAccess) {
		      if(lsqPregAccess <= PowerConfig.resMemport)
		        lsqPower+=PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline;
		      else
		        lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		    }
		    else
		      lsqPower+=turnoffFactor*PowerConfig.lsqRsPower;
		  #endif
*/
//		  #ifdef STATICAF

		    
/*		  #else
		    if(IntegerRegfileAccess) {
		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
		        regfilePower+=PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline;
		      else
		        regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		    }
		    else
		      regfilePower+=turnoffFactor*PowerConfig.regfilePower;
		  #endif
*/
/*		  #else
		    if(resultbusAccess) {
		      assert(PowerConfig.ruuIssueWidth != 0);
		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
		        resultbusPower+=resultbusAfB*PowerConfig.resultbus;
		      }
		      else {
		        resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      }
		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		    }
		    else
		      resultbusPower+=turnoffFactor*PowerConfig.resultbus;
		  #endif
*/
		      
		    totalCyclePower = (PowerConfig.renamePower + PowerConfig.bpredPower + PowerConfig.windowPower + 
		    		PowerConfig.lsqPower + PowerConfig.regfilePower + PowerConfig.icachePower + PowerConfig.dcachePower +
		    		PowerConfig.ialuPower + PowerConfig.faluPower +PowerConfig.resultbus);

		    totalCyclePower = renamePower + bpredPower + 
		      windowPower + lsqPower + regfilePower + 
		      icachePower + dcachePower + aluPower + 
		      resultbusPower;

		    clockPower=PowerConfig.clockPower*(totalCyclePower/totalCyclePower);
	
//		    System.out.println("Total Cycle power = "+totalCyclePower + "Config power = "+PowerConfig.clockPower);
		}
	
	public void updatePowerAfterCompletion(long totalCycles){
	//	System.out.println("Total Cycles = "+totalCycles);
		int clockGatingStyle=SystemConfig.clockGatingStyle;
		double temp=0;
		switch(clockGatingStyle){
			case 0:
			    totalRenamePower+=PowerConfig.renamePower*totalRenameAccess;
			    totalBpredPower+=(double)totalBpredAccessCycle * PowerConfig.bpredPower;
			    temp = max(0,totalBpredAccess - totalBpredAccessCycle*2);
			    totalBpredPower+=((double)temp/2.0) * PowerConfig.bpredPower;
			    totalWindowPower+=((double)totalWindowPregAccessCycle)*PowerConfig.rsPower;
			    temp = max(0,totalWindowPregAccess - totalWindowPregAccessCycle*3*PowerConfig.ruuIssueWidth);
			    totalWindowPower+=((double)temp/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
			    totalWindowPower+=((double)totalWindowSelectionAccessCycle)*PowerConfig.selection;
			    temp = max(0,totalWindowSelectionAccess - totalWindowSelectionAccessCycle*PowerConfig.ruuIssueWidth);
			    totalWindowPower+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
			    totalWindowPower+=((double)totalWindowWakeupAccessCycle)*PowerConfig.wakeupPower;
			    temp = max(0,totalWindowWakeupAccess - totalWindowWakeupAccessCycle*PowerConfig.ruuIssueWidth);
			    totalWindowPower+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
			    totalLsqPower+=(double)totalLsqWakeupAccessCycle*PowerConfig.lsqWakeupPower;
			    temp = max(0,totalLsqWakeupAccess - totalLsqWakeupAccessCycle*PowerConfig.resMemport);
			    totalLsqPower+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			    totalLsqPower+=(double)totalLsqPregAccessCycle*PowerConfig.lsqWakeupPower;
				temp = max(0,totalLsqPregAccess - totalLsqPregAccessCycle*PowerConfig.resMemport);
				totalLsqPower+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
				totalRegfilePower+=((double)totalIntegerRegfileAccessCycle)*PowerConfig.regfilePower;
				temp = max(0,totalIntegerRegfileAccess - totalIntegerRegfileAccessCycle*3*PowerConfig.ruuCommitWidth);
				totalRegfilePower+=((double)temp/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
				totalIcachePower+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
				totalDcachePower+=((double)totalDcacheAccessCycle)*(PowerConfig.dcachePower+PowerConfig.dtlb);
				  temp = max(0,totalDcacheAccess - totalDcacheAccessCycle*PowerConfig.dl1Port);
				  totalDcachePower+=((double)temp/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
				  totalDcache2Power+=((double)totalDcache2AccessCycle)*PowerConfig.dcache2Power;
				  temp = max(0,totalDcache2Access - totalDcache2AccessCycle*PowerConfig.dl2Port);
				  totalDcache2Power+=((double)temp/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
				  totalAluPower+=PowerConfig.ialuPower*totalIaluAccessCycle;
				  totalAluPower+=PowerConfig.faluPower*totalFaluAccessCycle;
				  totalResultbusPower+=PowerConfig.resultbus*totalResultbusAccessCycle;
			      temp = max(0,totalResultbusAccess - PowerConfig.ruuIssueWidth*totalResultbusAccessCycle);
			      totalResultbusPower+=((double)temp/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
			      
			    break;
		case 1:
			totalRenamePower+=((double)totalRenameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
			totalBpredPower+=((double)totalBpredAccess/2.0) * PowerConfig.bpredPower;
			totalWindowPower+=((double)totalWindowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
			totalWindowPower+=((double)totalWindowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
			totalWindowPower+=((double)totalWindowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
			totalLsqPower+=((double)totalLsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			totalLsqPower+=((double)totalLsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
			totalRegfilePower+=((double)totalIntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
			totalIcachePower+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
			totalDcachePower+=((double)totalDcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
			totalDcache2Power+=((double)totalDcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
			totalAluPower+=((double)totalIaluAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
			        ((double)totalFaluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
			 totalResultbusPower+=((double)totalResultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		             
			break;
		default:
		    totalRenamePower+=((double)totalRenameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		    totalRenamePower+=turnoffFactor*PowerConfig.renamePower*(totalCycles - totalRenameAccessCycle);

		    totalBpredPower+=((double)totalBpredAccess/2.0) * PowerConfig.bpredPower;
		    totalBpredPower+=turnoffFactor*PowerConfig.bpredPower*(totalCycles - totalBpredAccessCycle);

		    totalWindowPower+=((double)totalWindowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    totalWindowPower+=turnoffFactor*PowerConfig.rsPower*(totalCycles - totalWindowPregAccessCycle);
		    totalWindowPower+=((double)totalWindowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    totalWindowPower+=turnoffFactor*PowerConfig.selection*(totalCycles - totalWindowSelectionAccessCycle);

		    totalWindowPower+=((double)totalWindowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    totalWindowPower+=turnoffFactor*PowerConfig.wakeupPower*(totalCycles - totalWindowWakeupAccessCycle);

		    totalLsqPower+=((double)totalLsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    totalLsqPower+=turnoffFactor*PowerConfig.lsqWakeupPower*(totalCycles - totalLsqWakeupAccessCycle);

		    totalLsqPower+=((double)totalLsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
			totalLsqPower+=turnoffFactor*PowerConfig.lsqRsPower*(totalCycles-totalLsqPregAccessCycle);
			totalRegfilePower+=((double)totalIntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
			totalRegfilePower+=turnoffFactor*PowerConfig.regfilePower*(totalCycles-totalIntegerRenameAccessCycle);
			totalIcachePower+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
			totalIcachePower+=(totalCycles - totalIcacheAccessCycle)*turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);

			  totalDcachePower+=((double)totalDcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
			  totalDcachePower+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb)*(totalCycles - totalDcacheAccessCycle);

			  totalDcache2Power+=((double)totalDcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      totalDcache2Power+=turnoffFactor*PowerConfig.dcache2Power*(totalCycles - totalDcache2AccessCycle);

		      totalAluPower+=turnoffFactor*PowerConfig.ialuPower*(totalCycles - totalIaluAccessCycle);
		      totalAluPower+=turnoffFactor*PowerConfig.faluPower*(totalCycles - totalFaluAccessCycle);

		      totalAluPower+=((double)totalIaluAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)totalFaluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;

		      totalResultbusPower+=((double)totalResultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      totalResultbusPower+=turnoffFactor*PowerConfig.resultbus*(totalCycles - totalResultbusAccessCycle);
			break;
		}
				      
				    totalCyclePower = (PowerConfig.renamePower + PowerConfig.bpredPower + PowerConfig.windowPower + 
				    		PowerConfig.lsqPower + PowerConfig.regfilePower + PowerConfig.icachePower + PowerConfig.dcachePower +
				    		PowerConfig.ialuPower + PowerConfig.faluPower +PowerConfig.resultbus);

				    totalCyclePower = totalRenamePower + totalBpredPower + 
				      totalWindowPower + totalLsqPower + totalRegfilePower + 
				      totalIcachePower + totalDcachePower + totalAluPower + 
				      totalResultbusPower;
				  
				    totalClockPower=PowerConfig.clockPower*(totalCyclePower/totalCyclePower);
			
				}
			

/*	public void updatePowerStatsPerCycle(){
		
//		  #ifdef DYNAMICAF
//		    windowAfB = computeAf(windowNumPopCountCycle,windowTotalPopCountCycle,dataWidth);
//		    lsqAfB = computeAf(lsqNumPopCountCycle,lsqTotalPopCountCycle,dataWidth);
//		    regfileAfB = computeAf(regfileNumPopCountCycle,regfileTotalPopCountCycle,dataWidth);
//		    resultbusAfB = computeAf(resultbusNumPopCountCycle,resultbusTotalPopCountCycle,dataWidth);
//		  #endif
//		    
		    renamePower+=PowerConfig.renamePower;
		    bpredPower+=PowerConfig.bpredPower;
		    windowPower+=PowerConfig.windowPower;
		    lsqPower+=PowerConfig.lsqPower;
		    regfilePower+=PowerConfig.regfilePower;
		    icachePower+=PowerConfig.icachePower+PowerConfig.itlb;
		    dcachePower+=PowerConfig.dcachePower+PowerConfig.dtlb;
		    dcache2Power+=PowerConfig.dcache2Power;
		    aluPower+=PowerConfig.ialuPower + PowerConfig.faluPower;
		    resultbusPower+=PowerConfig.resultbus;
		    clockPower+=PowerConfig.clockPower;

		    totalRenameAccess+=renameAccess;
		    totalBpredAccess+=bpredAccess;
		    totalWindowAccess+=windowAccess;
		    totalLsqAccess+=lsqAccess;
		    totalRegfileAccess+=regfileAccess;
		    
		    totalIntegerRegfileAccess+=IntegerRegfileAccess;
		    totalIntegerRenameAccess+=IntegerRenameAccess;
		    
		    totalIcacheAccess+=icacheAccess;
		    totalDcacheAccess+=dcacheAccess;
		    totalDcache2Access+=dcache2Access;
		    totalAluAccess+=aluAccess;
		    totalResultbusAccess+=resultbusAccess;

		    maxRenameAccess=max(renameAccess,maxRenameAccess);
		    maxBpredAccess=max(bpredAccess,maxBpredAccess);
		    maxWindowAccess=max(windowAccess,maxWindowAccess);
		    maxLsqAccess=max(lsqAccess,maxLsqAccess);
		    maxRegfileAccess=max(regfileAccess,maxRegfileAccess);
		    maxIcacheAccess=max(icacheAccess,maxIcacheAccess);
		    maxDcacheAccess=max(dcacheAccess,maxDcacheAccess);
		    maxDcache2Access=max(dcache2Access,maxDcache2Access);
		    maxAluAccess=max(aluAccess,maxAluAccess);
		    maxResultbusAccess=max(resultbusAccess,maxResultbusAccess);

		    maxIntegerRenameAccess=max(IntegerRenameAccess,maxIntegerRenameAccess);
		    maxFloatRenameAccess=max(FloatRenameAccess,maxFloatRenameAccess);

		    maxIntegerRegfileAccess=max(IntegerRenameAccess,maxIntegerRegfileAccess);
		    maxFloatRegfileAccess=max(FloatRenameAccess,maxFloatRegfileAccess);

		    if(renameAccess>0) {
		      renamePower+=PowerConfig.renamePower;
		      renamePower+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		      renamePower+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		    }
		    else 
		      renamePower+=turnoffFactor*PowerConfig.renamePower;

		    if(bpredAccess>0) {
		      if(bpredAccess <= 2)
		        bpredPower+=PowerConfig.bpredPower;
		      else
		        bpredPower+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		      bpredPower+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		      bpredPower+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		    }
		    else
		      bpredPower+=turnoffFactor*PowerConfig.bpredPower;

//		  #ifdef STATICAF
		    if(windowPregAccess>0) {
		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
		        windowPower+=PowerConfig.rsPower;
		      else
		        windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    }
		    else
		      windowPower+=turnoffFactor*PowerConfig.rsPower;
//		  #elif defined(DYNAMICAF)
//		    if(windowPregAccess) {
//		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
//		        windowPower+=PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline;
//		      else
//		        windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
//		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
//		      windowPower+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
//		    }
//		    else
//		      windowPower+=turnoffFactor*PowerConfig.rsPower;
//		  #else
//		    panic("no AF-style defined\n");
//		  #endif
//
		    if(windowSelectionAccess>0) {
		      if(windowSelectionAccess <= PowerConfig.ruuIssueWidth)
		        windowPower+=PowerConfig.selection;
		      else
		        windowPower+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		      windowPower+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		      windowPower+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    }
		    else
		      windowPower+=turnoffFactor*PowerConfig.selection;

		    if(windowWakeupAccess>0) {
		      if(windowWakeupAccess <= PowerConfig.ruuIssueWidth)
		        windowPower+=PowerConfig.wakeupPower;
		      else
		        windowPower+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		      windowPower+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		      windowPower+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    }
		    else
		      windowPower+=turnoffFactor*PowerConfig.wakeupPower;

		    if(lsqWakeupAccess>0) {
		      if(lsqWakeupAccess <= PowerConfig.resMemport)
		        lsqPower+=PowerConfig.lsqWakeupPower;
		      else
		        lsqPower+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		      lsqPower+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		      lsqPower+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    }
		    else
		      lsqPower+=turnoffFactor*PowerConfig.lsqWakeupPower;

//		  #ifdef STATICAF
		    if(lsqPregAccess>0) {
		      if(lsqPregAccess <= PowerConfig.resMemport)
		        lsqPower+=PowerConfig.lsqRsPower;
		      else
		        lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		    }
		    else
		      lsqPower+=turnoffFactor*PowerConfig.lsqRsPower;
//		  #else
//		    if(lsqPregAccess) {
//		      if(lsqPregAccess <= PowerConfig.resMemport)
//		        lsqPower+=PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline;
//		      else
//		        lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
//		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
//		      lsqPower+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
//		    }
//		    else
//		      lsqPower+=turnoffFactor*PowerConfig.lsqRsPower;
//		  #endif
//
//		  #ifdef STATICAF
		    if(IntegerRegfileAccess>0) {
		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
		        regfilePower+=PowerConfig.regfilePower;
		      else
		        regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		    }
		    else
		      regfilePower+=turnoffFactor*PowerConfig.regfilePower;
//		  #else
//		    if(IntegerRegfileAccess) {
//		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
//		        regfilePower+=PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline;
//		      else
//		        regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
//		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
//		      regfilePower+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
//		    }
//		    else
//		      regfilePower+=turnoffFactor*PowerConfig.regfilePower;
//		  #endif
//
		    if(icacheAccess>0) {
		      icachePower+=PowerConfig.icachePower+PowerConfig.itlb;
		      icachePower+=PowerConfig.icachePower+PowerConfig.itlb;
		      icachePower+=PowerConfig.icachePower+PowerConfig.itlb;
		    }
		    else
		      icachePower+=turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);

		    if(dcacheAccess>0) {
		      if(dcacheAccess <= PowerConfig.dl1Port)
		        dcachePower+=PowerConfig.dcachePower+PowerConfig.dtlb;
		      else
		        dcachePower+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						     PowerConfig.dtlb);
		      dcachePower+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						   PowerConfig.dtlb);
		      dcachePower+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						   PowerConfig.dtlb);
		    }
		    else
		      dcachePower+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb);

		    if(dcache2Access>0) {
		      if(dcache2Access <= PowerConfig.dl2Port)
		        dcache2Power+=PowerConfig.dcache2Power;
		      else
		        dcache2Power+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2Power+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2Power+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		    }
		    else
		      dcache2Power+=turnoffFactor*PowerConfig.dcache2Power;

		    if(aluAccess>0) {
		      if(ialuAccess>0)
		        aluPower+=PowerConfig.ialuPower;
		      else
		        aluPower+=turnoffFactor*PowerConfig.ialuPower;
		      if(faluAccess>0)
		        aluPower+=PowerConfig.faluPower;
		      else
		        aluPower+=turnoffFactor*PowerConfig.faluPower;

		      aluPower+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
		      aluPower+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
		    }
		    else
		      aluPower+=turnoffFactor*(PowerConfig.ialuPower + PowerConfig.faluPower);

//		  #ifdef STATICAF
		    if(resultbusAccess>0) {
		      assert(PowerConfig.ruuIssueWidth != 0);
		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
		        resultbusPower+=PowerConfig.resultbus;
		      }
		      else {
		        resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      }
		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		    }
		    else
		      resultbusPower+=turnoffFactor*PowerConfig.resultbus;
//		  #else
//		    if(resultbusAccess) {
//		      assert(PowerConfig.ruuIssueWidth != 0);
//		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
//		        resultbusPower+=resultbusAfB*PowerConfig.resultbus;
//		      }
//		      else {
//		        resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
//		      }
//		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
//		      resultbusPower+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
//		    }
//		    else
//		      resultbusPower+=turnoffFactor*PowerConfig.resultbus;
//		  #endif
//
		    totalCyclePower = renamePower + bpredPower + windowPower + 
		      lsqPower + regfilePower + icachePower + dcachePower +
		      aluPower + resultbusPower;

		    totalCyclePower = renamePower + bpredPower + 
		      windowPower + lsqPower + regfilePower + 
		      icachePower + dcachePower + aluPower + 
		      resultbusPower;

		    totalCyclePower = renamePower + bpredPower + 
		      windowPower + lsqPower + regfilePower + 
		      icachePower + dcachePower + aluPower + 
		      resultbusPower;

		    totalCyclePower = renamePower + bpredPower + 
		      windowPower + lsqPower + regfilePower + 
		      icachePower + dcachePower + aluPower + 
		      resultbusPower;

		    clockPower+=PowerConfig.clockPower*(totalCyclePower/totalCyclePower);
		    clockPower+=PowerConfig.clockPower*(totalCyclePower/totalCyclePower);
		    clockPower+=PowerConfig.clockPower*(totalCyclePower/totalCyclePower);

		    totalCyclePower += clockPower;
		    totalCyclePower += clockPower;
		    totalCyclePower += clockPower;

		    currentTotalCyclePower = totalCyclePower
		      -lastSingleTotalCyclePower;
		    currentTotalCyclePower = totalCyclePower
		      -lastSingleTotalCyclePower;
		    currentTotalCyclePower = totalCyclePower
		      -lastSingleTotalCyclePower;

		    maxCyclePower = max(maxCyclePower,currentTotalCyclePower);
		    maxCyclePower = max(maxCyclePower,currentTotalCyclePower);
		    maxCyclePower = max(maxCyclePower,currentTotalCyclePower);

		    lastSingleTotalCyclePower = totalCyclePower;
		    lastSingleTotalCyclePower = totalCyclePower;
		    lastSingleTotalCyclePower = totalCyclePower;

	}
	
*/
	private static long max(long renameAccess, long maxRenameAccess) {
		// TODO Auto-generated method stub
		if(renameAccess > maxRenameAccess)
			return renameAccess;
		else
			return maxRenameAccess;
	}
	private static double max(double renameAccess, double maxRenameAccess) {
		// TODO Auto-generated method stub
		if(renameAccess > maxRenameAccess)
			return renameAccess;
		else
			return maxRenameAccess;
	}

	public long getDataWidth() {
		return dataWidth;
	}

	public void setDataWidth(long dataWidth) {
		this.dataWidth = dataWidth;
	}

	public long getIntegerRenameAccess() {
		return renameAccess;
	}
	public long getFloatRenameAccess() {
		return renameAccess;
	}

	public void incrementRenameAccess(long renameAccess) {
		this.renameAccess += renameAccess;
	}

	public long getBpredAccess() {
		return bpredAccess;
	}

	public void incrementBpredAccess(long bpredAccess) {
		this.bpredAccess += bpredAccess;
	}

	public long getWindowAccess() {
		return windowAccess;
	}

	public void incrementWindowAccess(long windowAccess) {
		this.windowAccess += windowAccess;
	}

	public long getLsqAccess() {
		return lsqAccess;
	}

	public void incrementLsqAccess(long lsqAccess) {
		this.lsqAccess += lsqAccess;
	}

	public long getRegfileAccess() {
		return regfileAccess;
	}

	public void incrementRegfileAccess(long regfileAccess) {
		this.regfileAccess += regfileAccess;
	}

	public long getIcacheAccess() {
		return icacheAccess;
	}

	public void incrementIcacheAccess(long icacheAccess) {
		this.icacheAccess += icacheAccess;
	}

	public long getDcacheAccess() {
		return dcacheAccess;
	}

	public void incrementDcacheAccess(long dcacheAccess) {
		this.dcacheAccess += dcacheAccess;
	}

	public long getDcache2Access() {
		return dcache2Access;
	}

	public static void incrementDcache2Access(long dcache2access) {
		dcache2Access += dcache2access;
	}

	public long getAluAccess() {
		return aluAccess;
	}

	public void incrementAluAccess(long aluAccess) {
		this.aluAccess += aluAccess;
	}

	public long getIaluAccess() {
		return ialuAccess;
	}

	public void incrementIaluAccess(long ialuAccess) {
		this.ialuAccess += ialuAccess;
	}

	public long getFaluAccess() {
		return faluAccess;
	}

	public void incrementFaluAccess(long faluAccess) {
		this.faluAccess += faluAccess;
	}

	public long getResultbusAccess() {
		return resultbusAccess;
	}

	public void incrementResultbusAccess(long resultbusAccess) {
		this.resultbusAccess += resultbusAccess;
	}

	public long getWindowPregAccess() {
		return windowPregAccess;
	}

	public void incrementWindowPregAccess(long windowPregAccess) {
		this.windowPregAccess += windowPregAccess;
	}

	public long getWindowSelectionAccess() {
		return windowSelectionAccess;
	}

	public void incrementWindowSelectionAccess(long windowSelectionAccess) {
		this.windowSelectionAccess += windowSelectionAccess;
	}

	public long getWindowWakeupAccess() {
		return windowWakeupAccess;
	}

	public void incrementWindowWakeupAccess(long windowWakeupAccess) {
		this.windowWakeupAccess += windowWakeupAccess;
	}

	public long getLsqStoreDataAccess() {
		return lsqStoreDataAccess;
	}

	public void incrementLsqStoreDataAccess(long lsqStoreDataAccess) {
		this.lsqStoreDataAccess += lsqStoreDataAccess;
	}

	public long getLsqLoadDataAccess() {
		return lsqLoadDataAccess;
	}

	public void incrementLsqLoadDataAccess(long lsqLoadDataAccess) {
		this.lsqLoadDataAccess += lsqLoadDataAccess;
	}

	public long getLsqPregAccess() {
		return lsqPregAccess;
	}

	public void incrementLsqPregAccess(long lsqPregAccess) {
		this.lsqPregAccess += lsqPregAccess;
	}

	public long getLsqWakeupAccess() {
		return lsqWakeupAccess;
	}

	public void incrementLsqWakeupAccess(long lsqWakeupAccess) {
		this.lsqWakeupAccess += lsqWakeupAccess;
	}

	public long getWindowTotalPopCountCycle() {
		return windowTotalPopCountCycle;
	}

	public void setWindowTotalPopCountCycle(long windowTotalPopCountCycle) {
		this.windowTotalPopCountCycle = windowTotalPopCountCycle;
	}

	public long getWindowNumPopCountCycle() {
		return windowNumPopCountCycle;
	}

	public void setWindowNumPopCountCycle(long windowNumPopCountCycle) {
		this.windowNumPopCountCycle = windowNumPopCountCycle;
	}

	public long getLsqTotalPopCountCycle() {
		return lsqTotalPopCountCycle;
	}

	public void setLsqTotalPopCountCycle(long lsqTotalPopCountCycle) {
		this.lsqTotalPopCountCycle = lsqTotalPopCountCycle;
	}

	public long getLsqNumPopCountCycle() {
		return lsqNumPopCountCycle;
	}

	public void setLsqNumPopCountCycle(long lsqNumPopCountCycle) {
		this.lsqNumPopCountCycle = lsqNumPopCountCycle;
	}

	public long getRegfileTotalPopCountCycle() {
		return regfileTotalPopCountCycle;
	}

	public void setRegfileTotalPopCountCycle(long regfileTotalPopCountCycle) {
		this.regfileTotalPopCountCycle = regfileTotalPopCountCycle;
	}

	public long getRegfileNumPopCountCycle() {
		return regfileNumPopCountCycle;
	}

	public void setRegfileNumPopCountCycle(long regfileNumPopCountCycle) {
		this.regfileNumPopCountCycle = regfileNumPopCountCycle;
	}

	public long getResultbusTotalPopCountCycle() {
		return resultbusTotalPopCountCycle;
	}

	public void setResultbusTotalPopCountCycle(long resultbusTotalPopCountCycle) {
		this.resultbusTotalPopCountCycle = resultbusTotalPopCountCycle;
	}

	public long getResultbusNumPopCountCycle() {
		return resultbusNumPopCountCycle;
	}

	public void setResultbusNumPopCountCycle(long resultbusNumPopCountCycle) {
		this.resultbusNumPopCountCycle = resultbusNumPopCountCycle;
	}

	public long getTotalRenameAccess() {
		return totalRenameAccess;
	}

	public void setTotalRenameAccess(long totalRenameAccess) {
		this.totalRenameAccess = totalRenameAccess;
	}

	public long getTotalBpredAccess() {
		return totalBpredAccess;
	}

	public void setTotalBpredAccess(long totalBpredAccess) {
		this.totalBpredAccess += totalBpredAccess;
	}

	public long getTotalWindowAccess() {
		return totalWindowAccess;
	}

	public void setTotalWindowAccess(long totalWindowAccess) {
		this.totalWindowAccess += totalWindowAccess;
	}

	public long getTotalLsqAccess() {
		return totalLsqAccess;
	}

	public void setTotalLsqAccess(long totalLsqAccess) {
		this.totalLsqAccess += totalLsqAccess;
	}

	public long getTotalRegfileAccess() {
		return totalRegfileAccess;
	}

	public void setTotalRegfileAccess(long totalRegfileAccess) {
		this.totalRegfileAccess += totalRegfileAccess;
	}

	public long getTotalIcacheAccess() {
		return totalIcacheAccess;
	}

	public void setTotalIcacheAccess(long totalIcacheAccess) {
		this.totalIcacheAccess += totalIcacheAccess;
	}

	public long getTotalDcacheAccess() {
		return totalDcacheAccess;
	}

	public void setTotalDcacheAccess(long totalDcacheAccess) {
		this.totalDcacheAccess += totalDcacheAccess;
	}

	public long getTotalDcache2Access() {
		return totalDcache2Access;
	}

	public void setTotalDcache2Access(long totalDcache2Access) {
		this.totalDcache2Access += totalDcache2Access;
	}

	public long getTotalAluAccess() {
		return totalAluAccess;
	}

	public void setTotalAluAccess(long totalAluAccess) {
		this.totalAluAccess += totalAluAccess;
	}

	public long getTotalResultbusAccess() {
		return totalResultbusAccess;
	}

	public void setTotalResultbusAccess(long totalResultbusAccess) {
		this.totalResultbusAccess = totalResultbusAccess;
	}

	public long getMaxRenameAccess() {
		return maxRenameAccess;
	}

	public void setMaxRenameAccess(long maxRenameAccess) {
		this.maxRenameAccess = maxRenameAccess;
	}

	public long getMaxBpredAccess() {
		return maxBpredAccess;
	}

	public void setMaxBpredAccess(long maxBpredAccess) {
		this.maxBpredAccess = maxBpredAccess;
	}

	public long getMaxWindowAccess() {
		return maxWindowAccess;
	}

	public void setMaxWindowAccess(long maxWindowAccess) {
		this.maxWindowAccess = maxWindowAccess;
	}

	public long getMaxLsqAccess() {
		return maxLsqAccess;
	}

	public void setMaxLsqAccess(long maxLsqAccess) {
		this.maxLsqAccess = maxLsqAccess;
	}

	public long getMaxRegfileAccess() {
		return maxRegfileAccess;
	}

	public void setMaxRegfileAccess(long maxRegfileAccess) {
		this.maxRegfileAccess = maxRegfileAccess;
	}

	public long getMaxIcacheAccess() {
		return maxIcacheAccess;
	}

	public void setMaxIcacheAccess(long maxIcacheAccess) {
		this.maxIcacheAccess = maxIcacheAccess;
	}

	public long getMaxDcacheAccess() {
		return maxDcacheAccess;
	}

	public void setMaxDcacheAccess(long maxDcacheAccess) {
		this.maxDcacheAccess = maxDcacheAccess;
	}

	public long getMaxDcache2Access() {
		return maxDcache2Access;
	}

	public void setMaxDcache2Access(long maxDcache2Access) {
		this.maxDcache2Access = maxDcache2Access;
	}

	public long getMaxAluAccess() {
		return maxAluAccess;
	}

	public void setMaxAluAccess(long maxAluAccess) {
		this.maxAluAccess = maxAluAccess;
	}

	public long getMaxResultbusAccess() {
		return maxResultbusAccess;
	}

	public void setMaxResultbusAccess(long maxResultbusAccess) {
		this.maxResultbusAccess = maxResultbusAccess;
	}

	public double getRenamePower() {
		return renamePower;
	}

	public void setRenamePower(double renamePower) {
		this.renamePower = renamePower;
	}

	public double getBpredPower() {
		return bpredPower;
	}

	public void setBpredPower(double bpredPower) {
		this.bpredPower = bpredPower;
	}

	public double getWindowPower() {
		return windowPower;
	}

	public void setWindowPower(double windowPower) {
		this.windowPower = windowPower;
	}

	public double getLsqPower() {
		return lsqPower;
	}

	public void setLsqPower(double lsqPower) {
		this.lsqPower = lsqPower;
	}

	public double getRegfilePower() {
		return regfilePower;
	}

	public void setRegfilePower(double regfilePower) {
		this.regfilePower = regfilePower;
	}

	public double getIcachePower() {
		return icachePower;
	}

	public void setIcachePower(double icachePower) {
		this.icachePower = icachePower;
	}

	public double getDcachePower() {
		return dcachePower;
	}

	public void setDcachePower(double dcachePower) {
		this.dcachePower = dcachePower;
	}

	public double getDcache2Power() {
		return dcache2Power;
	}

	public void setDcache2Power(double dcache2Power) {
		this.dcache2Power = dcache2Power;
	}

	public double getAluPower() {
		return aluPower;
	}

	public void setAluPower(double aluPower) {
		this.aluPower = aluPower;
	}

	public double getResultbusPower() {
		return resultbusPower;
	}

	public void setResultbusPower(double resultbusPower) {
		this.resultbusPower = resultbusPower;
	}

	public double getClockPower() {
		return clockPower;
	}

	public void setClockPower(double clockPower) {
		this.clockPower = clockPower;
	}

	public double getTotalCyclePower() {
		return totalCyclePower;
	}

	public void setTotalCyclePower(double totalCyclePower) {
		this.totalCyclePower = totalCyclePower;
	}

	public double getLastSingleTotalCyclePower() {
		return lastSingleTotalCyclePower;
	}

	public void setLastSingleTotalCyclePower(double lastSingleTotalCyclePower) {
		this.lastSingleTotalCyclePower = lastSingleTotalCyclePower;
	}

	public double getCurrentTotalCyclePower() {
		return currentTotalCyclePower;
	}

	public void setCurrentTotalCyclePower(double currentTotalCyclePower) {
		this.currentTotalCyclePower = currentTotalCyclePower;
	}

	public double getMaxCyclePower() {
		return maxCyclePower;
	}

	public void setMaxCyclePower(double maxCyclePower) {
		this.maxCyclePower = maxCyclePower;
	}

	public double getTurnoffFactor() {
		return turnoffFactor;
	}

	public void setTurnoffFactor(double turnoffFactor) {
		this.turnoffFactor = turnoffFactor;
	}

	public void incrementIntegerRenameAccess(int renameAccess) {
		this.IntegerRenameAccess += renameAccess;
		
	}
	
	public void incrementFloatRenameAccess(int renameAccess) {
		this.FloatRenameAccess += renameAccess;
		
	}

	public void incrementIntegerRegfileAccess(int regfileAccess) {
		this.IntegerRegfileAccess += regfileAccess;
		
	}
	public void incrementFloatRegfileAccess(int regfileAccess) {
		this.FloatRegfileAccess += regfileAccess;
		
	}

	public long getTotalIntegerRegfileAccess() {
		return totalIntegerRegfileAccess;
	}

	public void setTotalIntegerRegfileAccess(long totalIntegerRegfileAccess) {
		this.totalIntegerRegfileAccess = totalIntegerRegfileAccess;
	}

	public long getTotalFloatRegfileAccess() {
		return totalFloatRegfileAccess;
	}

	public void setTotalFloatRegfileAccess(long totalFloatRegfileAccess) {
		this.totalFloatRegfileAccess = totalFloatRegfileAccess;
	}

	public long getTotalIntegerRenameAccess() {
		return totalIntegerRenameAccess;
	}

	public void setTotalIntegerRenameAccess(long totalIntegerRenameAccess) {
		this.totalIntegerRenameAccess = totalIntegerRenameAccess;
	}

	public long getTotalFloatRenameAccess() {
		return totalFloatRenameAccess;
	}

	public void setTotalFloatRenameAccess(long totalFloatRenameAccess) {
		this.totalFloatRenameAccess = totalFloatRenameAccess;
	}

	public long getRenameAccess() {
		return renameAccess;
	}

	public long getIntegerRegfileAccess() {
		return IntegerRegfileAccess;
	}

	public long getFloatRegfileAccess() {
		return FloatRegfileAccess;
	}
	public double getTotalRenamePower() {
		return totalRenamePower;
	}

	public void setTotalRenamePower(double totalRenamePower) {
		this.totalRenamePower = totalRenamePower;
	}

	public double getTotalBpredPower() {
		return totalBpredPower;
	}

	public void setTotalBpredPower(double totalBpredPower) {
		this.totalBpredPower = totalBpredPower;
	}

	public double getTotalWindowPower() {
		return totalWindowPower;
	}

	public void setTotalWindowPower(double totalWindowPower) {
		this.totalWindowPower = totalWindowPower;
	}

	public double getTotalLsqPower() {
		return totalLsqPower;
	}

	public void setTotalLsqPower(double totalLsqPower) {
		this.totalLsqPower = totalLsqPower;
	}

	public double getTotalRegfilePower() {
		return totaRegfilePower;
	}

	public void setTotalRegfilePower(double totaRegfilePower) {
		this.totaRegfilePower = totaRegfilePower;
	}

	public double getTotalIcachePower() {
		return totalIcachePower;
	}

	public void setTotalIcachePower(double totalIcachePower) {
		this.totalIcachePower = totalIcachePower;
	}

	public double getTotalDcachePower() {
		return totalDcachePower;
	}

	public void setTotalDcachePower(double totalDcachePower) {
		this.totalDcachePower = totalDcachePower;
	}

	public double getTotalDcache2Power() {
		return totalDcache2Power;
	}

	public void setTotalDcache2Power(double totalDcache2Power) {
		this.totalDcache2Power = totalDcache2Power;
	}

	public double getTotalResultbusPower() {
		return totalResultbusPower;
	}

	public void setTotalResultbusPower(double totalResultbusPower) {
		this.totalResultbusPower = totalResultbusPower;
	}


	public double getTotalClockPower() {
		return totalClockPower;
	}

	public void setTotalClockPower(double totalClockPower) {
		this.totalClockPower = totalClockPower;
	}

	public double getTotalAluPower() {
		return totalAluPower;
	}

	public void setTotalAluPower(double totalAluPower) {
		this.totalAluPower = totalAluPower;
	}

	public long getBpredMisses() {
		return bpredMisses;
	}

	public void incrementBpredMisses() {
		this.bpredMisses++;
	}
}