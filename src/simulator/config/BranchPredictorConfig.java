package config;

public class BranchPredictorConfig {
	public int PCBits;
	public int BHRsize;
	public int saturating_bits;
	public int bimod_table_size;
	public int two_lev_L1;
	public int two_lev_L2;
	public int two_lev_histoyRegSize;
	public int two_lev_XOR;
	public int combConfig_metaTableSize;
	public int BTB_numSets;
	public int BTB_Assosiativity;
	public BP predictorMode;
	
	public static enum BP {
		NoPredictor, PerfectPredictor, AlwaysTaken, AlwaysNotTaken, Tournament, Bimodal, GShare, GAg, GAp, PAg, PAp
	}
}
