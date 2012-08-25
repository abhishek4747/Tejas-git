#ifndef H_include_shmem
#define H_include_shmem

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "../IPCBase.h"

// Must ensure that this is same as in SharedMem.java
#define COUNT	(1000)
#define locQ	(50)

namespace IPC
{

class Shm : public IPCBase
{
protected:
	int shmid;										/* shared memory segment id */

	// For keeping a record of various thread related variables
#define PADSIZE 28 //64-36, assuming it is on 32bit machine(as addresses are 32bit)
	struct THREAD_DATA
	{
		uint32_t tlqsize;							/* address of instruction */
		uint32_t in;								/* in pointer in the local queue */
		uint32_t out;								/* out pointer in the local queue */
		packet *shm;								/* thread's shared mem index pointer */
		packet *tlq;								/* local queue, write in shmem when this fils */
		uint32_t prod_ptr;							/* producer pointer in the shared mem */
		uint32_t tot_prod;							/* total packets produced */
		uint64_t sum;								/* checksum */
		uint8_t _pad[PADSIZE];						/* to handle false sharing */
	};

public:
	THREAD_DATA tldata[MaxNumThreads];
	Shm();
	Shm(uint64_t);
	Shm (uint32_t count,uint32_t localQueue);

	int analysisFn (int tid,uint64_t ip, uint64_t value, uint64_t tgt);
	void onThread_start (int tid);
	int onThread_finish (int tid);
	int shmwrite (int tid, int last);
	void get_lock(packet *map);
	void release_lock(packet *map);
	bool unload ();
	~Shm ();

};

}

#endif
