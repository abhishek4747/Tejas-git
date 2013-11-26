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
public class GApPredictor extends BranchPredictor {
        /**
         * <code>PHT[]</code>Predict History Table of size based on the input*/
        int[]PHT;

         /**
         * <code>BHR</code>--> Branch History Register keeps the record of last "BHRsize" branches
         * bit 1 is for branch taken
         * bit 0 is for branch not taken
         */
        int BHR;
        
        /*Size of Branch History Register enetered by the user
          PCBits are the number of Least Significant Bits of the Program Counter
          to be concatenated with the BHR bits*/
        int BHRsize,PCBits;

        /**
         * <code>BHRinitial</code> stores the initial value of BHR
         */
        /**
         * <code>maskbits</code> stores the bits to mask PC address
         */
        protected int BHRinitial,maskbits;
        /*
         * Constructor GApPredictor() which takes BHRsize and PCBits as its input values
         * And initialises all the member varialbles of this class
         */
        public GApPredictor(ExecutionEngine containingExecEngine, int BHRsize,int PCBits){
        		super(containingExecEngine);
        		
                this.BHRsize=BHRsize;
                this.PCBits=PCBits;
                int i,PHTsize;
                PHTsize=1<<(PCBits+BHRsize);
                PHT=new int[PHTsize];
                BHR=1<<BHRsize;
                BHR=BHR-1;
                maskbits=1<<PCBits;
                maskbits=maskbits-1;
                BHRinitial=BHR;
                for(i=0;i<PHTsize;i++)
                       PHT[i]=0;
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
        public void Train(long address, boolean outcome,boolean predict) {
                /*maskbits are used to extract the desired bits from the PC address*/
                int newaddress=(int)(address&maskbits);
                int index=BHR<<PCBits;
                index=index+newaddress;
                int state=PHT[index];


                 /*if the prediction is correct*/
                if(outcome==predict)
                {
                        if(state==2)
                                PHT[index]=3;
               
                        else if(state == 1)
                                PHT[index]=0;
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
                        PHT[index]=state;

                }
                BHR=BHR<<1;
                if(outcome==true)
                        BHR=BHR+1;
                BHR=((1<<BHRsize)-1)&BHR;
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
                /*maskbits are used to extract the desired bits from the PC address*/
                int newaddress=(int)(address&maskbits);

                /*index is the integer generated by concatenation of 'n' bit of the BHR and 'k' bit of the PCaddress
                 it is used to find out the PHT value for the current values of PC and BHR*/
                int index=BHR<<PCBits;
                index=index+newaddress;

                if(PHT[index]==0 || PHT[index]==1)
                        return false;
                else
                        return true;
        }


}
