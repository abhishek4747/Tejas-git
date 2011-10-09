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
//	TLB_SEARCH,
//	TLB_ADDRESS_READY,
//	CACHE_REQUEST,
//	BUS_REQUEST,
//	PORT_REQUEST,
	
	PERFORM_DECODE,
	DECODE_COMPLETE,
	ALLOC_DEST_REG,
	RENAME_COMPLETE,
	FUNC_UNIT_AVAILABLE,
	LOAD_ADDRESS_COMPUTED,
	EXEC_COMPLETE,
	WRITEBACK_ATTEMPT,
	WRITEBACK_COMPLETE,
	PERFORM_COMMITS,
	MISPRED_PENALTY_COMPLETE,
	BOOT_PIPELINE,
	
	Tell_LSQ_Addr_Ready,
	Validate_LSQ_Addr,
	Cache_Read,
	Cache_Write,
	Main_Mem_Read,
	Main_Mem_Write,
	Mem_Response,
	LSQ_Commit
	
//	MEM_READ,
//	MEM_WRITE,
//	
//	EVICTION_WRITE,
//	MEM_BLOCK_READY,
//	ADD_LSQ_ENTRY,
//	VALIDATE_LSQ_ENTRY,
//	LSQ_COMMIT,
//	LSQ_LOAD_COMPLETE,
//	MAIN_MEM_ACCESS_TLB
}
