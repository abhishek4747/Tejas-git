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
public class GShare extends BranchPredictor{

         /*<code>PHT[]</code> Predict History Table of size based on the input*/
        int[] PHT;

        /*<code>BHRsize </code>size of Branch History Register enetered by the user*/
        int BHRsize;

       
        /**
         * <code>BHR</code> Branch History Register keeps the record of last "BHRsize" branches
         * bit 1 is for branch taken
         * bit 0 is for branch not taken
         */
        public static int BHR;


        /*
         * saturating_states contains the maximum value a saturating counter can have, starting from 0
         * PHTsize is the number of entries the PHT array can have i.e. 2^PCBits
         * maskbits are used to extract the desired bits from the PC address
         * not_taken_states contains the maximum value of a saturating state that gives the prediction as NOT_TAKEN
         */
        int saturating_states,PHTsize,maskbits,not_taken_states;


        /*
         * Constructor <code> GShare() </code> to take the value of PCBits/BHRsize and number of saturating_bits
         * Also it initialises values of all the member variable of the class
         */
        public GShare(ExecutionEngine containingExecEngine, int BHRsize,int saturating_bits)
        {
        		super(containingExecEngine);
        		
                this.BHRsize=BHRsize;
                this.saturating_states=(1<<saturating_bits)-1;
                BHR=1<<BHRsize;
                PHTsize=BHR;
                BHR=BHR-1;
                PHT=new int[PHTsize];
                for(int i=0;i<PHTsize;i++)
                        PHT[i]=saturating_states;
                maskbits=BHR;
                not_taken_states=(int)(saturating_states/2);
        }


        /*
         * Method Train used to train the BHR and the corresponding PHT
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
               int index;
               index=(int)address&maskbits;
               index=index^BHR;
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
               BHR=BHR<<1;
               if(outcome==true)
                        BHR=BHR+1;
               BHR=maskbits&BHR;

        }
        /* predict the branch taken or not according to the current value of member variable
         * boolean true for branch Taken
         * boolean false for Not Taken
         */
        /**
         * @param address takes in the values the PC address whose branch has to be trained
         * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
         */
        public boolean predict(long address, boolean outcome) {
               int index;
               index=(int)address&maskbits;
               index=index^BHR;
               
               int state=PHT[index];
               if(state<=not_taken_states)
                       return false;
               else
                       return true;

        }

}
