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

/**
 *
 * @author Rikita
 */
public class BimodalPredictor extends BranchPredictor {

         /*Predict History Table of size based on the input*/
        int[] PHT;

         /*
         * saturating_states contains the maximum value a saturating counter can have, starting from 0
         * PHTsize is the number of entries the PHT array can have i.e. 2^PCBits
         * maskbits are used to extract the desired bits from the PC address
         * not_taken_states contains the maximum value of a saturating state that gives the prediction as NOT_TAKEN
         */
        int PCBits,saturating_states,maskbits,not_taken_states;

         /*
         * Constructor Bimodal_Predictor() to take the value of PCBits and number of saturating_bits
         * Also it initialises values of all the member variable of the class
         */ 
        public BimodalPredictor(ExecutionEngine containingExecEngine,int PCBits,int saturating_bits)
        {
        		super(containingExecEngine);
        		
                this.PCBits=PCBits;
                this.saturating_states=(1<<saturating_bits)-1;
                maskbits=(1<<PCBits)-1;
                PHT=new int[maskbits+1];
                for(int i=0;i<=maskbits;i++)
                        PHT[i]=saturating_states;
                not_taken_states=(int)(saturating_states/2);
        }

        /*
         * Method <code>Train()</code> used to train the BHR and the corresponding PHT
         * according to the last few branches Taken/Not Taken
         */
        /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @param outcome takes in the actual value of branch taken/not taken
         * @param predict takes in the value which is predicted for the corresponding address
         * <code>true</code> when branch taken otherwise <code>false</code>
         */
        public void Train(long address, boolean outcome, boolean predict) {
                int index=(int)(address&maskbits);
                int state=PHT[index];
                if(predict==outcome)
                {
                        if(outcome && state!=saturating_states)
                                state++;
                        else if(!outcome && state!=0)
                                state--;

                }
                else
                {
                        if(!predict && state!=saturating_states)
                                state++;
                        else if(predict && state!=0)
                                state--;
                }
               PHT[index]=state;

        }

        /*
         * predict the branch taken or not according to the current value of member variable
         * boolean true for branch Taken
         * boolean false for Not Taken
         */
        /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
         */
        public boolean predict(long address, boolean outcome) {
                int index=(int)(address&maskbits);
                int state=PHT[index];
                if(state<=not_taken_states)
                        return false;
                else
                        return true;
        }
}
