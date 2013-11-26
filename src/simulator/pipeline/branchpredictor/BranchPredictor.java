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

	Contributors:  a) Abhishek (Dual Degree) Interface from PIN to Java
                           b) Rikita (B.Tech): Branch Predictor
                           c) Prathmesh (M.Tech): CISC to RISC micro-op translation
                           d) Rajshekhar (M.Tech): Pipeline
                           e) Moksh (M.Tech): Memory system

*****************************************************************************/



package pipeline.branchpredictor;

import java.io.FileWriter;
import java.io.IOException;

import config.PowerConfigNew;

import pipeline.ExecutionEngine;
import generic.Core;

/**
 *
 * @author Rikita
 */

/*Class Interface BranchPredictor
 * Has to be implemented by all types of BranchPRedictor
 */
public abstract class BranchPredictor {
	
	ExecutionEngine containingExecutionEngine;
	long numAccesses;
	
	public BranchPredictor(ExecutionEngine containingExecutionEngine) {
		this.containingExecutionEngine = containingExecutionEngine;
	}
	
  /**
   *
   * @param address takes in the values the PC address whose branch has to be trained
   * @param outcome takes in the actual value of branch taken/not taken
   * @param predict takes in the value which is predicted for the corresponding address
   * <code>true</code> when branch taken otherwise <code>false</code>
   */
    public abstract void  Train(long address, boolean outcome,boolean predict);
 /**
  *
  * @param address takes in the values the PC address whose branch has to be trained
  * @return <code>true</code> when prediction is branch taken otherwise <code>false</code>
  * NOTE : the outcome field is useful only in the NoPredictor and PerfectPredictor cases
  */
public abstract  boolean predict(long address, boolean outcome);

public PowerConfigNew calculateAndPrintPower(FileWriter outputFileWriter, String componentName) throws IOException
{
	double leakagePower = containingExecutionEngine.getContainingCore().getbPredPower().leakagePower;
	double dynamicPower = containingExecutionEngine.getContainingCore().getbPredPower().dynamicPower;
	
	double activityFactor = (double)numAccesses
								/(double)containingExecutionEngine.getContainingCore().getCoreCyclesTaken();
	
	PowerConfigNew power = new PowerConfigNew(leakagePower, dynamicPower * activityFactor);
	
	outputFileWriter.write("\n" + componentName + " :\n" + power + "\n");
	
	return power;
}

public void incrementNumAccesses(int incrementBy)
{
	numAccesses += incrementBy * containingExecutionEngine.getContainingCore().getStepSize();
}

}
