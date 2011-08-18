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


#include "IPCBase.h"
#include "shmem.h"

#define COUNT	(1000)

#define locQ	(50)
#define QSIZE	(locQ*sizeof(packet))

#define MASK 0x00000000ffffffff

// Defining  command line arguments
KNOB<UINT64>   KnobLong(KNOB_MODE_WRITEONCE,       "pintool",
    "map", "1", "Maps");

PIN_LOCK lock;
INT32 numThreads = 0;
UINT64 checkSum = 0;

IPC::IPCBase *tst;

VOID ThreadStart(THREADID threadid, CONTEXT *ctxt, INT32 flags, VOID *v)
{
	GetLock(&lock, threadid+1);
	numThreads++;
	printf("threads till now %d\n",numThreads);
	fflush(stdout);
	ReleaseLock(&lock);
	ASSERT(numThreads <= MaxNumThreads, "Maximum number of threads exceeded\n");

	tst->onThread_start(threadid);
}

VOID ThreadFini(THREADID tid,const CONTEXT *ctxt, INT32 flags, VOID *v)
{
	while (tst->onThread_finish(tid)==-1) {
		PIN_Yield();
	}
}

// Pass a memory read record
VOID RecordMemRead(THREADID tid,VOID * ip, VOID * addr)
{
		checkSum+=2;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t naddr = MASK & (uint64_t)addr;
		while (tst->analysisFn(tid,nip,2,naddr)== -1) {
			PIN_Yield();
		}
}

// Pass a memory write record
VOID RecordMemWrite(THREADID tid,VOID * ip, VOID * addr)
{
		checkSum+=3;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t naddr = MASK & (uint64_t)addr;
		while(tst->analysisFn(tid,nip,3,naddr)== -1) {
			PIN_Yield();
		}
}

VOID BrnFun(THREADID tid,ADDRINT tadr,BOOL taken,VOID *ip)
{
	uint64_t nip = MASK & (uint64_t)ip;
	uint64_t ntadr = MASK & (uint64_t)tadr;
		if (taken) {
			checkSum+=4;
			while (tst->analysisFn(tid,nip,4,ntadr)==-1) {
				PIN_Yield();
			}
		}
		else {
			checkSum+=5;
			while (tst->analysisFn(tid,nip,5,ntadr)==-1) {
				PIN_Yield();
			}
		}
}

VOID RegValRead(THREADID tid,VOID * ip,REG* _reg)
{
		checkSum+=6;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t _nreg = MASK & (uint64_t)_reg;
		while (tst->analysisFn(tid,nip,6,_nreg)== -1) {
			PIN_Yield();
		}
}


VOID RegValWrite(THREADID tid,VOID * ip,REG* _reg)
{

		checkSum+=7;
		uint64_t nip = MASK & (uint64_t)ip;
		uint64_t _nreg = MASK & (uint64_t)_reg;
		while (tst->analysisFn(tid,nip,7,_nreg)== -1) {
			PIN_Yield();
		}
}

// Pin calls this function every time a new instruction is encountered
VOID Instruction(INS ins, VOID *v)
{
	UINT32 memOperands = INS_MemoryOperandCount(ins);
	/*UINT32 maxWregs = INS_MaxNumWRegs(ins);
		UINT32 maxRregs = INS_MaxNumRRegs(ins);

		for(UINT32 i=0; i< maxWregs; i++) {
			REG x = REG_FullRegName(INS_RegW(ins, i));
			if (REG_is_gr(x) || x == REG_EFLAGS)
				INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)RegValWrite,IARG_THREAD_ID, IARG_INST_PTR, IARG_REG_VALUE,x,IARG_END);
		}
		for(UINT32 i=0; i< maxRregs; i++) {
			REG x = REG_FullRegName(INS_RegR(ins, i));
			if (REG_is_gr(x) || x == REG_EFLAGS)
				INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)RegValRead,IARG_THREAD_ID, IARG_INST_PTR, IARG_REG_VALUE,x,IARG_END);
		}
	 */

	if (INS_IsBranchOrCall(ins))//INS_IsIndirectBranchOrCall(ins))
	{
		INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)BrnFun, IARG_THREAD_ID, IARG_BRANCH_TARGET_ADDR, IARG_BRANCH_TAKEN, IARG_INST_PTR, IARG_END);
	}
	/*   if (INS_HasFallThrough(ins))//INS_IsIndirectBranchOrCall(ins))
     {
 	INS_InsertCall(ins, IPOINT_AFTER, (AFUNPTR)BrnFun, IARG_BRANCH_TARGET_ADDR, IARG_BRANCH_TAKEN, IARG_INST_PTR, IARG_END);
     } */
	// Iterate over each memory operand of the instruction.
	for (UINT32 memOp = 0; memOp < memOperands; memOp++)
	{
		if (INS_MemoryOperandIsRead(ins, memOp))
		{
			INS_InsertPredicatedCall(
					ins, IPOINT_BEFORE, (AFUNPTR)RecordMemRead,
					IARG_THREAD_ID,
					IARG_INST_PTR,
					IARG_MEMORYOP_EA, memOp,
					IARG_END);
		}
		// Note that in some architectures a single memory operand can be
		// both read and written (for instance incl (%eax) on IA-32)
		// In that case we instrument it once for read and once for write.
		if (INS_MemoryOperandIsWritten(ins, memOp))
		{
			INS_InsertPredicatedCall(
					ins, IPOINT_BEFORE, (AFUNPTR)RecordMemWrite,
					IARG_THREAD_ID,
					IARG_INST_PTR,
					IARG_MEMORYOP_EA, memOp,
					IARG_END);
		}
	}
}

// This function is called when the application exits
VOID Fini(INT32 code, VOID *v)
{
	printf("checkSum is %lld\n",checkSum);
	tst->unload();
}

/* ===================================================================== */
/* Print Help Message                                                    */
/* ===================================================================== */

INT32 Usage()
{
	cerr << "This tool instruments the benchmarks" << endl;
	cerr << endl << KNOB_BASE::StringKnobSummary() << endl;
	return -1;
}

/* ===================================================================== */
/* Main                                                                  */
/* ===================================================================== */

// argc, argv are the entire command line, including pin -t <toolname> -- ...
int main(int argc, char * argv[])
{

	UINT64 mask = KnobLong;
	printf("mask for pin %lld\n", mask);
	fflush(stdout);
	if (sched_setaffinity(0, sizeof(mask), (cpu_set_t *)&mask) <0) {
		perror("sched_setaffinity");
	}

	// Initialize pin
	if (PIN_Init(argc, argv)) return Usage();

	tst = new IPC::Shm ();
	PIN_AddThreadStartFunction(ThreadStart, 0);

	// Register Instruction to be called to instrument instructions
	INS_AddInstrumentFunction(Instruction, 0);

	PIN_AddThreadFiniFunction(ThreadFini, 0);

	// Register Fini to be called when the application exits
	PIN_AddFiniFunction(Fini, 0);

	// Start the program, never returns
	PIN_StartProgram();

	return 0;
}
