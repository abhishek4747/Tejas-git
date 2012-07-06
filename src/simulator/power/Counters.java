package power;

public class Counters {

	/* options for Wattch */
	long dataWidth;

	/* counters added for Wattch */
	long renameAccess=0;
	long bpredAccess=0;
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

	double renamePowerCC1=0;
	double bpredPowerCC1=0;
	double windowPowerCC1=0;
	double lsqPowerCC1=0;
	double regfilePowerCC1=0;
	double icachePowerCC1=0;
	double dcachePowerCC1=0;
	double dcache2PowerCC1=0;
	double aluPowerCC1=0;
	double resultbusPowerCC1=0;
	double clockPowerCC1=0;

	double renamePowerCC2=0;
	double bpredPowerCC2=0;
	double windowPowerCC2=0;
	double lsqPowerCC2=0;
	double regfilePowerCC2=0;
	double icachePowerCC2=0;
	double dcachePowerCC2=0;
	double dcache2PowerCC2=0;
	double aluPowerCC2=0;
	double resultbusPowerCC2=0;
	double clockPowerCC2=0;

	double renamePowerCC3=0;
	double bpredPowerCC3=0;
	double windowPowerCC3=0;
	double lsqPowerCC3=0;
	double regfilePowerCC3=0;
	double icachePowerCC3=0;
	double dcachePowerCC3=0;
	double dcache2PowerCC3=0;
	double aluPowerCC3=0;
	double resultbusPowerCC3=0;
	double clockPowerCC3=0;

	double totalRenamePower=0;
	double totalBpredPower=0;
	double totalWindowPower=0;
	double totalLsqPower=0;
	double totaRegfilePower=0;
	double totalIcachePower=0;
	double totalDcachePower=0;
	double totalDcache2Power=0;
	double totalAluPower=0;
	double totalResultbusPower=0;
	double totalClockPower=0;

	double totalRenamePowerCC1=0;
	double totalBpredPowerCC1=0;
	double totalWindowPowerCC1=0;
	double totalLsqPowerCC1=0;
	double totalRegfilePowerCC1=0;
	double totalIcachePowerCC1=0;
	double totalDcachePowerCC1=0;
	double totalDcache2PowerCC1=0;
	double totalAluPowerCC1=0;

	double totalResultbusPowerCC1=0;
	double totalClockPowerCC1=0;

	double totalRenamePowerCC2=0;
	double totalBpredPowerCC2=0;
	double totalWindowPowerCC2=0;
	double totalLsqPowerCC2=0;
	double totalRegfilePowerCC2=0;
	double totalIcachePowerCC2=0;
	double totalDcachePowerCC2=0;
	double totalDcache2PowerCC2=0;
	double totalAluPowerCC2=0;
	double totalResultbusPowerCC2=0;
	double totalClockPowerCC2=0;

	double totalRenamePowerCC3=0;
	double totalBpredPowerCC3=0;
	double totalWindowPowerCC3=0;
	double totalLsqPowerCC3=0;
	double totalRegfilePowerCC3=0;
	double totalIcachePowerCC3=0;
	double totalDcachePowerCC3=0;
	double totalDcache2PowerCC3=0;
	double totalAluPowerCC3=0;
	double totalResultbusPowerCC3=0;
	double totalClockPowerCC3=0;

	double totalCyclePower;
	double totalCyclePowerCC1;
	double totalCyclePowerCC2;
	double totalCyclePowerCC3;

	double lastSingleTotalCyclePowerCC1 = 0.0;
	double lastSingleTotalCyclePowerCC2 = 0.0;
	double lastSingleTotalCyclePowerCC3 = 0.0;
	double currentTotalCyclePowerCC1;
	double currentTotalCyclePowerCC2;
	double currentTotalCyclePowerCC3;

	double maxCyclePowerCC1 = 0.0;
	double maxCyclePowerCC2 = 0.0;
	double maxCyclePowerCC3 = 0.0;

	double turnoffFactor = 0.1;

	
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

		 renamePowerCC1=0;
		 bpredPowerCC1=0;
		 windowPowerCC1=0;
		 lsqPowerCC1=0;
		 regfilePowerCC1=0;
		 icachePowerCC1=0;
		 dcachePowerCC1=0;
		 dcache2PowerCC1=0;
		 aluPowerCC1=0;
		 resultbusPowerCC1=0;
		 clockPowerCC1=0;

		 renamePowerCC2=0;
		 bpredPowerCC2=0;
		 windowPowerCC2=0;
		 lsqPowerCC2=0;
		 regfilePowerCC2=0;
		 icachePowerCC2=0;
		 dcachePowerCC2=0;
		 dcache2PowerCC2=0;
		 aluPowerCC2=0;
		 resultbusPowerCC2=0;
		 clockPowerCC2=0;

		 renamePowerCC3=0;
		 bpredPowerCC3=0;
		 windowPowerCC3=0;
		 lsqPowerCC3=0;
		 regfilePowerCC3=0;
		 icachePowerCC3=0;
		 dcachePowerCC3=0;
		 dcache2PowerCC3=0;
		 aluPowerCC3=0;
		 resultbusPowerCC3=0;
		 clockPowerCC3=0;


		 lastSingleTotalCyclePowerCC1 = 0.0;
		 lastSingleTotalCyclePowerCC2 = 0.0;
		 lastSingleTotalCyclePowerCC3 = 0.0;


		 maxCyclePowerCC1 = 0.0;
		 maxCyclePowerCC2 = 0.0;
		 maxCyclePowerCC3 = 0.0;

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

		 renamePowerCC1=0;
		 bpredPowerCC1=0;
		 windowPowerCC1=0;
		 lsqPowerCC1=0;
		 regfilePowerCC1=0;
		 icachePowerCC1=0;
		 dcachePowerCC1=0;
		 dcache2PowerCC1=0;
		 aluPowerCC1=0;
		 resultbusPowerCC1=0;
		 clockPowerCC1=0;

		 renamePowerCC2=0;
		 bpredPowerCC2=0;
		 windowPowerCC2=0;
		 lsqPowerCC2=0;
		 regfilePowerCC2=0;
		 icachePowerCC2=0;
		 dcachePowerCC2=0;
		 dcache2PowerCC2=0;
		 aluPowerCC2=0;
		 resultbusPowerCC2=0;
		 clockPowerCC2=0;

		 renamePowerCC3=0;
		 bpredPowerCC3=0;
		 windowPowerCC3=0;
		 lsqPowerCC3=0;
		 regfilePowerCC3=0;
		 icachePowerCC3=0;
		 dcachePowerCC3=0;
		 dcache2PowerCC3=0;
		 aluPowerCC3=0;
		 resultbusPowerCC3=0;
		 clockPowerCC3=0;

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
	 * CC1 -> Total energy = num of accesses * per unit access energy 
	 * CC3 -> for idle cycles, turnofffactor * num idle cycles * per unit access energy also added
	 * */
	
	public void updatePowerPeriodically(long totalCycles){
		
		    renamePowerCC1+=PowerConfig.renamePower*renameAccess;
		    renamePowerCC2+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		    renamePowerCC3+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		    renamePowerCC3+=turnoffFactor*PowerConfig.renamePower*(totalCycles - renameAccessCycle);

			bpredPowerCC1+=(double)bpredAccessCycle * PowerConfig.bpredPower;
		    double temp = max(0,bpredAccess - bpredAccessCycle*2);
			bpredPowerCC1+=((double)temp/2.0) * PowerConfig.bpredPower;
			bpredPowerCC2+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
			bpredPowerCC3+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
			bpredPowerCC3+=turnoffFactor*PowerConfig.bpredPower*(totalCycles - bpredAccessCycle);

//		  #ifdef STATICAF
		    windowPowerCC1+=((double)windowPregAccessCycle)*PowerConfig.rsPower;
		    temp = max(0,windowPregAccess - windowPregAccessCycle*3*PowerConfig.ruuIssueWidth);
		    windowPowerCC1+=((double)temp/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    windowPowerCC2+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    windowPowerCC3+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    windowPowerCC3+=turnoffFactor*PowerConfig.rsPower*(totalCycles - windowPregAccessCycle);
/*		  #elif defined(DYNAMICAF)
		    if(windowPregAccess) {
		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
		        windowPowerCC1+=PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline;
		      else
		        windowPowerCC1+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPowerCC2+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPowerCC3+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		    }
		    else
		      windowPowerCC3+=turnoffFactor*PowerConfig.rsPower;
		  #else
		    panic("no AF-style defined\n");
		  #endif
*/

	        windowPowerCC1+=((double)windowSelectionAccessCycle)*PowerConfig.selection;
		    temp = max(0,windowSelectionAccess - windowSelectionAccessCycle*PowerConfig.ruuIssueWidth);
		    windowPowerCC1+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    windowPowerCC2+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    windowPowerCC3+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    windowPowerCC3+=turnoffFactor*PowerConfig.selection*(totalCycles - windowSelectionAccessCycle);

		    windowPowerCC1+=((double)windowWakeupAccessCycle)*PowerConfig.wakeupPower;
		    temp = max(0,windowWakeupAccess - windowWakeupAccessCycle*PowerConfig.ruuIssueWidth);
		    windowPowerCC1+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    windowPowerCC2+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    windowPowerCC3+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    windowPowerCC3+=turnoffFactor*PowerConfig.wakeupPower*(totalCycles - windowWakeupAccessCycle);

		    lsqPowerCC1+=(double)lsqWakeupAccessCycle*PowerConfig.lsqWakeupPower;
		    temp = max(0,lsqWakeupAccess - lsqWakeupAccessCycle*PowerConfig.resMemport);
		    lsqPowerCC1+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    lsqPowerCC2+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    lsqPowerCC3+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    lsqPowerCC3+=turnoffFactor*PowerConfig.lsqWakeupPower*(totalCycles - lsqWakeupAccessCycle);

//		  #ifdef STATICAF
		    
			lsqPowerCC1+=(double)lsqPregAccessCycle*PowerConfig.lsqWakeupPower;
			temp = max(0,lsqPregAccess - lsqPregAccessCycle*PowerConfig.resMemport);
			lsqPowerCC1+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
			lsqPowerCC2+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
			lsqPowerCC3+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
			lsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower*(totalCycles-lsqPregAccessCycle);
/*		  #else
		    if(lsqPregAccess) {
		      if(lsqPregAccess <= PowerConfig.resMemport)
		        lsqPowerCC1+=PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline;
		      else
		        lsqPowerCC1+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPowerCC2+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPowerCC3+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		    }
		    else
		      lsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower;
		  #endif
*/
//		  #ifdef STATICAF

			regfilePowerCC1+=((double)IntegerRegfileAccessCycle)*PowerConfig.regfilePower;
			temp = max(0,IntegerRegfileAccess - IntegerRegfileAccessCycle*3*PowerConfig.ruuCommitWidth);
			regfilePowerCC1+=((double)temp/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
			regfilePowerCC2+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		    regfilePowerCC3+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		    regfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower*(totalCycles-IntegerRenameAccessCycle);
		    
/*		  #else
		    if(IntegerRegfileAccess) {
		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
		        regfilePowerCC1+=PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline;
		      else
		        regfilePowerCC1+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePowerCC2+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePowerCC3+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		    }
		    else
		      regfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower;
		  #endif
*/
		      icachePowerCC1+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
		      icachePowerCC2+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
		      icachePowerCC3+=(icacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
		      icachePowerCC3+=(totalCycles - icacheAccessCycle)*turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);

   			  dcachePowerCC1+=((double)dcacheAccessCycle)*(PowerConfig.dcachePower+PowerConfig.dtlb);
			  temp = max(0,dcacheAccess - dcacheAccessCycle*PowerConfig.dl1Port);
	          dcachePowerCC1+=((double)temp/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
		      dcachePowerCC2+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
		      dcachePowerCC3+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
		      dcachePowerCC3+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb)*(totalCycles - dcacheAccessCycle);

   			  dcache2PowerCC1+=((double)dcache2AccessCycle)*PowerConfig.dcache2Power;
			  temp = max(0,dcache2Access - dcache2AccessCycle*PowerConfig.dl2Port);
	          dcache2PowerCC1+=((double)temp/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2PowerCC2+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2PowerCC3+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2PowerCC3+=turnoffFactor*PowerConfig.dcache2Power*(totalCycles - dcache2AccessCycle);

		  	  aluPowerCC1+=PowerConfig.ialuPower*ialuAccessCycle;
			  aluPowerCC3+=turnoffFactor*PowerConfig.ialuPower*(totalCycles - ialuAccessCycle);
			  aluPowerCC1+=PowerConfig.faluPower*faluAccessCycle;
			  aluPowerCC3+=turnoffFactor*PowerConfig.faluPower*(totalCycles - faluAccessCycle);

		      aluPowerCC2+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
		      aluPowerCC3+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;

//		  #ifdef STATICAF
		      resultbusPowerCC1+=PowerConfig.resultbus*resultbusAccessCycle;
		      temp = max(0,resultbusAccess - PowerConfig.ruuIssueWidth*resultbusAccessCycle);
		      resultbusPowerCC1+=((double)temp/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      resultbusPowerCC2+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      resultbusPowerCC3+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      resultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus*(totalCycles - resultbusAccessCycle);
/*		  #else
		    if(resultbusAccess) {
		      assert(PowerConfig.ruuIssueWidth != 0);
		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
		        resultbusPowerCC1+=resultbusAfB*PowerConfig.resultbus;
		      }
		      else {
		        resultbusPowerCC1+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      }
		      resultbusPowerCC2+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      resultbusPowerCC3+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		    }
		    else
		      resultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus;
		  #endif
*/
		      
		    totalCyclePower = (PowerConfig.renamePower + PowerConfig.bpredPower + PowerConfig.windowPower + 
		    		PowerConfig.lsqPower + PowerConfig.regfilePower + PowerConfig.icachePower + PowerConfig.dcachePower +
		    		PowerConfig.ialuPower + PowerConfig.faluPower +PowerConfig.resultbus);

		    totalCyclePowerCC1 = renamePowerCC1 + bpredPowerCC1 + 
		      windowPowerCC1 + lsqPowerCC1 + regfilePowerCC1 + 
		      icachePowerCC1 + dcachePowerCC1 + aluPowerCC1 + 
		      resultbusPowerCC1;

		    totalCyclePowerCC2 = renamePowerCC2 + bpredPowerCC2 + 
		      windowPowerCC2 + lsqPowerCC2 + regfilePowerCC2 + 
		      icachePowerCC2 + dcachePowerCC2 + aluPowerCC2 + 
		      resultbusPowerCC2;

		    totalCyclePowerCC3 = renamePowerCC3 + bpredPowerCC3 + 
		      windowPowerCC3 + lsqPowerCC3 + regfilePowerCC3 + 
		      icachePowerCC3 + dcachePowerCC3 + aluPowerCC3 + 
		      resultbusPowerCC3;

		    //FIXME Clock power calculation not correct!
		    clockPowerCC1=PowerConfig.clockPower*(totalCyclePowerCC1/totalCyclePower);
		    clockPowerCC2=PowerConfig.clockPower*(totalCyclePowerCC2/totalCyclePower);
		    clockPowerCC3=PowerConfig.clockPower*(totalCyclePowerCC3/totalCyclePower);
	
//		    System.out.println("Total Cycle power = "+totalCyclePower + "Config power = "+PowerConfig.clockPower);
		}
	
	public void updatePowerAfterCompletion(long totalCycles){
		
		System.out.println("Total Cycles = "+totalCycles);
				    totalRenamePowerCC1+=PowerConfig.renamePower*totalRenameAccess;
				    totalRenamePowerCC2+=((double)totalRenameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
				    totalRenamePowerCC3+=((double)totalRenameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
				    totalRenamePowerCC3+=turnoffFactor*PowerConfig.renamePower*(totalCycles - totalRenameAccessCycle);

				    totalBpredPowerCC1+=(double)totalBpredAccessCycle * PowerConfig.bpredPower;
				    double temp = max(0,totalBpredAccess - totalBpredAccessCycle*2);
				    totalBpredPowerCC1+=((double)temp/2.0) * PowerConfig.bpredPower;
				    totalBpredPowerCC2+=((double)totalBpredAccess/2.0) * PowerConfig.bpredPower;
				    totalBpredPowerCC3+=((double)totalBpredAccess/2.0) * PowerConfig.bpredPower;
				    totalBpredPowerCC3+=turnoffFactor*PowerConfig.bpredPower*(totalCycles - totalBpredAccessCycle);

//				  #ifdef STATICAF
				    totalWindowPowerCC1+=((double)totalWindowPregAccessCycle)*PowerConfig.rsPower;
				    temp = max(0,totalWindowPregAccess - totalWindowPregAccessCycle*3*PowerConfig.ruuIssueWidth);
				    totalWindowPowerCC1+=((double)temp/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
				    totalWindowPowerCC2+=((double)totalWindowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
				    totalWindowPowerCC3+=((double)totalWindowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
				    totalWindowPowerCC3+=turnoffFactor*PowerConfig.rsPower*(totalCycles - totalWindowPregAccessCycle);
		/*		  #elif defined(DYNAMICAF)
				    if(windowPregAccess) {
				      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
				        windowPowerCC1+=PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline;
				      else
				        windowPowerCC1+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
				      windowPowerCC2+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
				      windowPowerCC3+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
				    }
				    else
				      windowPowerCC3+=turnoffFactor*PowerConfig.rsPower;
				  #else
				    panic("no AF-style defined\n");
				  #endif
		*/

				    totalWindowPowerCC1+=((double)totalWindowSelectionAccessCycle)*PowerConfig.selection;
				    temp = max(0,totalWindowSelectionAccess - totalWindowSelectionAccessCycle*PowerConfig.ruuIssueWidth);
				    totalWindowPowerCC1+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
				    totalWindowPowerCC2+=((double)totalWindowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
				    totalWindowPowerCC3+=((double)totalWindowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
				    totalWindowPowerCC3+=turnoffFactor*PowerConfig.selection*(totalCycles - totalWindowSelectionAccessCycle);

				    totalWindowPowerCC1+=((double)totalWindowWakeupAccessCycle)*PowerConfig.wakeupPower;
				    temp = max(0,totalWindowWakeupAccess - totalWindowWakeupAccessCycle*PowerConfig.ruuIssueWidth);
				    totalWindowPowerCC1+=((double)temp/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
				    totalWindowPowerCC2+=((double)totalWindowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
				    totalWindowPowerCC3+=((double)totalWindowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
				    totalWindowPowerCC3+=turnoffFactor*PowerConfig.wakeupPower*(totalCycles - totalWindowWakeupAccessCycle);

				    totalLsqPowerCC1+=(double)totalLsqWakeupAccessCycle*PowerConfig.lsqWakeupPower;
				    temp = max(0,totalLsqWakeupAccess - totalLsqWakeupAccessCycle*PowerConfig.resMemport);
				    totalLsqPowerCC1+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
				    totalLsqPowerCC2+=((double)totalLsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
				    totalLsqPowerCC3+=((double)totalLsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
				    totalLsqPowerCC3+=turnoffFactor*PowerConfig.lsqWakeupPower*(totalCycles - totalLsqWakeupAccessCycle);

//				  #ifdef STATICAF
				    
				    totalLsqPowerCC1+=(double)totalLsqPregAccessCycle*PowerConfig.lsqWakeupPower;
					temp = max(0,totalLsqPregAccess - totalLsqPregAccessCycle*PowerConfig.resMemport);
					totalLsqPowerCC1+=((double)(temp)/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
					totalLsqPowerCC2+=((double)totalLsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
					totalLsqPowerCC3+=((double)totalLsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
					totalLsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower*(totalCycles-totalLsqPregAccessCycle);
		/*		  #else
				    if(lsqPregAccess) {
				      if(lsqPregAccess <= PowerConfig.resMemport)
				        lsqPowerCC1+=PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline;
				      else
				        lsqPowerCC1+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
				      lsqPowerCC2+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
				      lsqPowerCC3+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
				    }
				    else
				      lsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower;
				  #endif
		*/
//				  #ifdef STATICAF

					totalRegfilePowerCC1+=((double)totalIntegerRegfileAccessCycle)*PowerConfig.regfilePower;
					temp = max(0,totalIntegerRegfileAccess - totalIntegerRegfileAccessCycle*3*PowerConfig.ruuCommitWidth);
					totalRegfilePowerCC1+=((double)temp/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
					totalRegfilePowerCC2+=((double)totalIntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
					totalRegfilePowerCC3+=((double)totalIntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
					totalRegfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower*(totalCycles-totalIntegerRenameAccessCycle);
				    
		/*		  #else
				    if(IntegerRegfileAccess) {
				      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
				        regfilePowerCC1+=PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline;
				      else
				        regfilePowerCC1+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
				      regfilePowerCC2+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
				      regfilePowerCC3+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
				    }
				    else
				      regfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower;
				  #endif
		*/
					totalIcachePowerCC1+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
					totalIcachePowerCC2+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
					totalIcachePowerCC3+=(totalIcacheAccessCycle)*(PowerConfig.icachePower+PowerConfig.itlb);
					totalIcachePowerCC3+=(totalCycles - totalIcacheAccessCycle)*turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);

					totalDcachePowerCC1+=((double)totalDcacheAccessCycle)*(PowerConfig.dcachePower+PowerConfig.dtlb);
					  temp = max(0,totalDcacheAccess - totalDcacheAccessCycle*PowerConfig.dl1Port);
					  totalDcachePowerCC1+=((double)temp/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
					  totalDcachePowerCC2+=((double)totalDcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
					  totalDcachePowerCC3+=((double)totalDcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +PowerConfig.dtlb);
					  totalDcachePowerCC3+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb)*(totalCycles - totalDcacheAccessCycle);

					  totalDcache2PowerCC1+=((double)totalDcache2AccessCycle)*PowerConfig.dcache2Power;
					  temp = max(0,totalDcache2Access - totalDcache2AccessCycle*PowerConfig.dl2Port);
					  totalDcache2PowerCC1+=((double)temp/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
					  totalDcache2PowerCC2+=((double)totalDcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
				      totalDcache2PowerCC3+=((double)totalDcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
				      totalDcache2PowerCC3+=turnoffFactor*PowerConfig.dcache2Power*(totalCycles - totalDcache2AccessCycle);

				      totalAluPowerCC1+=PowerConfig.ialuPower*totalIaluAccessCycle;
				      totalAluPowerCC3+=turnoffFactor*PowerConfig.ialuPower*(totalCycles - totalIaluAccessCycle);
				      totalAluPowerCC1+=PowerConfig.faluPower*totalFaluAccessCycle;
				      totalAluPowerCC3+=turnoffFactor*PowerConfig.faluPower*(totalCycles - totalFaluAccessCycle);

				      totalAluPowerCC2+=((double)totalIaluAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
				        ((double)totalFaluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
				      totalAluPowerCC3+=((double)totalIaluAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
				        ((double)totalFaluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;

//				  #ifdef STATICAF
				      totalResultbusPowerCC1+=PowerConfig.resultbus*totalResultbusAccessCycle;
				      temp = max(0,totalResultbusAccess - PowerConfig.ruuIssueWidth*totalResultbusAccessCycle);
				      totalResultbusPowerCC1+=((double)temp/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
				      totalResultbusPowerCC2+=((double)totalResultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
				      totalResultbusPowerCC3+=((double)totalResultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
				      totalResultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus*(totalCycles - totalResultbusAccessCycle);
		/*		  #else
				    if(resultbusAccess) {
				      assert(PowerConfig.ruuIssueWidth != 0);
				      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
				        resultbusPowerCC1+=resultbusAfB*PowerConfig.resultbus;
				      }
				      else {
				        resultbusPowerCC1+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
				      }
				      resultbusPowerCC2+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
				      resultbusPowerCC3+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
				    }
				    else
				      resultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus;
				  #endif
		*/
				      
				    totalCyclePower = (PowerConfig.renamePower + PowerConfig.bpredPower + PowerConfig.windowPower + 
				    		PowerConfig.lsqPower + PowerConfig.regfilePower + PowerConfig.icachePower + PowerConfig.dcachePower +
				    		PowerConfig.ialuPower + PowerConfig.faluPower +PowerConfig.resultbus);

				    totalCyclePowerCC1 = totalRenamePowerCC1 + totalBpredPowerCC1 + 
				      totalWindowPowerCC1 + totalLsqPowerCC1 + totalRegfilePowerCC1 + 
				      totalIcachePowerCC1 + totalDcachePowerCC1 + totalAluPowerCC1 + 
				      totalResultbusPowerCC1;

				    totalCyclePowerCC2 = totalRenamePowerCC2 + totalBpredPowerCC2 + 
				      totalWindowPowerCC2 + totalLsqPowerCC2 + totalRegfilePowerCC2 + 
				      totalIcachePowerCC2 + totalDcachePowerCC2 + totalAluPowerCC2 + 
				      totalResultbusPowerCC2;

				    totalCyclePowerCC3 = totalRenamePowerCC3 + totalBpredPowerCC3 + 
				      totalWindowPowerCC3 + totalLsqPowerCC3 + totalRegfilePowerCC3 + 
				      totalIcachePowerCC3 + totalDcachePowerCC3 + totalAluPowerCC3 + 
				      totalResultbusPowerCC3;
				  
				    totalClockPowerCC1=PowerConfig.clockPower*(totalCyclePowerCC1/totalCyclePower);
				    totalClockPowerCC2=PowerConfig.clockPower*(totalCyclePowerCC2/totalCyclePower);
				    totalClockPowerCC3=PowerConfig.clockPower*(totalCyclePowerCC3/totalCyclePower);
			
				}
			
	public double getTotalRenamePowerCC1() {
		return totalRenamePowerCC1;
	}

	public void setTotalRenamePowerCC1(double totalRenamePowerCC1) {
		this.totalRenamePowerCC1 = totalRenamePowerCC1;
	}

	public double getTotalBpredPowerCC1() {
		return totalBpredPowerCC1;
	}

	public void setTotalBpredPowerCC1(double totalBpredPowerCC1) {
		this.totalBpredPowerCC1 = totalBpredPowerCC1;
	}

	public double getTotalWindowPowerCC1() {
		return totalWindowPowerCC1;
	}

	public void setTotalWindowPowerCC1(double totalWindowPowerCC1) {
		this.totalWindowPowerCC1 = totalWindowPowerCC1;
	}

	public double getTotalLsqPowerCC1() {
		return totalLsqPowerCC1;
	}

	public void setTotalLsqPowerCC1(double totalLsqPowerCC1) {
		this.totalLsqPowerCC1 = totalLsqPowerCC1;
	}

	public double getTotalRegfilePowerCC1() {
		return totalRegfilePowerCC1;
	}

	public void setTotalRegfilePowerCC1(double totalRegfilePowerCC1) {
		this.totalRegfilePowerCC1 = totalRegfilePowerCC1;
	}

	public double getTotalIcachePowerCC1() {
		return totalIcachePowerCC1;
	}

	public void setTotalIcachePowerCC1(double totalIcachePowerCC1) {
		this.totalIcachePowerCC1 = totalIcachePowerCC1;
	}

	public double getTotalDcachePowerCC1() {
		return totalDcachePowerCC1;
	}

	public void setTotalDcachePowerCC1(double totalDcachePowerCC1) {
		this.totalDcachePowerCC1 = totalDcachePowerCC1;
	}

	public double getTotalDcache2PowerCC1() {
		return totalDcache2PowerCC1;
	}

	public void setTotalDcache2PowerCC1(double totalDcache2PowerCC1) {
		this.totalDcache2PowerCC1 = totalDcache2PowerCC1;
	}

	public double getTotalAluPowerCC1() {
		return totalAluPowerCC1;
	}

	public void setTotalAluPowerCC1(double totalAluPowerCC1) {
		this.totalAluPowerCC1 = totalAluPowerCC1;
	}

	public double getTotalResultbusPowerCC1() {
		return totalResultbusPowerCC1;
	}

	public void setTotalResultbusPowerCC1(double totalResultbusPowerCC1) {
		this.totalResultbusPowerCC1 = totalResultbusPowerCC1;
	}

	public double getTotalclockPowerCC1() {
		return totalClockPowerCC1;
	}

	public void setTotalclockPowerCC1(double totalClockPowerCC1) {
		this.totalClockPowerCC1 = totalClockPowerCC1;
	}

	public double getTotalRenamePowerCC2() {
		return totalRenamePowerCC2;
	}

	public void setTotalRenamePowerCC2(double totalRenamePowerCC2) {
		this.totalRenamePowerCC2 = totalRenamePowerCC2;
	}

	public double getTotalBpredPowerCC2() {
		return totalBpredPowerCC2;
	}

	public void setTotalBpredPowerCC2(double totalBpredPowerCC2) {
		this.totalBpredPowerCC2 = totalBpredPowerCC2;
	}

	public double getTotalWindowPowerCC2() {
		return totalWindowPowerCC2;
	}

	public void setTotalWindowPowerCC2(double totalWindowPowerCC2) {
		this.totalWindowPowerCC2 = totalWindowPowerCC2;
	}

	public double getTotalLsqPowerCC2() {
		return totalLsqPowerCC2;
	}

	public void setTotalLsqPowerCC2(double totalLsqPowerCC2) {
		this.totalLsqPowerCC2 = totalLsqPowerCC2;
	}

	public double getTotalRegfilePowerCC2() {
		return totalRegfilePowerCC2;
	}

	public void setTotalRegfilePowerCC2(double totalRegfilePowerCC2) {
		this.totalRegfilePowerCC2 = totalRegfilePowerCC2;
	}

	public double getTotalIcachePowerCC2() {
		return totalIcachePowerCC2;
	}

	public void setTotalIcachePowerCC2(double totalIcachePowerCC2) {
		this.totalIcachePowerCC2 = totalIcachePowerCC2;
	}

	public double getTotalDcachePowerCC2() {
		return totalDcachePowerCC2;
	}

	public void setTotalDcachePowerCC2(double totalDcachePowerCC2) {
		this.totalDcachePowerCC2 = totalDcachePowerCC2;
	}

	public double getTotalDcache2PowerCC2() {
		return totalDcache2PowerCC2;
	}

	public void setTotalDcache2PowerCC2(double totalDcache2PowerCC2) {
		this.totalDcache2PowerCC2 = totalDcache2PowerCC2;
	}

	public double getTotalAluPowerCC2() {
		return totalAluPowerCC2;
	}

	public void setTotalAluPowerCC2(double totalAluPowerCC2) {
		this.totalAluPowerCC2 = totalAluPowerCC2;
	}

	public double getTotalResultbusPowerCC2() {
		return totalResultbusPowerCC2;
	}

	public void setTotalResultbusPowerCC2(double totalResultbusPowerCC2) {
		this.totalResultbusPowerCC2 = totalResultbusPowerCC2;
	}

	public double getTotalclockPowerCC2() {
		return totalClockPowerCC2;
	}

	public void setTotalclockPowerCC2(double totalClockPowerCC2) {
		this.totalClockPowerCC2 = totalClockPowerCC2;
	}

	public double getTotalRenamePowerCC3() {
		return totalRenamePowerCC3;
	}

	public void setTotalRenamePowerCC3(double totalRenamePowerCC3) {
		this.totalRenamePowerCC3 = totalRenamePowerCC3;
	}

	public double getTotalBpredPowerCC3() {
		return totalBpredPowerCC3;
	}

	public void setTotalBpredPowerCC3(double totalBpredPowerCC3) {
		this.totalBpredPowerCC3 = totalBpredPowerCC3;
	}

	public double getTotalWindowPowerCC3() {
		return totalWindowPowerCC3;
	}

	public void setTotalWindowPowerCC3(double totalWindowPowerCC3) {
		this.totalWindowPowerCC3 = totalWindowPowerCC3;
	}

	public double getTotalLsqPowerCC3() {
		return totalLsqPowerCC3;
	}

	public void setTotalLsqPowerCC3(double totalLsqPowerCC3) {
		this.totalLsqPowerCC3 = totalLsqPowerCC3;
	}

	public double getTotalRegfilePowerCC3() {
		return totalRegfilePowerCC3;
	}

	public void setTotalRegfilePowerCC3(double totalRegfilePowerCC3) {
		this.totalRegfilePowerCC3 = totalRegfilePowerCC3;
	}

	public double getTotalIcachePowerCC3() {
		return totalIcachePowerCC3;
	}

	public void setTotalIcachePowerCC3(double totalIcachePowerCC3) {
		this.totalIcachePowerCC3 = totalIcachePowerCC3;
	}

	public double getTotalDcachePowerCC3() {
		return totalDcachePowerCC3;
	}

	public void setTotalDcachePowerCC3(double totalDcachePowerCC3) {
		this.totalDcachePowerCC3 = totalDcachePowerCC3;
	}

	public double getTotalDcache2PowerCC3() {
		return totalDcache2PowerCC3;
	}

	public void setTotalDcache2PowerCC3(double totalDcache2PowerCC3) {
		this.totalDcache2PowerCC3 = totalDcache2PowerCC3;
	}

	public double getTotalAluPowerCC3() {
		return totalAluPowerCC3;
	}

	public void setTotalAluPowerCC3(double totalAluPowerCC3) {
		this.totalAluPowerCC3 = totalAluPowerCC3;
	}

	public double getTotalResultbusPowerCC3() {
		return totalResultbusPowerCC3;
	}

	public void setTotalResultbusPowerCC3(double totalResultbusPowerCC3) {
		this.totalResultbusPowerCC3 = totalResultbusPowerCC3;
	}

	public double getTotalclockPowerCC3() {
		return totalClockPowerCC3;
	}

	public void setTotalclockPowerCC3(double totalClockPowerCC3) {
		this.totalClockPowerCC3 = totalClockPowerCC3;
	}

	public void updatePowerStatsPerCycle(){
		
/*		  #ifdef DYNAMICAF
		    windowAfB = computeAf(windowNumPopCountCycle,windowTotalPopCountCycle,dataWidth);
		    lsqAfB = computeAf(lsqNumPopCountCycle,lsqTotalPopCountCycle,dataWidth);
		    regfileAfB = computeAf(regfileNumPopCountCycle,regfileTotalPopCountCycle,dataWidth);
		    resultbusAfB = computeAf(resultbusNumPopCountCycle,resultbusTotalPopCountCycle,dataWidth);
		  #endif
*/		    
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
		      renamePowerCC1+=PowerConfig.renamePower;
		      renamePowerCC2+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		      renamePowerCC3+=((double)renameAccess/(double)PowerConfig.ruuDecodeWidth)*PowerConfig.renamePower;
		    }
		    else 
		      renamePowerCC3+=turnoffFactor*PowerConfig.renamePower;

		    if(bpredAccess>0) {
		      if(bpredAccess <= 2)
		        bpredPowerCC1+=PowerConfig.bpredPower;
		      else
		        bpredPowerCC1+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		      bpredPowerCC2+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		      bpredPowerCC3+=((double)bpredAccess/2.0) * PowerConfig.bpredPower;
		    }
		    else
		      bpredPowerCC3+=turnoffFactor*PowerConfig.bpredPower;

//		  #ifdef STATICAF
		    if(windowPregAccess>0) {
		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
		        windowPowerCC1+=PowerConfig.rsPower;
		      else
		        windowPowerCC1+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		      windowPowerCC2+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		      windowPowerCC3+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*PowerConfig.rsPower;
		    }
		    else
		      windowPowerCC3+=turnoffFactor*PowerConfig.rsPower;
/*		  #elif defined(DYNAMICAF)
		    if(windowPregAccess) {
		      if(windowPregAccess <= 3*PowerConfig.ruuIssueWidth)
		        windowPowerCC1+=PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline;
		      else
		        windowPowerCC1+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPowerCC2+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		      windowPowerCC3+=((double)windowPregAccess/(3.0*(double)PowerConfig.ruuIssueWidth))*(PowerConfig.rsPowerNobit + windowAfB*PowerConfig.rsBitline);
		    }
		    else
		      windowPowerCC3+=turnoffFactor*PowerConfig.rsPower;
		  #else
		    panic("no AF-style defined\n");
		  #endif
*/
		    if(windowSelectionAccess>0) {
		      if(windowSelectionAccess <= PowerConfig.ruuIssueWidth)
		        windowPowerCC1+=PowerConfig.selection;
		      else
		        windowPowerCC1+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		      windowPowerCC2+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		      windowPowerCC3+=((double)windowSelectionAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.selection;
		    }
		    else
		      windowPowerCC3+=turnoffFactor*PowerConfig.selection;

		    if(windowWakeupAccess>0) {
		      if(windowWakeupAccess <= PowerConfig.ruuIssueWidth)
		        windowPowerCC1+=PowerConfig.wakeupPower;
		      else
		        windowPowerCC1+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		      windowPowerCC2+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		      windowPowerCC3+=((double)windowWakeupAccess/((double)PowerConfig.ruuIssueWidth))*PowerConfig.wakeupPower;
		    }
		    else
		      windowPowerCC3+=turnoffFactor*PowerConfig.wakeupPower;

		    if(lsqWakeupAccess>0) {
		      if(lsqWakeupAccess <= PowerConfig.resMemport)
		        lsqPowerCC1+=PowerConfig.lsqWakeupPower;
		      else
		        lsqPowerCC1+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		      lsqPowerCC2+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		      lsqPowerCC3+=((double)lsqWakeupAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqWakeupPower;
		    }
		    else
		      lsqPowerCC3+=turnoffFactor*PowerConfig.lsqWakeupPower;

//		  #ifdef STATICAF
		    if(lsqPregAccess>0) {
		      if(lsqPregAccess <= PowerConfig.resMemport)
		        lsqPowerCC1+=PowerConfig.lsqRsPower;
		      else
		        lsqPowerCC1+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		      lsqPowerCC2+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		      lsqPowerCC3+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*PowerConfig.lsqRsPower;
		    }
		    else
		      lsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower;
/*		  #else
		    if(lsqPregAccess) {
		      if(lsqPregAccess <= PowerConfig.resMemport)
		        lsqPowerCC1+=PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline;
		      else
		        lsqPowerCC1+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPowerCC2+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		      lsqPowerCC3+=((double)lsqPregAccess/((double)PowerConfig.resMemport))*(PowerConfig.lsqRsPowerNobit + lsqAfB*PowerConfig.lsqRsBitline);
		    }
		    else
		      lsqPowerCC3+=turnoffFactor*PowerConfig.lsqRsPower;
		  #endif
*/
//		  #ifdef STATICAF
		    if(IntegerRegfileAccess>0) {
		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
		        regfilePowerCC1+=PowerConfig.regfilePower;
		      else
		        regfilePowerCC1+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		      regfilePowerCC2+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		      regfilePowerCC3+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*PowerConfig.regfilePower;
		    }
		    else
		      regfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower;
/*		  #else
		    if(IntegerRegfileAccess) {
		      if(IntegerRegfileAccess <= (3.0*PowerConfig.ruuCommitWidth))
		        regfilePowerCC1+=PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline;
		      else
		        regfilePowerCC1+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePowerCC2+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		      regfilePowerCC3+=((double)IntegerRegfileAccess/(3.0*(double)PowerConfig.ruuCommitWidth))*(PowerConfig.regfilePowerNobit + regfileAfB*PowerConfig.regfileBitline);
		    }
		    else
		      regfilePowerCC3+=turnoffFactor*PowerConfig.regfilePower;
		  #endif
*/
		    if(icacheAccess>0) {
		      icachePowerCC1+=PowerConfig.icachePower+PowerConfig.itlb;
		      icachePowerCC2+=PowerConfig.icachePower+PowerConfig.itlb;
		      icachePowerCC3+=PowerConfig.icachePower+PowerConfig.itlb;
		    }
		    else
		      icachePowerCC3+=turnoffFactor*(PowerConfig.icachePower+PowerConfig.itlb);

		    if(dcacheAccess>0) {
		      if(dcacheAccess <= PowerConfig.dl1Port)
		        dcachePowerCC1+=PowerConfig.dcachePower+PowerConfig.dtlb;
		      else
		        dcachePowerCC1+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						     PowerConfig.dtlb);
		      dcachePowerCC2+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						   PowerConfig.dtlb);
		      dcachePowerCC3+=((double)dcacheAccess/(double)PowerConfig.dl1Port)*(PowerConfig.dcachePower +
		  						   PowerConfig.dtlb);
		    }
		    else
		      dcachePowerCC3+=turnoffFactor*(PowerConfig.dcachePower+PowerConfig.dtlb);

		    if(dcache2Access>0) {
		      if(dcache2Access <= PowerConfig.dl2Port)
		        dcache2PowerCC1+=PowerConfig.dcache2Power;
		      else
		        dcache2PowerCC1+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2PowerCC2+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		      dcache2PowerCC3+=((double)dcache2Access/(double)PowerConfig.dl2Port)*PowerConfig.dcache2Power;
		    }
		    else
		      dcache2PowerCC3+=turnoffFactor*PowerConfig.dcache2Power;

		    if(aluAccess>0) {
		      if(ialuAccess>0)
		        aluPowerCC1+=PowerConfig.ialuPower;
		      else
		        aluPowerCC3+=turnoffFactor*PowerConfig.ialuPower;
		      if(faluAccess>0)
		        aluPowerCC1+=PowerConfig.faluPower;
		      else
		        aluPowerCC3+=turnoffFactor*PowerConfig.faluPower;

		      aluPowerCC2+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
		      aluPowerCC3+=((double)ialuAccess/(double)PowerConfig.resIalu)*PowerConfig.ialuPower +
		        ((double)faluAccess/(double)PowerConfig.resFpalu)*PowerConfig.faluPower;
		    }
		    else
		      aluPowerCC3+=turnoffFactor*(PowerConfig.ialuPower + PowerConfig.faluPower);

//		  #ifdef STATICAF
		    if(resultbusAccess>0) {
		      assert(PowerConfig.ruuIssueWidth != 0);
		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
		        resultbusPowerCC1+=PowerConfig.resultbus;
		      }
		      else {
		        resultbusPowerCC1+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      }
		      resultbusPowerCC2+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		      resultbusPowerCC3+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*PowerConfig.resultbus;
		    }
		    else
		      resultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus;
/*		  #else
		    if(resultbusAccess) {
		      assert(PowerConfig.ruuIssueWidth != 0);
		      if(resultbusAccess <= PowerConfig.ruuIssueWidth) {
		        resultbusPowerCC1+=resultbusAfB*PowerConfig.resultbus;
		      }
		      else {
		        resultbusPowerCC1+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      }
		      resultbusPowerCC2+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		      resultbusPowerCC3+=((double)resultbusAccess/(double)PowerConfig.ruuIssueWidth)*resultbusAfB*PowerConfig.resultbus;
		    }
		    else
		      resultbusPowerCC3+=turnoffFactor*PowerConfig.resultbus;
		  #endif
*/
		    totalCyclePower = renamePower + bpredPower + windowPower + 
		      lsqPower + regfilePower + icachePower + dcachePower +
		      aluPower + resultbusPower;

		    totalCyclePowerCC1 = renamePowerCC1 + bpredPowerCC1 + 
		      windowPowerCC1 + lsqPowerCC1 + regfilePowerCC1 + 
		      icachePowerCC1 + dcachePowerCC1 + aluPowerCC1 + 
		      resultbusPowerCC1;

		    totalCyclePowerCC2 = renamePowerCC2 + bpredPowerCC2 + 
		      windowPowerCC2 + lsqPowerCC2 + regfilePowerCC2 + 
		      icachePowerCC2 + dcachePowerCC2 + aluPowerCC2 + 
		      resultbusPowerCC2;

		    totalCyclePowerCC3 = renamePowerCC3 + bpredPowerCC3 + 
		      windowPowerCC3 + lsqPowerCC3 + regfilePowerCC3 + 
		      icachePowerCC3 + dcachePowerCC3 + aluPowerCC3 + 
		      resultbusPowerCC3;

		    clockPowerCC1+=PowerConfig.clockPower*(totalCyclePowerCC1/totalCyclePower);
		    clockPowerCC2+=PowerConfig.clockPower*(totalCyclePowerCC2/totalCyclePower);
		    clockPowerCC3+=PowerConfig.clockPower*(totalCyclePowerCC3/totalCyclePower);

		    totalCyclePowerCC1 += clockPowerCC1;
		    totalCyclePowerCC2 += clockPowerCC2;
		    totalCyclePowerCC3 += clockPowerCC3;

		    currentTotalCyclePowerCC1 = totalCyclePowerCC1
		      -lastSingleTotalCyclePowerCC1;
		    currentTotalCyclePowerCC2 = totalCyclePowerCC2
		      -lastSingleTotalCyclePowerCC2;
		    currentTotalCyclePowerCC3 = totalCyclePowerCC3
		      -lastSingleTotalCyclePowerCC3;

		    maxCyclePowerCC1 = max(maxCyclePowerCC1,currentTotalCyclePowerCC1);
		    maxCyclePowerCC2 = max(maxCyclePowerCC2,currentTotalCyclePowerCC2);
		    maxCyclePowerCC3 = max(maxCyclePowerCC3,currentTotalCyclePowerCC3);

		    lastSingleTotalCyclePowerCC1 = totalCyclePowerCC1;
		    lastSingleTotalCyclePowerCC2 = totalCyclePowerCC2;
		    lastSingleTotalCyclePowerCC3 = totalCyclePowerCC3;

	}
	

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

	public double getRenamePowerCC1() {
		return renamePowerCC1;
	}

	public void setRenamePowerCC1(double renamePowerCC1) {
		this.renamePowerCC1 = renamePowerCC1;
	}

	public double getBpredPowerCC1() {
		return bpredPowerCC1;
	}

	public void setBpredPowerCC1(double bpredPowerCC1) {
		this.bpredPowerCC1 = bpredPowerCC1;
	}

	public double getWindowPowerCC1() {
		return windowPowerCC1;
	}

	public void setWindowPowerCC1(double windowPowerCC1) {
		this.windowPowerCC1 = windowPowerCC1;
	}

	public double getLsqPowerCC1() {
		return lsqPowerCC1;
	}

	public void setLsqPowerCC1(double lsqPowerCC1) {
		this.lsqPowerCC1 = lsqPowerCC1;
	}

	public double getRegfilePowerCC1() {
		return regfilePowerCC1;
	}

	public void setRegfilePowerCC1(double regfilePowerCC1) {
		this.regfilePowerCC1 = regfilePowerCC1;
	}

	public double getIcachePowerCC1() {
		return icachePowerCC1;
	}

	public void setIcachePowerCC1(double icachePowerCC1) {
		this.icachePowerCC1 = icachePowerCC1;
	}

	public double getDcachePowerCC1() {
		return dcachePowerCC1;
	}

	public void setDcachePowerCC1(double dcachePowerCC1) {
		this.dcachePowerCC1 = dcachePowerCC1;
	}

	public double getDcache2PowerCC1() {
		return dcache2PowerCC1;
	}

	public void setDcache2PowerCC1(double dcache2PowerCC1) {
		this.dcache2PowerCC1 = dcache2PowerCC1;
	}

	public double getAluPowerCC1() {
		return aluPowerCC1;
	}

	public void setAluPowerCC1(double aluPowerCC1) {
		this.aluPowerCC1 = aluPowerCC1;
	}

	public double getResultbusPowerCC1() {
		return resultbusPowerCC1;
	}

	public void setResultbusPowerCC1(double resultbusPowerCC1) {
		this.resultbusPowerCC1 = resultbusPowerCC1;
	}

	public double getClockPowerCC1() {
		return clockPowerCC1;
	}

	public void setClockPowerCC1(double clockPowerCC1) {
		this.clockPowerCC1 = clockPowerCC1;
	}

	public double getRenamePowerCC2() {
		return renamePowerCC2;
	}

	public void setRenamePowerCC2(double renamePowerCC2) {
		this.renamePowerCC2 = renamePowerCC2;
	}

	public double getBpredPowerCC2() {
		return bpredPowerCC2;
	}

	public void setBpredPowerCC2(double bpredPowerCC2) {
		this.bpredPowerCC2 = bpredPowerCC2;
	}

	public double getWindowPowerCC2() {
		return windowPowerCC2;
	}

	public void setWindowPowerCC2(double windowPowerCC2) {
		this.windowPowerCC2 = windowPowerCC2;
	}

	public double getLsqPowerCC2() {
		return lsqPowerCC2;
	}

	public void setLsqPowerCC2(double lsqPowerCC2) {
		this.lsqPowerCC2 = lsqPowerCC2;
	}

	public double getRegfilePowerCC2() {
		return regfilePowerCC2;
	}

	public void setRegfilePowerCC2(double regfilePowerCC2) {
		this.regfilePowerCC2 = regfilePowerCC2;
	}

	public double getIcachePowerCC2() {
		return icachePowerCC2;
	}

	public void setIcachePowerCC2(double icachePowerCC2) {
		this.icachePowerCC2 = icachePowerCC2;
	}

	public double getDcachePowerCC2() {
		return dcachePowerCC2;
	}

	public void setDcachePowerCC2(double dcachePowerCC2) {
		this.dcachePowerCC2 = dcachePowerCC2;
	}

	public double getDcache2PowerCC2() {
		return dcache2PowerCC2;
	}

	public void setDcache2PowerCC2(double dcache2PowerCC2) {
		this.dcache2PowerCC2 = dcache2PowerCC2;
	}

	public double getAluPowerCC2() {
		return aluPowerCC2;
	}

	public void setAluPowerCC2(double aluPowerCC2) {
		this.aluPowerCC2 = aluPowerCC2;
	}

	public double getResultbusPowerCC2() {
		return resultbusPowerCC2;
	}

	public void setResultbusPowerCC2(double resultbusPowerCC2) {
		this.resultbusPowerCC2 = resultbusPowerCC2;
	}

	public double getClockPowerCC2() {
		return clockPowerCC2;
	}

	public void setClockPowerCC2(double clockPowerCC2) {
		this.clockPowerCC2 = clockPowerCC2;
	}

	public double getRenamePowerCC3() {
		return renamePowerCC3;
	}

	public void setRenamePowerCC3(double renamePowerCC3) {
		this.renamePowerCC3 = renamePowerCC3;
	}

	public double getBpredPowerCC3() {
		return bpredPowerCC3;
	}

	public void setBpredPowerCC3(double bpredPowerCC3) {
		this.bpredPowerCC3 = bpredPowerCC3;
	}

	public double getWindowPowerCC3() {
		return windowPowerCC3;
	}

	public void setWindowPowerCC3(double windowPowerCC3) {
		this.windowPowerCC3 = windowPowerCC3;
	}

	public double getLsqPowerCC3() {
		return lsqPowerCC3;
	}

	public void setLsqPowerCC3(double lsqPowerCC3) {
		this.lsqPowerCC3 = lsqPowerCC3;
	}

	public double getRegfilePowerCC3() {
		return regfilePowerCC3;
	}

	public void setRegfilePowerCC3(double regfilePowerCC3) {
		this.regfilePowerCC3 = regfilePowerCC3;
	}

	public double getIcachePowerCC3() {
		return icachePowerCC3;
	}

	public void setIcachePowerCC3(double icachePowerCC3) {
		this.icachePowerCC3 = icachePowerCC3;
	}

	public double getDcachePowerCC3() {
		return dcachePowerCC3;
	}

	public void setDcachePowerCC3(double dcachePowerCC3) {
		this.dcachePowerCC3 = dcachePowerCC3;
	}

	public double getDcache2PowerCC3() {
		return dcache2PowerCC3;
	}

	public void setDcache2PowerCC3(double dcache2PowerCC3) {
		this.dcache2PowerCC3 = dcache2PowerCC3;
	}

	public double getAluPowerCC3() {
		return aluPowerCC3;
	}

	public void setAluPowerCC3(double aluPowerCC3) {
		this.aluPowerCC3 = aluPowerCC3;
	}

	public double getResultbusPowerCC3() {
		return resultbusPowerCC3;
	}

	public void setResultbusPowerCC3(double resultbusPowerCC3) {
		this.resultbusPowerCC3 = resultbusPowerCC3;
	}

	public double getClockPowerCC3() {
		return clockPowerCC3;
	}

	public void setClockPowerCC3(double clockPowerCC3) {
		this.clockPowerCC3 = clockPowerCC3;
	}

	public double getTotalCyclePower() {
		return totalCyclePower;
	}

	public void setTotalCyclePower(double totalCyclePower) {
		this.totalCyclePower = totalCyclePower;
	}

	public double getTotalCyclePowerCC1() {
		return totalCyclePowerCC1;
	}

	public void setTotalCyclePowerCC1(double totalCyclePowerCC1) {
		this.totalCyclePowerCC1 = totalCyclePowerCC1;
	}

	public double getTotalCyclePowerCC2() {
		return totalCyclePowerCC2;
	}

	public void setTotalCyclePowerCC2(double totalCyclePowerCC2) {
		this.totalCyclePowerCC2 = totalCyclePowerCC2;
	}

	public double getTotalCyclePowerCC3() {
		return totalCyclePowerCC3;
	}

	public void setTotalCyclePowerCC3(double totalCyclePowerCC3) {
		this.totalCyclePowerCC3 = totalCyclePowerCC3;
	}

	public double getLastSingleTotalCyclePowerCC1() {
		return lastSingleTotalCyclePowerCC1;
	}

	public void setLastSingleTotalCyclePowerCC1(double lastSingleTotalCyclePowerCC1) {
		this.lastSingleTotalCyclePowerCC1 = lastSingleTotalCyclePowerCC1;
	}

	public double getLastSingleTotalCyclePowerCC2() {
		return lastSingleTotalCyclePowerCC2;
	}

	public void setLastSingleTotalCyclePowerCC2(double lastSingleTotalCyclePowerCC2) {
		this.lastSingleTotalCyclePowerCC2 = lastSingleTotalCyclePowerCC2;
	}

	public double getLastSingleTotalCyclePowerCC3() {
		return lastSingleTotalCyclePowerCC3;
	}

	public void setLastSingleTotalCyclePowerCC3(double lastSingleTotalCyclePowerCC3) {
		this.lastSingleTotalCyclePowerCC3 = lastSingleTotalCyclePowerCC3;
	}

	public double getCurrentTotalCyclePowerCC1() {
		return currentTotalCyclePowerCC1;
	}

	public void setCurrentTotalCyclePowerCC1(double currentTotalCyclePowerCC1) {
		this.currentTotalCyclePowerCC1 = currentTotalCyclePowerCC1;
	}

	public double getCurrentTotalCyclePowerCC2() {
		return currentTotalCyclePowerCC2;
	}

	public void setCurrentTotalCyclePowerCC2(double currentTotalCyclePowerCC2) {
		this.currentTotalCyclePowerCC2 = currentTotalCyclePowerCC2;
	}

	public double getCurrentTotalCyclePowerCC3() {
		return currentTotalCyclePowerCC3;
	}

	public void setCurrentTotalCyclePowerCC3(double currentTotalCyclePowerCC3) {
		this.currentTotalCyclePowerCC3 = currentTotalCyclePowerCC3;
	}

	public double getMaxCyclePowerCC1() {
		return maxCyclePowerCC1;
	}

	public void setMaxCyclePowerCC1(double maxCyclePowerCC1) {
		this.maxCyclePowerCC1 = maxCyclePowerCC1;
	}

	public double getMaxCyclePowerCC2() {
		return maxCyclePowerCC2;
	}

	public void setMaxCyclePowerCC2(double maxCyclePowerCC2) {
		this.maxCyclePowerCC2 = maxCyclePowerCC2;
	}

	public double getMaxCyclePowerCC3() {
		return maxCyclePowerCC3;
	}

	public void setMaxCyclePowerCC3(double maxCyclePowerCC3) {
		this.maxCyclePowerCC3 = maxCyclePowerCC3;
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

	public double getTotalClockPowerCC1() {
		return totalClockPowerCC1;
	}

	public void setTotalClockPowerCC1(double totalClockPowerCC1) {
		this.totalClockPowerCC1 = totalClockPowerCC1;
	}

	public double getTotalClockPowerCC2() {
		return totalClockPowerCC2;
	}

	public void setTotalClockPowerCC2(double totalClockPowerCC2) {
		this.totalClockPowerCC2 = totalClockPowerCC2;
	}

	public double getTotalClockPowerCC3() {
		return totalClockPowerCC3;
	}

	public void setTotalClockPowerCC3(double totalClockPowerCC3) {
		this.totalClockPowerCC3 = totalClockPowerCC3;
	}
	public double getTotalAluPower() {
		return totalAluPower;
	}

	public void setTotalAluPower(double totalAluPower) {
		this.totalAluPower = totalAluPower;
	}



}