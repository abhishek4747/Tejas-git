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
public class PAgPredictor extends BranchPredictor {

        /*
         * int PBHT[] is a table of Branch History Register which has 2^PCBits number of entries
         * Keeps record of BHR per-branch wise
         */
        int[] PBHT;

        /*
         * int PHT[] is a global table containing the saturating counter bits
         * it is inedxed by the BHR entry in the PBHT[] table
         */
        int[] PHT;

        /*
         * maskbits are used to mask the PC address and extract the desired number of bits from PC
         */
        int maskbits;
        /*
         * saturating_states contains the maximum value a saturating counter can have, starting from 0
         * PHTsize is the number of entries the PHT array can have i.e. 2^PCBits
         * not_taken_states contains the maximum value of a saturating state that gives the prediction as NOT_TAKEN
         */
        int PHTsize;
        int saturating_states,not_taken_states;


        /**
         * <code>state</code> records the current value of the saturating counter
         */
        /**
         * <code>BHR</code> records the BHR value of current Program Counter
         */
        /**
         * <code>index</code> the value to index PBHT table
         */
        protected int state,BHR,index;

        /*
         * Constructor <code>PAg_predictor()</code>
         * takes in the values of number of LSB of PC address (PCBits),
         * the number of bits in each BHR(BHTsize),
         * number of saturating bits for counter(saturating_bits)
         * and initializes each member variable of the class
         */
        public PAgPredictor(ExecutionEngine containingExecEngine, int PCBits,int BHRsize,int saturating_bits)
        {
        		super(containingExecEngine);
        		
                maskbits=(1<<PCBits);
                PBHT=new int[maskbits];
                maskbits--;
                PHTsize=(1<<BHRsize);
                PHT=new int[PHTsize];
                PHTsize--;
                saturating_states=(1<<saturating_bits)-1;
                for(int i=0;i<=maskbits;i++)
                        PBHT[i]=PHTsize;
                for(int j=0;j<=PHTsize;j++)
                        PHT[j]=saturating_states;
                not_taken_states=(int)(saturating_states/2);
        }


        /**
         * Method code>Train()</code> used to train the BHR and the corresponding PHT
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
               index=(int)(maskbits&address);
               BHR=PBHT[index];
               state=PHT[BHR];

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
               
               PHT[BHR]=state;
               BHR=BHR<<1;
               if(outcome==true)
                       BHR++;
               BHR=BHR&PHTsize;
               PBHT[index]=BHR;

        }

        /*
         * <code>predict()</code> the branch taken or not according to the current value of member variable
         * boolean true for branch Taken
         * boolean false for Not Taken
         */
       /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
         */
        public boolean predict(long address, boolean outcome) {
               index=(int)(maskbits&address);
               BHR=PBHT[index];
               state=PHT[BHR];
               if(state<=not_taken_states)
                       return false;
               else
                       return true;

        }



}
