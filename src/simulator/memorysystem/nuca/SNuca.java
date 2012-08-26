/*****************************************************************************
				BhartiSim Simulator
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

				Contributor: Mayur Harne
*****************************************************************************/

package memorysystem.nuca;
import java.util.Vector;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class SNuca extends NucaCache
{
	public SNuca(CacheConfig cacheParameters, CoreMemorySystem containingMemSys) {
        super(cacheParameters,containingMemSys);
    }
		
	public Vector<Integer> getDestinationBankId(long addr)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		int bankNumber = getBankNumber(addr);
		bankId.add(bankNumber /cacheColumns);
		bankId.add(bankNumber%cacheColumns);
		return bankId;
	}
	
	public Vector<Integer> getSourceBankId(long addr,int coreId)
	{
		Vector<Integer> bankId = new Vector<Integer>();
		bankId.add(coreId/cacheColumns);
		bankId.add(coreId%cacheColumns);
		return bankId;
	}
}
