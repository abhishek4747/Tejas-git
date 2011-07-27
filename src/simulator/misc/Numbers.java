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

	Contributors:  Prathmesh Kallurkar
*****************************************************************************/

package misc;

public class Numbers {

	static public Long hexToLong(String hexStr)
	{
		//Remove the 0x prefix
		if(hexStr.length()>2 && hexStr.substring(0,2).contentEquals("0x"))
			hexStr = hexStr.substring(2);
		
		return new Long(Long.parseLong(hexStr,16));
	}
	
	static public boolean isValidNumber(String numStr)
	{
		if(numStr==null)
			return false;
		
		//Remove the 0x prefix
		if(numStr.length()>2 && numStr.substring(0,2).contentEquals("0x"))
			numStr = numStr.substring(2);
		
		//If conversion from string to number generates an exception then 
		// the string probably ain't a valid number
		try{
			Long.parseLong(numStr,16);
			return true;
		}catch(NumberFormatException nfe){
			return false;
		}
	}
}
