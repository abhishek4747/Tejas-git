package power;

public class Counters {

	/* options for Wattch */
	public static long data_width;

	/* counters added for Wattch */
	public static long rename_access=0;
	public static long bpred_access=0;
	public static long window_access=0;
	public static long lsq_access=0;
	public static long regfile_access=0;
//	public static long []icache_access=0;
//	public static long []dcache_access=0;
	public static long icache_access=0;
	public static long dcache_access=0;

	public static long dcache2_access=0;
	public static long alu_access=0;
	public static long ialu_access=0;
	public static long falu_access=0;
	public static long resultbus_access=0;

	public static long window_preg_access=0;
	public static long window_selection_access=0;
	public static long window_wakeup_access=0;
	public static long lsq_store_data_access=0;
	public static long lsq_load_data_access=0;
	public static long lsq_preg_access=0;
	public static long lsq_wakeup_access=0;

	public static long window_total_pop_count_cycle=0;
	public static long window_num_pop_count_cycle=0;
	public static long lsq_total_pop_count_cycle=0;
	public static long lsq_num_pop_count_cycle=0;
	public static long regfile_total_pop_count_cycle=0;
	public static long regfile_num_pop_count_cycle=0;
	public static long resultbus_total_pop_count_cycle=0;
	public static long resultbus_num_pop_count_cycle=0;
	
	public static long total_rename_access=0;
	public static long total_bpred_access=0;
	public static long total_window_access=0;
	public static long total_lsq_access=0;
	public static long total_regfile_access=0;
	public static long total_icache_access=0;
	public static long total_dcache_access=0;
	public static long total_dcache2_access=0;
	public static long total_alu_access=0;
	public static long total_resultbus_access=0;

	public static long max_rename_access=0;
	public static long max_bpred_access=0;
	public static long max_window_access=0;
	public static long max_lsq_access=0;
	public static long max_regfile_access=0;
	public static long max_icache_access=0;
	public static long max_dcache_access=0;
	public static long max_dcache2_access=0;
	public static long max_alu_access=0;
	public static long max_resultbus_access=0;
	
	public static double rename_power=0;
	public static double bpred_power=0;
	public static double window_power=0;
	public static double lsq_power=0;
	public static double regfile_power=0;
	public static double icache_power=0;
	public static double dcache_power=0;
	public static double dcache2_power=0;
	public static double alu_power=0;
	public static double falu_power=0;
	public static double resultbus_power=0;
	public static double clock_power=0;

	public static double rename_power_cc1=0;
	public static double bpred_power_cc1=0;
	public static double window_power_cc1=0;
	public static double lsq_power_cc1=0;
	public static double regfile_power_cc1=0;
	public static double icache_power_cc1=0;
	public static double dcache_power_cc1=0;
	public static double dcache2_power_cc1=0;
	public static double alu_power_cc1=0;
	public static double resultbus_power_cc1=0;
	public static double clock_power_cc1=0;

	public static double rename_power_cc2=0;
	public static double bpred_power_cc2=0;
	public static double window_power_cc2=0;
	public static double lsq_power_cc2=0;
	public static double regfile_power_cc2=0;
	public static double icache_power_cc2=0;
	public static double dcache_power_cc2=0;
	public static double dcache2_power_cc2=0;
	public static double alu_power_cc2=0;
	public static double resultbus_power_cc2=0;
	public static double clock_power_cc2=0;

	public static double rename_power_cc3=0;
	public static double bpred_power_cc3=0;
	public static double window_power_cc3=0;
	public static double lsq_power_cc3=0;
	public static double regfile_power_cc3=0;
	public static double icache_power_cc3=0;
	public static double dcache_power_cc3=0;
	public static double dcache2_power_cc3=0;
	public static double alu_power_cc3=0;
	public static double resultbus_power_cc3=0;
	public static double clock_power_cc3=0;

	public static double total_cycle_power;
	public static double total_cycle_power_cc1;
	public static double total_cycle_power_cc2;
	public static double total_cycle_power_cc3;

	public static double last_single_total_cycle_power_cc1 = 0.0;
	public static double last_single_total_cycle_power_cc2 = 0.0;
	public static double last_single_total_cycle_power_cc3 = 0.0;
	public static double current_total_cycle_power_cc1;
	public static double current_total_cycle_power_cc2;
	public static double current_total_cycle_power_cc3;

	public static double max_cycle_power_cc1 = 0.0;
	public static double max_cycle_power_cc2 = 0.0;
	public static double max_cycle_power_cc3 = 0.0;

	public static double turnoff_factor = 0.1;
	
	public Counters(){
		/* options for Wattch */
		 data_width = 64;

		/* counters added for Wattch */
		 rename_access=0;
		 bpred_access=0;
		 window_access=0;
		 lsq_access=0;
		 regfile_access=0;
		 
		 //icache_access=new long[SystemConfig.NoOfCores];
		 //dcache_access=new long[SystemConfig.NoOfCores];
		 icache_access=0;
		 dcache_access=0;
//		 for(int i=0;i<SystemConfig.NoOfCores;i++){
//			 icache_access[i]=0;
//			 dcache_access[i]=0;
//		 }
//		 
		 icache_access=0;
		 dcache_access=0;
		 dcache2_access=0;
		 alu_access=0;
		 ialu_access=0;
		 falu_access=0;
		 resultbus_access=0;

		 window_preg_access=0;
		 window_selection_access=0;
		 window_wakeup_access=0;
		 lsq_store_data_access=0;
		 lsq_load_data_access=0;
		 lsq_preg_access=0;
		 lsq_wakeup_access=0;

		 window_total_pop_count_cycle=0;
		 window_num_pop_count_cycle=0;
		 lsq_total_pop_count_cycle=0;
		 lsq_num_pop_count_cycle=0;
		 regfile_total_pop_count_cycle=0;
		 regfile_num_pop_count_cycle=0;
		 resultbus_total_pop_count_cycle=0;
		 resultbus_num_pop_count_cycle=0;
		 
		 rename_power=0;
		 bpred_power=0;
		 window_power=0;
		 lsq_power=0;
		 regfile_power=0;
		 icache_power=0;
		 dcache_power=0;
		 dcache2_power=0;
		 alu_power=0;
		 falu_power=0;
		 resultbus_power=0;
		 clock_power=0;

		 rename_power_cc1=0;
		 bpred_power_cc1=0;
		 window_power_cc1=0;
		 lsq_power_cc1=0;
		 regfile_power_cc1=0;
		 icache_power_cc1=0;
		 dcache_power_cc1=0;
		 dcache2_power_cc1=0;
		 alu_power_cc1=0;
		 resultbus_power_cc1=0;
		 clock_power_cc1=0;

		 rename_power_cc2=0;
		 bpred_power_cc2=0;
		 window_power_cc2=0;
		 lsq_power_cc2=0;
		 regfile_power_cc2=0;
		 icache_power_cc2=0;
		 dcache_power_cc2=0;
		 dcache2_power_cc2=0;
		 alu_power_cc2=0;
		 resultbus_power_cc2=0;
		 clock_power_cc2=0;

		 rename_power_cc3=0;
		 bpred_power_cc3=0;
		 window_power_cc3=0;
		 lsq_power_cc3=0;
		 regfile_power_cc3=0;
		 icache_power_cc3=0;
		 dcache_power_cc3=0;
		 dcache2_power_cc3=0;
		 alu_power_cc3=0;
		 resultbus_power_cc3=0;
		 clock_power_cc3=0;


		 last_single_total_cycle_power_cc1 = 0.0;
		 last_single_total_cycle_power_cc2 = 0.0;
		 last_single_total_cycle_power_cc3 = 0.0;


		 max_cycle_power_cc1 = 0.0;
		 max_cycle_power_cc2 = 0.0;
		 max_cycle_power_cc3 = 0.0;

		
	}
	
	public static void clearAccessStats(){

		/* counters added for Wattch */
		 rename_access=0;
		 bpred_access=0;
		 window_access=0;
		 lsq_access=0;
		 regfile_access=0;
		 
//		 for(int i=0;i<SystemConfig.NoOfCores;i++){
//			 icache_access[i]=0;
//			 dcache_access[i]=0;
//		 }
//		 
		 icache_access=0;
		 dcache_access=0;
		 dcache2_access=0;
		 alu_access=0;
		 ialu_access=0;
		 falu_access=0;
		 resultbus_access=0;

		 window_preg_access=0;
		 window_selection_access=0;
		 window_wakeup_access=0;
		 lsq_store_data_access=0;
		 lsq_load_data_access=0;
		 lsq_preg_access=0;
		 lsq_wakeup_access=0;

		 window_total_pop_count_cycle=0;
		 window_num_pop_count_cycle=0;
		 lsq_total_pop_count_cycle=0;
		 lsq_num_pop_count_cycle=0;
		 regfile_total_pop_count_cycle=0;
		 regfile_num_pop_count_cycle=0;
		 resultbus_total_pop_count_cycle=0;
		 resultbus_num_pop_count_cycle=0;
				
	}
	public static void updatePowerStats(){
		  double window_af_b, lsq_af_b, regfile_af_b, resultbus_af_b;

/*		  #ifdef DYNAMIC_AF
		    window_af_b = compute_af(window_num_pop_count_cycle,window_total_pop_count_cycle,data_width);
		    lsq_af_b = compute_af(lsq_num_pop_count_cycle,lsq_total_pop_count_cycle,data_width);
		    regfile_af_b = compute_af(regfile_num_pop_count_cycle,regfile_total_pop_count_cycle,data_width);
		    resultbus_af_b = compute_af(resultbus_num_pop_count_cycle,resultbus_total_pop_count_cycle,data_width);
		  #endif
*/		    
		    rename_power+=PowerConfig.rename_power;
		    bpred_power+=PowerConfig.bpred_power;
		    window_power+=PowerConfig.window_power;
		    lsq_power+=PowerConfig.lsq_power;
		    regfile_power+=PowerConfig.regfile_power;
		    icache_power+=PowerConfig.icache_power+PowerConfig.itlb;
		    dcache_power+=PowerConfig.dcache_power+PowerConfig.dtlb;
		    dcache2_power+=PowerConfig.dcache2_power;
		    alu_power+=PowerConfig.ialu_power + PowerConfig.falu_power;
		    falu_power+=PowerConfig.falu_power;
		    resultbus_power+=PowerConfig.resultbus;
		    clock_power+=PowerConfig.clock_power;

		    total_rename_access+=rename_access;
		    total_bpred_access+=bpred_access;
		    total_window_access+=window_access;
		    total_lsq_access+=lsq_access;
		    total_regfile_access+=regfile_access;
		    total_icache_access+=icache_access;
		    total_dcache_access+=dcache_access;
		    total_dcache2_access+=dcache2_access;
		    total_alu_access+=alu_access;
		    total_resultbus_access+=resultbus_access;

		    max_rename_access=max(rename_access,max_rename_access);
		    max_bpred_access=max(bpred_access,max_bpred_access);
		    max_window_access=max(window_access,max_window_access);
		    max_lsq_access=max(lsq_access,max_lsq_access);
		    max_regfile_access=max(regfile_access,max_regfile_access);
		    max_icache_access=max(icache_access,max_icache_access);
		    max_dcache_access=max(dcache_access,max_dcache_access);
		    max_dcache2_access=max(dcache2_access,max_dcache2_access);
		    max_alu_access=max(alu_access,max_alu_access);
		    max_resultbus_access=max(resultbus_access,max_resultbus_access);
		        
		    if(rename_access>0) {
		      rename_power_cc1+=PowerConfig.rename_power;
		      rename_power_cc2+=((double)rename_access/(double)PowerConfig.ruu_decode_width)*PowerConfig.rename_power;
		      rename_power_cc3+=((double)rename_access/(double)PowerConfig.ruu_decode_width)*PowerConfig.rename_power;
		    }
		    else 
		      rename_power_cc3+=turnoff_factor*PowerConfig.rename_power;

		    if(bpred_access>0) {
		      if(bpred_access <= 2)
		        bpred_power_cc1+=PowerConfig.bpred_power;
		      else
		        bpred_power_cc1+=((double)bpred_access/2.0) * PowerConfig.bpred_power;
		      bpred_power_cc2+=((double)bpred_access/2.0) * PowerConfig.bpred_power;
		      bpred_power_cc3+=((double)bpred_access/2.0) * PowerConfig.bpred_power;
		    }
		    else
		      bpred_power_cc3+=turnoff_factor*PowerConfig.bpred_power;

//		  #ifdef STATIC_AF
		    if(window_preg_access>0) {
		      if(window_preg_access <= 3*PowerConfig.ruu_issue_width)
		        window_power_cc1+=PowerConfig.rs_power;
		      else
		        window_power_cc1+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*PowerConfig.rs_power;
		      window_power_cc2+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*PowerConfig.rs_power;
		      window_power_cc3+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*PowerConfig.rs_power;
		    }
		    else
		      window_power_cc3+=turnoff_factor*PowerConfig.rs_power;
/*		  #elif defined(DYNAMIC_AF)
		    if(window_preg_access) {
		      if(window_preg_access <= 3*PowerConfig.ruu_issue_width)
		        window_power_cc1+=PowerConfig.rs_power_nobit + window_af_b*PowerConfig.rs_bitline;
		      else
		        window_power_cc1+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*(PowerConfig.rs_power_nobit + window_af_b*PowerConfig.rs_bitline);
		      window_power_cc2+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*(PowerConfig.rs_power_nobit + window_af_b*PowerConfig.rs_bitline);
		      window_power_cc3+=((double)window_preg_access/(3.0*(double)PowerConfig.ruu_issue_width))*(PowerConfig.rs_power_nobit + window_af_b*PowerConfig.rs_bitline);
		    }
		    else
		      window_power_cc3+=turnoff_factor*PowerConfig.rs_power;
		  #else
		    panic("no AF-style defined\n");
		  #endif
*/
		    if(window_selection_access>0) {
		      if(window_selection_access <= PowerConfig.ruu_issue_width)
		        window_power_cc1+=PowerConfig.selection;
		      else
		        window_power_cc1+=((double)window_selection_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.selection;
		      window_power_cc2+=((double)window_selection_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.selection;
		      window_power_cc3+=((double)window_selection_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.selection;
		    }
		    else
		      window_power_cc3+=turnoff_factor*PowerConfig.selection;

		    if(window_wakeup_access>0) {
		      if(window_wakeup_access <= PowerConfig.ruu_issue_width)
		        window_power_cc1+=PowerConfig.wakeup_power;
		      else
		        window_power_cc1+=((double)window_wakeup_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.wakeup_power;
		      window_power_cc2+=((double)window_wakeup_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.wakeup_power;
		      window_power_cc3+=((double)window_wakeup_access/((double)PowerConfig.ruu_issue_width))*PowerConfig.wakeup_power;
		    }
		    else
		      window_power_cc3+=turnoff_factor*PowerConfig.wakeup_power;

		    if(lsq_wakeup_access>0) {
		      if(lsq_wakeup_access <= PowerConfig.res_memport)
		        lsq_power_cc1+=PowerConfig.lsq_wakeup_power;
		      else
		        lsq_power_cc1+=((double)lsq_wakeup_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_wakeup_power;
		      lsq_power_cc2+=((double)lsq_wakeup_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_wakeup_power;
		      lsq_power_cc3+=((double)lsq_wakeup_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_wakeup_power;
		    }
		    else
		      lsq_power_cc3+=turnoff_factor*PowerConfig.lsq_wakeup_power;

//		  #ifdef STATIC_AF
		    if(lsq_preg_access>0) {
		      if(lsq_preg_access <= PowerConfig.res_memport)
		        lsq_power_cc1+=PowerConfig.lsq_rs_power;
		      else
		        lsq_power_cc1+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_rs_power;
		      lsq_power_cc2+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_rs_power;
		      lsq_power_cc3+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*PowerConfig.lsq_rs_power;
		    }
		    else
		      lsq_power_cc3+=turnoff_factor*PowerConfig.lsq_rs_power;
/*		  #else
		    if(lsq_preg_access) {
		      if(lsq_preg_access <= PowerConfig.res_memport)
		        lsq_power_cc1+=PowerConfig.lsq_rs_power_nobit + lsq_af_b*PowerConfig.lsq_rs_bitline;
		      else
		        lsq_power_cc1+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*(PowerConfig.lsq_rs_power_nobit + lsq_af_b*PowerConfig.lsq_rs_bitline);
		      lsq_power_cc2+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*(PowerConfig.lsq_rs_power_nobit + lsq_af_b*PowerConfig.lsq_rs_bitline);
		      lsq_power_cc3+=((double)lsq_preg_access/((double)PowerConfig.res_memport))*(PowerConfig.lsq_rs_power_nobit + lsq_af_b*PowerConfig.lsq_rs_bitline);
		    }
		    else
		      lsq_power_cc3+=turnoff_factor*PowerConfig.lsq_rs_power;
		  #endif
*/
//		  #ifdef STATIC_AF
		    if(regfile_access>0) {
		      if(regfile_access <= (3.0*PowerConfig.ruu_commit_width))
		        regfile_power_cc1+=PowerConfig.regfile_power;
		      else
		        regfile_power_cc1+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*PowerConfig.regfile_power;
		      regfile_power_cc2+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*PowerConfig.regfile_power;
		      regfile_power_cc3+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*PowerConfig.regfile_power;
		    }
		    else
		      regfile_power_cc3+=turnoff_factor*PowerConfig.regfile_power;
/*		  #else
		    if(regfile_access) {
		      if(regfile_access <= (3.0*PowerConfig.ruu_commit_width))
		        regfile_power_cc1+=PowerConfig.regfile_power_nobit + regfile_af_b*PowerConfig.regfile_bitline;
		      else
		        regfile_power_cc1+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*(PowerConfig.regfile_power_nobit + regfile_af_b*PowerConfig.regfile_bitline);
		      regfile_power_cc2+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*(PowerConfig.regfile_power_nobit + regfile_af_b*PowerConfig.regfile_bitline);
		      regfile_power_cc3+=((double)regfile_access/(3.0*(double)PowerConfig.ruu_commit_width))*(PowerConfig.regfile_power_nobit + regfile_af_b*PowerConfig.regfile_bitline);
		    }
		    else
		      regfile_power_cc3+=turnoff_factor*PowerConfig.regfile_power;
		  #endif
*/
		    if(icache_access>0) {
		      /* don't scale icache because we assume 1 line is fetched, unless fetch stalls */
		      icache_power_cc1+=PowerConfig.icache_power+PowerConfig.itlb;
		      icache_power_cc2+=PowerConfig.icache_power+PowerConfig.itlb;
		      icache_power_cc3+=PowerConfig.icache_power+PowerConfig.itlb;
		    }
		    else
		      icache_power_cc3+=turnoff_factor*(PowerConfig.icache_power+PowerConfig.itlb);

		    if(dcache_access>0) {
		      if(dcache_access <= PowerConfig.res_memport)
		        dcache_power_cc1+=PowerConfig.dcache_power+PowerConfig.dtlb;
		      else
		        dcache_power_cc1+=((double)dcache_access/(double)PowerConfig.res_memport)*(PowerConfig.dcache_power +
		  						     PowerConfig.dtlb);
		      dcache_power_cc2+=((double)dcache_access/(double)PowerConfig.res_memport)*(PowerConfig.dcache_power +
		  						   PowerConfig.dtlb);
		      dcache_power_cc3+=((double)dcache_access/(double)PowerConfig.res_memport)*(PowerConfig.dcache_power +
		  						   PowerConfig.dtlb);
		    }
		    else
		      dcache_power_cc3+=turnoff_factor*(PowerConfig.dcache_power+PowerConfig.dtlb);

		    if(dcache2_access>0) {
		      if(dcache2_access <= PowerConfig.res_memport)
		        dcache2_power_cc1+=PowerConfig.dcache2_power;
		      else
		        dcache2_power_cc1+=((double)dcache2_access/(double)PowerConfig.res_memport)*PowerConfig.dcache2_power;
		      dcache2_power_cc2+=((double)dcache2_access/(double)PowerConfig.res_memport)*PowerConfig.dcache2_power;
		      dcache2_power_cc3+=((double)dcache2_access/(double)PowerConfig.res_memport)*PowerConfig.dcache2_power;
		    }
		    else
		      dcache2_power_cc3+=turnoff_factor*PowerConfig.dcache2_power;

		    if(alu_access>0) {
		      if(ialu_access>0)
		        alu_power_cc1+=PowerConfig.ialu_power;
		      else
		        alu_power_cc3+=turnoff_factor*PowerConfig.ialu_power;
		      if(falu_access>0)
		        alu_power_cc1+=PowerConfig.falu_power;
		      else
		        alu_power_cc3+=turnoff_factor*PowerConfig.falu_power;

		      alu_power_cc2+=((double)ialu_access/(double)PowerConfig.res_ialu)*PowerConfig.ialu_power +
		        ((double)falu_access/(double)PowerConfig.res_fpalu)*PowerConfig.falu_power;
		      alu_power_cc3+=((double)ialu_access/(double)PowerConfig.res_ialu)*PowerConfig.ialu_power +
		        ((double)falu_access/(double)PowerConfig.res_fpalu)*PowerConfig.falu_power;
		    }
		    else
		      alu_power_cc3+=turnoff_factor*(PowerConfig.ialu_power + PowerConfig.falu_power);

//		  #ifdef STATIC_AF
		    if(resultbus_access>0) {
		      assert(PowerConfig.ruu_issue_width != 0);
		      if(resultbus_access <= PowerConfig.ruu_issue_width) {
		        resultbus_power_cc1+=PowerConfig.resultbus;
		      }
		      else {
		        resultbus_power_cc1+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*PowerConfig.resultbus;
		      }
		      resultbus_power_cc2+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*PowerConfig.resultbus;
		      resultbus_power_cc3+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*PowerConfig.resultbus;
		    }
		    else
		      resultbus_power_cc3+=turnoff_factor*PowerConfig.resultbus;
/*		  #else
		    if(resultbus_access) {
		      assert(PowerConfig.ruu_issue_width != 0);
		      if(resultbus_access <= PowerConfig.ruu_issue_width) {
		        resultbus_power_cc1+=resultbus_af_b*PowerConfig.resultbus;
		      }
		      else {
		        resultbus_power_cc1+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*resultbus_af_b*PowerConfig.resultbus;
		      }
		      resultbus_power_cc2+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*resultbus_af_b*PowerConfig.resultbus;
		      resultbus_power_cc3+=((double)resultbus_access/(double)PowerConfig.ruu_issue_width)*resultbus_af_b*PowerConfig.resultbus;
		    }
		    else
		      resultbus_power_cc3+=turnoff_factor*PowerConfig.resultbus;
		  #endif
*/
		    total_cycle_power = rename_power + bpred_power + window_power + 
		      lsq_power + regfile_power + icache_power + dcache_power +
		      alu_power + resultbus_power;

		    total_cycle_power_cc1 = rename_power_cc1 + bpred_power_cc1 + 
		      window_power_cc1 + lsq_power_cc1 + regfile_power_cc1 + 
		      icache_power_cc1 + dcache_power_cc1 + alu_power_cc1 + 
		      resultbus_power_cc1;

		    total_cycle_power_cc2 = rename_power_cc2 + bpred_power_cc2 + 
		      window_power_cc2 + lsq_power_cc2 + regfile_power_cc2 + 
		      icache_power_cc2 + dcache_power_cc2 + alu_power_cc2 + 
		      resultbus_power_cc2;

		    total_cycle_power_cc3 = rename_power_cc3 + bpred_power_cc3 + 
		      window_power_cc3 + lsq_power_cc3 + regfile_power_cc3 + 
		      icache_power_cc3 + dcache_power_cc3 + alu_power_cc3 + 
		      resultbus_power_cc3;

		    clock_power_cc1+=PowerConfig.clock_power*(total_cycle_power_cc1/total_cycle_power);
		    clock_power_cc2+=PowerConfig.clock_power*(total_cycle_power_cc2/total_cycle_power);
		    clock_power_cc3+=PowerConfig.clock_power*(total_cycle_power_cc3/total_cycle_power);

		    total_cycle_power_cc1 += clock_power_cc1;
		    total_cycle_power_cc2 += clock_power_cc2;
		    total_cycle_power_cc3 += clock_power_cc3;

		    current_total_cycle_power_cc1 = total_cycle_power_cc1
		      -last_single_total_cycle_power_cc1;
		    current_total_cycle_power_cc2 = total_cycle_power_cc2
		      -last_single_total_cycle_power_cc2;
		    current_total_cycle_power_cc3 = total_cycle_power_cc3
		      -last_single_total_cycle_power_cc3;

		    max_cycle_power_cc1 = max(max_cycle_power_cc1,current_total_cycle_power_cc1);
		    max_cycle_power_cc2 = max(max_cycle_power_cc2,current_total_cycle_power_cc2);
		    max_cycle_power_cc3 = max(max_cycle_power_cc3,current_total_cycle_power_cc3);

		    last_single_total_cycle_power_cc1 = total_cycle_power_cc1;
		    last_single_total_cycle_power_cc2 = total_cycle_power_cc2;
		    last_single_total_cycle_power_cc3 = total_cycle_power_cc3;

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

}
