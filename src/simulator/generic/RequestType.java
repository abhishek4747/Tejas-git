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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package generic;

public enum RequestType {
	LSQ_REQUEST,
	TLB_REQUEST,
	CACHE_REQUEST,
	BUS_REQUEST,
	
	PERFORM_DECODE,
	DECODE_COMPLETE,
	ALLOC_DEST_REG,
	RENAME_COMPLETE,
	FUNC_UNIT_AVAILABLE,
	EXEC_COMPLETE,
	PERFORM_COMMITS,
	MISPRED_PENALTY_COMPLETE,
	
	//FIXME:
	READ,
	WRITE,
}
