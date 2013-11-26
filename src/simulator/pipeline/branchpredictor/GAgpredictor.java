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
public class GAgpredictor extends BranchPredictor {
        /*Predict History Table of size based on the input*/
        int[] PHT;

        /*size of Branch History Register enetered by the user*/
        int BHRsize;

       /**
         * <code>BHR</code>--> Branch History Register keeps the record of last "BHRsize" branches
         * bit 1 is for branch taken
         * bit 0 is for branch not taken
         */
        public static int BHR;
        /*contains the initial value of BHR*/
        int initialBHR;

        /*
         * Constructor <code>GAgpredictor()</code> to initialize the values of the member variables
         * Takes the size of the window of the BHR Register
         */

         public GAgpredictor(ExecutionEngine containingExecEngine, int BHRsize){
        	   super(containingExecEngine);
        	   
               this.BHRsize=BHRsize;
               initialBHR=(int) (Math.pow(2, BHRsize));
               BHR=(initialBHR)-1;
               PHT=new int[initialBHR];
               for(int i=0;i<(initialBHR);i++)
                       PHT[i]=0;
       }

       /*
        * <code>Train()</code> method used to train the BHR and the corresponding PHT
        * according to the last few branches Taken/Not Taken
        */
      /**
         *
         * @param address takes in the values the PC address whose branch has to be trained
         * @param outcome takes in the actual value of branch taken/not taken
         * @param predict takes in the value which is predicted for the corresponding address
         * <code>true</code> when branch taken otherwise <code>false</code>
         */
       public void Train(long address, boolean outcome,boolean predict) {
                int state;
                state=PHT[BHR];

                /*if the prediction is correct*/
               if(outcome==predict){
                        
                        if(state==2)
                                PHT[BHR]=3;
              
                       if(state==1)
                               PHT[BHR]=0;
               }

                /*if the prediction is false*/
               else
               {
                        if(state==0)
                                state=1;
                        else if(state==1)
                                state=2;
                        else if(state==2)
                                state=1;
                        else if(state==3)
                                state=2;
                        PHT[BHR]=state;

                }
                /*to record the new 0/1 value (at the LSB) according to the actual implemetation, in the BHR register*/
                BHR=BHR<<1;
                if(outcome==true)
                BHR=BHR+1;
                BHR=BHR&((initialBHR)-1);


        }

        /*predict the branch taken or not according to the current value of member variable*/
        /* boolean true for branch Taken
         * boolean false for Not Taken
         */
/**
  *
  * @param address takes in the values the PC address whose branch has to be trained
  * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
  */
       public boolean predict(long address, boolean outcome) {
                boolean pred;
                int current_state=PHT[BHR];
                if(current_state==0 || current_state==1)
                        pred=false;
                else
                        pred=true;
                return pred;
        }

}
