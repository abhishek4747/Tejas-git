/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Rikita Ahuja
*****************************************************************************/



package pipeline.branchpredictor;

import pipeline.ExecutionEngine;
import config.CoreConfig;
import config.SystemConfig;

/**
 *
 * @author Rikita
 */
public class TournamentPredictor extends BranchPredictor{
        /*
         * Tournament predictors: use 2 predictors, 1
         * based on global information and 1 based on
         * local information, and combine with a selector
         * pred1 and pred2 are the objecs of the classes PAg_Predictor, PAp_predictor respectively
         */
        PAgPredictor pred1;
        PApPredictor pred2;

        /**
         * <code>PAg_pred</code> and <code>PAp_pred</code> stores the respective predictions from each predictor
         */
        boolean PAg_pred,PAp_pred;

        /**
         * <code>counter</code> is used as a selector of predictors
         */
        static int counter;
        /**
         * Constructor <code>Tournament_predictor()</code> used to instantiate the member variables
         */
        public TournamentPredictor(ExecutionEngine containingExecEngine)
        {
        	super(containingExecEngine);
        	
        	CoreConfig coreConfig = SystemConfig.core[containingExecEngine.getContainingCore().getCore_number()];
        	
                pred1=new PAgPredictor(containingExecutionEngine,
                						coreConfig.branchPredictor.PCBits,
                						coreConfig.branchPredictor.BHRsize,
                						coreConfig.branchPredictor.saturating_bits);
                pred2=new PApPredictor(containingExecutionEngine,
                						coreConfig.branchPredictor.PCBits,
										coreConfig.branchPredictor.BHRsize,
										coreConfig.branchPredictor.saturating_bits);
                counter=0;
        }
        /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @param outcome takes in the actual value of branch taken/not taken
         * @param predict takes in the value which is predicted for the corresponding address
         * <code>true</code> when branch taken otherwise <code>false</code>
         */
        public void Train(long address, boolean outcome, boolean predict) {
	        	pred1.Train(address, outcome, PAg_pred);
	            pred2.Train(address, outcome, PAp_pred);
	            if(PAg_pred!=PAp_pred)
	            {
	                    if (PAg_pred == outcome && counter != 0)
	                            counter--;
	                    else if(PAp_pred==outcome && counter!=3)
	                            counter++;
	            }
        }

        /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
         */
        public boolean predict(long address, boolean outcome) {
	        	PAg_pred=pred1.predict(address, outcome);
	            PAp_pred=pred2.predict(address, outcome);
	            if(counter==0 ||counter==1)
	                    return PAg_pred;
	            else
	                    return PAp_pred;
        }


}
