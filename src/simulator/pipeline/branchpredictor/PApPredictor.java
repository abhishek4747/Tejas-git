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
public class PApPredictor extends BranchPredictor{

        /*
         * int <code>PBHT[]</code> is a table of Branch History Register which has 2^PCBits number of entries
         * Keeps record of BHR per-branch wise
         */
        int[] PBHT;

        /*
         * int <code>PPHT[]</code> is a Per-Branch PHT table containing the saturating counter bits
         * it is inedxed by the BHR entry in the PBHT[] table
         */
        int[][] PPHT;

        /*
         * maskbits are used to mask the PC address and extract the desired number of bits from PC
         */
        int maskbits;
        /**
         * <code>saturating_states</code>  contains the maximum value a saturating counter can have, starting from 0
         * <code>PHTsize</code> is the number of entries the PHT array can have i.e. 2^PCBits
         * <code>not_taken_states</code> contains the maximum value of a saturating state that gives the prediction as NOT_TAKEN
         */
        int PHTsize;
        int saturating_states,not_taken_states;



        protected int state,BHR,index;

        /**
         * Constructor PAp_predictor()
         * @param PCBits takes in the value of number of LSB of PC address to be taken,
         * @param BHTsize the number of bits in each BHR,
         * @param saturating_bits number of saturating bits for counter
         * and initializes each member variable of the class
         */
        public PApPredictor(ExecutionEngine containingExecEngine, int PCBits,int BHRsize,int saturating_bits)
        {
        		super(containingExecEngine);
        		
                maskbits=(1<<PCBits);
                PBHT=new int[maskbits];
                PHTsize=(1<<BHRsize);
                PPHT=new int[maskbits][PHTsize];
                PHTsize--;
                maskbits--;
                saturating_states=(1<<saturating_bits)-1;
                for(int i=0;i<=maskbits;i++)
                        PBHT[i]=PHTsize;
                for(int i=0;i<=maskbits;i++)
                {
                        for(int j=0;j<=PHTsize;j++)
                                PPHT[i][j]=saturating_states;
                }
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
               index=(int)(maskbits&address);
               BHR=PBHT[index];
               state=PPHT[index][BHR];
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
               PPHT[index][BHR]=state;
               BHR=BHR<<1;
               if(outcome==true)
                       BHR++;
               BHR=BHR&PHTsize;
               PBHT[index]=BHR;

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
               index=(int)(maskbits&address);
               BHR=PBHT[index];
               state=PPHT[index][BHR];
               if(state<=not_taken_states)
                       return false;
               else
                       return true;

        }



}
