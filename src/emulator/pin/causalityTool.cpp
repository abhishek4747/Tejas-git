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

	Contributors:  Abhishek Sagar, Eldhose Peter
*****************************************************************************/
#include <iostream>
#include <fstream>
#include "pin.H"
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/shm.h>
#include <cstdlib>
#include <cstring>
#include <sched.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <time.h>
#include <sys/timeb.h>

#include "IPCBase.h"
#include "shmem.h"

#include "encoding.h"

#ifdef _LP64
#define MASK 0xffffffffffffffff
#else
#define MASK 0x00000000ffffffff
#endif

// Defining  command line arguments
KNOB<UINT64>   KnobMap(KNOB_MODE_WRITEONCE,       "pintool",
    "map", "1", "Maps");
KNOB<UINT64>   KnobIgnore(KNOB_MODE_WRITEONCE,       "pintool",
    "numIgn", "0", "Ignore these many profilable instructions");
KNOB<INT64>   KnobSimulate(KNOB_MODE_WRITEONCE,       "pintool",
    "numSim", "0", "Simulate these many profilable instructions (-1 if no subset simulation is desired)");
KNOB<UINT64>   KnobId(KNOB_MODE_WRITEONCE,       "pintool",
    "id", "1", "shm id to generate key");
KNOB<std::string>   KnobPinPointsFile(KNOB_MODE_WRITEONCE,       "pintool",
    "pinpointsFile", "nofile", "pinpoints file (pass numIgn = 0, numSim = -1)");
KNOB<std::string>   KnobStartMarker(KNOB_MODE_WRITEONCE,       "pintool",
    "startMarker", "", "start marker function name");
KNOB<std::string>   KnobEndMarker(KNOB_MODE_WRITEONCE,       "pintool",
    "endMarker", "", "end marker function name");

PIN_MUTEX lock;
INT32 numThreads = 0;
INT32 livethreads = 0;
UINT64 checkSum = 0;
IPC::IPCBase *tst;
bool pumpingStatus[MaxThreads];
ADDRINT curSynchVar[MaxThreads];
static UINT64 numIns = 0;
UINT64 numInsToIgnore = 0;
INT64 numInsToSimulate = 0;
std::string startMarker;
std::string endMarker;
BOOL ignoreActive = false;
UINT64 numCISC[MaxThreads];
UINT64 totalNumCISC;
bool threadAlive[MaxThreads];
std::string pinpointsFilename;
unsigned long * sliceArray;
int numberOfSlices;
int currentSlice;

#define PacketEpoch 50
uint32_t countPacket[MaxThreads];

// needs -lrt (real-time lib)
// 1970-01-01 epoch UTC time, 1 nanosecond resolution
uint64_t ClockGetTime() {
	timespec ts;
	clock_gettime((clockid_t)CLOCK_REALTIME, (struct timespec *)&ts);
	return (uint64_t) ts.tv_sec * 1000000000LL + (uint64_t) ts.tv_nsec;
}

// this compulsory is true if it is entering some function
// so that even if halts we have a timer packet here.
void sendTimerPacket(int tid, bool compulsory) {
	if ((countPacket[tid]++ % PacketEpoch)==0 || compulsory){
		PIN_MutexLock(&lock);
		checkSum +=TIMER;
		PIN_MutexUnlock(&lock);

		countPacket[tid]=0;
		uint64_t time = ClockGetTime();
		while (tst->analysisFn(tid, time, TIMER, 0) == -1) {
			PIN_Yield();
		}
	}
}

#define cmp(a)	(rtn_name->find(a) != string::npos)

bool isActive(int tid) {
	return pumpingStatus[tid];
}
void reActivate(int tid) {
	pumpingStatus[tid] = true;
	cout << "reAcivated " << tid << "\n";
	curSynchVar[tid] = 0;
}
void deActivate(int tid, ADDRINT addr) {
	curSynchVar[tid] = addr;
	cout << "deAcivated " << tid << "\n";
	pumpingStatus[tid] = false;
}
bool hasEntered(int tid, ADDRINT addr) {
	return (curSynchVar[tid] == addr);
}

VOID ThreadStart(THREADID threadid, CONTEXT *ctxt, INT32 flags, VOID *v) {
	PIN_MutexLock(&lock);
	numThreads++;
	livethreads++;
	threadAlive[threadid] = true;
	cout << "threads till now " << numThreads << "\n";
	fflush(stdout);
	pumpingStatus[numThreads - 1] = true;
	tst->onThread_start(threadid);
	PIN_MutexUnlock(&lock);
	ASSERT(livethreads <= MaxNumThreads, "Maximum number of threads exceeded\n");
}

VOID ThreadFini(THREADID tid, const CONTEXT *ctxt, INT32 flags, VOID *v) {

	printf("thread %d finished exec\n",tid);
	fflush(stdout);
	PIN_MutexLock(&lock);
	while (tst->onThread_finish(tid, (numCISC[tid])) == -1) {
				PIN_Yield();
		}
	cout << "wrote -1 for tid " << tid << "\n";
	livethreads--;
	threadAlive[tid] = false;
	fflush(stdout);
	PIN_MutexUnlock(&lock);
}

//Pass a memory read record
VOID RecordMemRead(THREADID tid, VOID * ip, VOID * addr) {
	if (!isActive(tid))
		return;

	if(ignoreActive)
		return;

	sendTimerPacket(tid,false);

	PIN_MutexLock(&lock);
	checkSum +=MEMREAD;
	PIN_MutexUnlock(&lock);

	uint64_t nip = MASK & (uint64_t) ip;
	uint64_t naddr = MASK & (uint64_t) addr;
	while (tst->analysisFn(tid, nip, MEMREAD, naddr) == -1) {
		PIN_Yield();
	}
}

// Pass a memory write record
VOID RecordMemWrite(THREADID tid, VOID * ip, VOID * addr) {
	if (!isActive(tid))
		return;

	if(ignoreActive)
		return;

	sendTimerPacket(tid,false);

	PIN_MutexLock(&lock);
	checkSum +=MEMWRITE;
	PIN_MutexUnlock(&lock);

	uint64_t nip = MASK & (uint64_t) ip;
	uint64_t naddr = MASK & (uint64_t) addr;
	while (tst->analysisFn(tid, nip, MEMWRITE, naddr) == -1) {
		PIN_Yield();
	}
}

VOID BrnFun(THREADID tid, ADDRINT tadr, BOOL taken, VOID *ip) {
	if (!isActive(tid))
		return;

	if(ignoreActive)
		return;

	sendTimerPacket(tid,false);

	uint64_t nip = MASK & (uint64_t) ip;
	uint64_t ntadr = MASK & (uint64_t) tadr;
	if (taken) {
		PIN_MutexLock(&lock);
		checkSum +=TAKEN;
		PIN_MutexUnlock(&lock);

		while (tst->analysisFn(tid, nip, TAKEN, ntadr) == -1) {
			PIN_Yield();
		}
	} else {
		PIN_MutexLock(&lock);
		checkSum +=NOTTAKEN;
		PIN_MutexUnlock(&lock);
		while (tst->analysisFn(tid, nip, NOTTAKEN, ntadr) == -1) {
			PIN_Yield();
		}
	}
}
VOID RegValRead(THREADID tid,VOID * ip,REG* _reg)
{
	if (ignoreActive) return;
		checkSum+=6;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t _nreg = MASK & (uint64_t)_reg;
		while (tst->analysisFn(tid,nip,6,_nreg)== -1) {
			PIN_Yield();
		}
}


VOID RegValWrite(THREADID tid,VOID * ip,REG* _reg)
{
	if (ignoreActive) return;
		checkSum+=7;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t _nreg = MASK & (uint64_t)_reg;
		while (tst->analysisFn(tid,nip,7,_nreg)== -1) {
			PIN_Yield();
		}
}
VOID CountIns()
{
	if (!ignoreActive) return;
	numIns++;
	if (numIns>numInsToIgnore) ignoreActive = false;	//activate Now
}

VOID FunStartInstrumentation() {
	//if (cmp("XXX_startInstrumentation") && numThreads > 0)
	{
		ignoreActive = false;
		numInsToIgnore = 0;
		cout << "at function " << startMarker << " : beginning instrumentation" << endl;
		cout << "ignoreActive = " << ignoreActive << " numInsToIgnore = " << numInsToIgnore << endl;
		fflush(stdout);
	}
}

VOID FunEndInstrumentation() {
	//if (cmp("XXX_startInstrumentation") && numThreads > 0)
	{
		ignoreActive = true;
		numInsToIgnore = 0xFFFFFFFFFFFFFFFF;
		cout << "at function " << endMarker << " : stopping instrumentation" << endl;
		cout << "ignoreActive = " << ignoreActive << " numInsToIgnore = " << numInsToIgnore << endl;
		fflush(stdout);
	}
}
static int barrier = 0;
VOID FunEntry(ADDRINT first_arg, UINT32 encode, THREADID tid) {
	if(encode == 22){
		cout<<"barrier in causality tool : "<<++barrier<<"\n";
		fflush(stdout);
	}
	uint64_t time = ClockGetTime();
	sendTimerPacket(tid,true);

	PIN_MutexLock(&lock);
	checkSum +=encode;
	PIN_MutexUnlock(&lock);

	uint64_t uarg = MASK & (uint64_t) first_arg;
	while (tst->analysisFn(tid, time, encode, uarg) == -1) {
		PIN_Yield();
	}
}

VOID FunExit(ADDRINT first_arg, UINT32 encode, THREADID tid) {
	uint64_t time = ClockGetTime();


	sendTimerPacket(tid,false);

	PIN_MutexLock(&lock);
	checkSum +=encode;
	PIN_MutexUnlock(&lock);

	uint64_t uarg = MASK & (uint64_t) first_arg;
	while (tst->analysisFn(tid, time, encode, uarg) == -1) {
		PIN_Yield();
	}

}
/*** Called on the initialization of a barrier  ***/
VOID BarrierInit(ADDRINT first_arg, ADDRINT val, UINT32 encode, THREADID tid) {
        PIN_MutexLock(&lock);
        checkSum +=encode;
        PIN_MutexUnlock(&lock);

        uint64_t uarg = MASK & (uint64_t) first_arg;
        uint64_t value = MASK & (uint64_t) val;
        while (tst->analysisFn(tid, value, encode, uarg) == -1) {
                PIN_Yield();
        }
}
/*** This function is called on every instruction ***/
VOID printip(THREADID tid, VOID *ip) {

	PIN_MutexLock(&lock);

	if(ignoreActive == false)
		numCISC[tid]++;
	totalNumCISC++;

	if(pinpointsFilename.compare("nofile") == 0)
	{
		if(totalNumCISC >= numInsToIgnore)
			{
				if(numInsToSimulate < 0 ||
					totalNumCISC < numInsToIgnore + numInsToSimulate)
				{
					ignoreActive = false;
				}
				else
				{
					ignoreActive = true;
				}
			}
			else
			{
				ignoreActive = true;
			}

		if(numInsToSimulate > 0 && totalNumCISC >= (numInsToIgnore + numInsToSimulate))
		{
			// Now, we will write -2 packet in shared memory.
			// This will ensure that complete emulator (PIN) gets stopped.
			while (tst->onSubset_finish((int)tid, (numCISC[tid])) == -1) {
				PIN_Yield();
			}

			cout<<"subset finish called by thread "<<tid<<endl;
			tst->setSubsetsimComplete(true);
		}
	}
	else
	{
		if(totalNumCISC >= sliceArray[currentSlice] * 3000000)
		{
			if(totalNumCISC <= (sliceArray[currentSlice] + 1) * 3000000)
			{
				ignoreActive = false;
			}
			else
			{
				ignoreActive = true;
				cout << "completed slice : " << currentSlice << "\t\ttotalNumCisc = " << totalNumCISC << "\n";
				cout << totalNumCISC << "\t\t" << (sliceArray[numberOfSlices - 1] + 1) * 3000000 << "\t\t" <<numberOfSlices<< "\n";
				currentSlice++;
			}
		}
		else
		{
			ignoreActive = true;
		}

		if(totalNumCISC > (sliceArray[numberOfSlices - 1] + 1) * 3000000)
		{
			for(int i = 0; i < MaxThreads; i++)
			{
				if(threadAlive[i] == true)
				{
					int tid_1 = i;
					cout << "attempting to write -1\n";
					while (tst->onThread_finish(tid_1, (numCISC[tid_1])) == -1) {
									PIN_Yield();
							}
					cout << "wrote -1 for tid " << tid_1 << "\n";
					livethreads--;
					threadAlive[tid_1] = false;
					fflush(stdout);
				}
			}

			if(livethreads == 0)
			{
				cout << "subset simulation complete\n";
				fflush(stdout);
				tst->unload();
				exit(0);
			}

			ASSERT(livethreads != 0, "subset sim complete, but live threads not zero!!!\n");
		}

	}

	if(ignoreActive==false) {
		// For every instruction, I am sending one Instruction packet to Tejas.
		// For rep instruction, this function is called for each iteration of rep.
		uint64_t nip = MASK & (uint64_t) ip;
		while (tst->analysisFn(tid, nip, INSTRUCTION, 1) == -1) {
			PIN_Yield();
		}
	}

	if(numCISC[tid] % 1000000 == 0 && numCISC[tid] > 0)
	{
		cout << "numCISC on thread " << tid <<" = "<<numCISC[tid] <<" ignoreActive = "<< ignoreActive <<"\n";
		fflush(stdout);
	}

	if(totalNumCISC % 1000000 == 0 && totalNumCISC > 0)
	{
		cout <<"totalNumCISC = "<<totalNumCISC <<" ignoreActive = "<< ignoreActive <<"\n";
		fflush(stdout);
	}
	PIN_MutexUnlock(&lock);

}

VOID funcHandler(CHAR* name, int a, int b, int c) {
	cout << "function encountered\n ";
	cout << "numSim = " << totalNumCISC << "\n";
}

void Image(IMG img,VOID *v) {
	RTN funcRtn = RTN_FindByName(img, "__parsec_roi_begin");
	if (RTN_Valid(funcRtn)) {
		RTN_Open(funcRtn);
		RTN_InsertCall(funcRtn, IPOINT_BEFORE, (AFUNPTR)funcHandler,
					  IARG_ADDRINT, "funcA", IARG_FUNCARG_ENTRYPOINT_VALUE,
					  0, IARG_END);
		RTN_Close(funcRtn);
	}
	funcRtn = RTN_FindByName(img, "__parsec_roi_end");
	if (RTN_Valid(funcRtn)) {
		RTN_Open(funcRtn);
		RTN_InsertCall(funcRtn, IPOINT_BEFORE, (AFUNPTR)funcHandler,
					  IARG_ADDRINT, "funcA", IARG_FUNCARG_ENTRYPOINT_VALUE,
					  0, IARG_END);
		RTN_Close(funcRtn);
	}
}

// Pin calls this function every time a new instruction is encountered
VOID Instruction(INS ins, VOID *v) {

	if(tst->isSubsetsimCompleted() == true) {
		return;
	}

	INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)printip, IARG_THREAD_ID, IARG_INST_PTR, IARG_END);

	UINT32 memOperands = INS_MemoryOperandCount(ins);

	if (INS_IsBranchOrCall(ins))//INS_IsIndirectBranchOrCall(ins))
	{
		INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR) BrnFun, IARG_THREAD_ID,
				IARG_BRANCH_TARGET_ADDR, IARG_BRANCH_TAKEN, IARG_INST_PTR,
				IARG_END);
	}

	// Iterate over each memory operand of the instruction.
	for (UINT32 memOp = 0; memOp < memOperands; memOp++) {
		if (INS_MemoryOperandIsRead(ins, memOp)) {
			INS_InsertPredicatedCall(ins, IPOINT_BEFORE,
					(AFUNPTR) RecordMemRead, IARG_THREAD_ID, IARG_INST_PTR,
					IARG_MEMORYOP_EA, memOp, IARG_END);
		}
		// Note that in some architectures a single memory operand can be
		// both read and written (for instance incl (%eax) on IA-32)
		// In that case we instrument it once for read and once for write.
		if (INS_MemoryOperandIsWritten(ins, memOp)) {
			INS_InsertPredicatedCall(ins, IPOINT_BEFORE,
					(AFUNPTR) RecordMemWrite, IARG_THREAD_ID, IARG_INST_PTR,
					IARG_MEMORYOP_EA, memOp, IARG_END);
		}
	}
}

//if (RTN_Valid(rtn) && RtnMatchesName(RTN_Name(rtn), name))

// This is a routine level instrumentation
VOID FlagRtn(RTN rtn, VOID* v) {
	RTN_Open(rtn);
	const string* rtn_name = new string(RTN_Name(rtn));
	INT32 encode;

	if (cmp("pthread_cond_broadcast"))
		encode = BCAST;
	else if (cmp("pthread_cond_signal"))
		encode = SIGNAL;
	else if (cmp("pthread_mutex_lock"))
		encode = LOCK;
	else if (cmp("pthread_mutex_unlock_"))
		encode = UNLOCK; //pthread_mutex_unlock is just a wrapper
	else if (cmp("pthread_join"))
		encode = JOIN;
	else if (cmp("pthread_cond_wait"))
		encode = CONDWAIT;
	/*** For barriers. Used for research purpose ***/
	else if (cmp("pthread_barrier_wait")){

		encode = BARRIERWAIT;
	}
	else if (cmp("parsec_barrier_wait"))
			encode = BARRIERWAIT;
	else if (cmp("pthread_barrier_init")) {
		cout << "barrier init encountered !!\n";
		encode = BARRIERINIT;
	}
	/*** For barriers. Used for research purpose ***/
	else
	{
		encode = -1;
		if(startMarker.compare("") != 0)
		{
			if(cmp(startMarker)) {
				RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR) FunStartInstrumentation,
								IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
			}
		}
		if(endMarker.compare("") != 0)
		{
			if(cmp(endMarker)) {
				RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR) FunEndInstrumentation,
								IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
			}
		}
	}

	if (encode != -1 && RTN_Valid(rtn) && encode != BARRIERINIT) {
		RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR) FunEntry,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_UINT32, encode,
				IARG_THREAD_ID, IARG_END);

		RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR) FunExit,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_UINT32, encode + 1,
				IARG_THREAD_ID, IARG_END);

	}
	else if(encode != -1 && RTN_Valid(rtn)){
		RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR) BarrierInit,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_FUNCARG_ENTRYPOINT_VALUE, 2, IARG_UINT32, encode,
				IARG_THREAD_ID, IARG_END);
	}
	RTN_Close(rtn);
}

// This function is called when the application exits
VOID Fini(INT32 code, VOID *v) {
	cout <<"checkSum is "<<checkSum<<"\n";
	fflush(stdout);
	tst->unload();
}

/* ===================================================================== */
/* Print Help Message                                                    */
/* ===================================================================== */

INT32 Usage() {
	cerr << "This tool instruments the benchmarks" << endl;
	cerr << endl << KNOB_BASE::StringKnobSummary() << endl;
	return -1;
}

/* ===================================================================== */
/* Main                                                                  */
/* ===================================================================== */

// argc, argv are the entire command line, including pin -t <toolname> -- ...
int main(int argc, char * argv[]) {

	// Knobs get initialized only after initializing PIN

	//if (numInsToIgnore>0)
	ignoreActive = true;
	/*UINT64 mask = KnobMap;

	if (sched_setaffinity(0, sizeof(mask), (cpu_set_t *)&mask) <0) {
		perror("sched_setaffinity");
	}*/

	PIN_InitSymbols();
	// Initialize pin
	if (PIN_Init(argc, argv))
		return Usage();

	numInsToIgnore = KnobIgnore;
	startMarker = KnobStartMarker;
	endMarker = KnobEndMarker;
	numInsToSimulate = KnobSimulate;
	pinpointsFilename = KnobPinPointsFile;
	UINT64 id = KnobId;

	if(startMarker.compare("") != 0)
	{
		numInsToIgnore = 0xFFFFFFFFFFFFFFFF;
	}
	if(endMarker.compare("") != 0)
	{
		numInsToSimulate = 0xFFFFFFFFFFFFFFFF;
	}

	cout << "numIgn = " << numInsToIgnore << endl;
	cout << "numSim = " << numInsToSimulate << endl;
	cout << "id received = " << id << endl;
	cout << "pinpoints file received = " << pinpointsFilename << endl;
	cout << "start marker = " << startMarker << endl;
	cout <<"end marker = " << endMarker << endl;

	if(pinpointsFilename.compare("nofile") != 0)
	{
		ifstream pinpointsFile;
		std::string line;
		numberOfSlices = 0;
		pinpointsFile.open(pinpointsFilename.c_str(), ios::in);
		while ( pinpointsFile.good() )
		{
		  getline (pinpointsFile,line);
		  numberOfSlices++;
		}
		pinpointsFile.close();

		numberOfSlices--;//required because of the way good() works
		sliceArray = new unsigned long[numberOfSlices];
		int sliceArrayIndex = 0;

		pinpointsFile.open(pinpointsFilename.c_str(), ios::in);
		while ( pinpointsFile.good() )
		{
			getline (pinpointsFile,line);
			std::string temp;
			int index = 0;
			if(line.length() != 0)
			{
				while(line.at(index) != ' ')
				{
					temp.append(1,line.at(index++));
				}
				sliceArray[sliceArrayIndex++] = strtol(temp.c_str(), NULL, 10);
			}
		}
		pinpointsFile.close();
	}

	tst = new IPC::Shm (id);

	for(int i = 0; i < MaxThreads; i++)
	{
		numCISC[i] = 0;
		threadAlive[i] = false;
	}
	totalNumCISC = 0;

	PIN_AddThreadStartFunction(ThreadStart, 0);

	// Register Instruction to be called to instrument instructions
	INS_AddInstrumentFunction(Instruction, 0);

	IMG_AddInstrumentFunction(Image,0);
	// Register ThreadFini to be called when a thread exits
	PIN_AddThreadFiniFunction(ThreadFini, 0);

	// Register FlagRtn whenever you get a routine
	RTN_AddInstrumentFunction(FlagRtn, 0);

	// Register Fini to be called when the application exits
	PIN_AddFiniFunction(Fini, 0);

	// Start the program, never returns
	PIN_StartProgram();

	return 0;
}

const char* findType(int type) {
	switch(type) {
	case(MEMREAD) :
			return "MEMREAD";
	case(MEMWRITE) :
				return "MEMWRITE";
	case(TAKEN) :
				return "TAKEN";
	case(NOTTAKEN) :
				return "NOTTAKEN";
	case(REGREAD) :
				return "REGREAD";
	case(REGWRITE) :
				return "REGWRITE";
	case(BCAST) :
				return "BCAST ENTER";
	case(BCAST+1) :
				return "BCAST EXIT";
	case(SIGNAL) :
				return "SIGNAL ENTER";
	case(SIGNAL+1) :
				return "SIGNAL EXIT";
	case(LOCK) :
				return "LOCK ENTER";
	case(LOCK+1) :
				return "LOCK EXIT";
	case(UNLOCK) :
				return "UNLOCK ENTER";
	case(UNLOCK+1) :
				return "UNLOCK EXIT";
	case(JOIN) :
				return "JOIN ENTER";
	case(JOIN+1) :
				return "JOIN EXIT";
	case(CONDWAIT) :
				return "WAIT ENTER";
	case(CONDWAIT+1) :
				return "WAIT EXIT";
	case(BARRIERWAIT) :
				return "BARRIER ENTER";
	case(BARRIERWAIT+1) :
				return "BARRIER EXIT";
	case(TIMER) :
				return "Timer packet";
	case(BARRIERINIT) :
				return "BARRIER INIT";
	default:
		return "ADD THIS IN encoding.h";
	}
}
