/*
 *  Here is the base class for the IPC mechanisms. Any new IPC mechanism should implement the
 * declared virtual functions. Also any variable common or independent of IPC mechanism should
 * be declared here
 */

#ifndef	H_include_IPC
#define	H_include_IPC

#include <stdint.h>
#include "common.h"

// This must be equal to the MAXNUMTHREADS*EMUTHREADS in IPCBase.java file. This is
// important so that we attach to the same sized memory segment
#define MaxNumThreads	(1000)
#define MaxThreads (10000)

namespace IPC
{

class IPCBase
{
public:

	// Initialise buffers or other stuffs related to IPC mechanisms
	IPCBase(){}

	// Fill the packet struct when doing analysis and send to Java process. This is the
	// most important function
	virtual int analysisFn (int tid,uint64_t ip, uint64_t value, uint64_t tgt)=0;

	// Things to be done when a thread is started in PIN/ application
	virtual void onThread_start (int tid)=0;

	// Things to be done when a thread is finished in PIN/ application
	virtual int onThread_finish (int tid, long numCISC)=0;

	// Deallocate any memory, delete any buffers, shared memory, semaphores
	virtual bool unload ()=0;

	virtual ~IPCBase() {}
};

}

#endif
