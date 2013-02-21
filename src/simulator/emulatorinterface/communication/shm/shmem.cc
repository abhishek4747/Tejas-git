#include "shmem.h"

#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/stat.h>
#include <sys/sem.h>
#include <sys/shm.h>
#include <sys/msg.h>
#include <errno.h>
#include <pthread.h>

#include <sys/syscall.h>
#include <unistd.h>

namespace IPC
{

void
Shm::get_lock(packet *map) {
	map[COUNT+1].value = 1; 				// flag[0] = 1
	__sync_synchronize();			// compiler barriers
	map[COUNT+3].value = 1; 				// turn = 1
	__sync_synchronize();
	while((map[COUNT+2].value == 1) && (map[COUNT+3].value == 1)) {}
}

void
Shm::release_lock(packet *map) {
	map[COUNT + 1].value = 0;
	__sync_synchronize();
}


Shm::Shm ()
{
	// get a unique key
	key_t key=ftok(ftokpath,ftok_id);
	if ( key == (key_t)-1 )
	{
		perror("ftok in pin ");
		exit(1);
	}

	// get a segment for this key. This key is shared with the JNI through common.h
	int size = (COUNT+5) * sizeof(packet)*MaxNumThreads;
	if ((shmid = shmget(key, size, 0666)) < 0) {
		perror("shmget in pin ");
		exit(1);
	}

	// attach to this segment
	if ((tldata[0].shm = (packet *)shmat(shmid, NULL, 0)) == (packet *)-1) {
		perror("shmat in pin ");
		exit(1);
	}

	// initialise book-keeping variables for each of the threads
	THREAD_DATA *myData;
	for (int t=0; t<MaxNumThreads; t++) {
		myData = &tldata[t];
		myData->tlqsize = 0;
		myData->in = 0;
		myData->out = 0;
		myData->sum = 0;
		myData->tlq = new packet[locQ];
		myData->shm = tldata[0].shm+(COUNT+5)*t;		// point to the correct index of the shared memory
		myData->avail = 1;
//		myData->tid = 0;
	}
}

Shm::Shm (uint64_t pid)
{
	// get a unique key
	key_t key=ftok(ftokpath,pid);
	if ( key == (key_t)-1 )
	{
		perror("ftok in pin ");
		exit(1);
	}

	// get a segment for this key. This key is shared with the JNI through common.h
	int size = (COUNT+5) * sizeof(packet)*MaxNumThreads;
	if ((shmid = shmget(key, size, 0666)) < 0) {
		perror("shmget in pin ");
		exit(1);
	}

	// attach to this segment
	if ((tldata[0].shm = (packet *)shmat(shmid, NULL, 0)) == (packet *)-1) {
		perror("shmat in pin ");
		exit(1);
	}

	// initialise book-keeping variables for each of the threads
	THREAD_DATA *myData;
	for (int t=0; t<MaxNumThreads; t++) {
		myData = &tldata[t];
		myData->tlqsize = 0;
		myData->in = 0;
		myData->out = 0;
		myData->sum = 0;
		myData->tlq = new packet[locQ];
		myData->shm = tldata[0].shm+(COUNT+5)*t;		// point to the correct index of the shared memory
		myData->avail = 1;
//		myData->tid = 0;
	}
}


/* If local queue is full, write to the shared memory and then write to localQueue.
 * else just write at localQueue at the appropriate index i.e. at 'in'
 */
int
Shm::analysisFn (int tid,uint64_t ip, uint64_t val, uint64_t addr)
{
//	static int mem_read = 0;
	int actual_tid = tid;
	tid = memMapping[tid];
	THREAD_DATA *myData = &tldata[tid];

	// if my local queue is full, I should write to the shared memory and return if cannot return
	// write immediately, so that PIN can yield this thread.
	if (myData->tlqsize == locQ) {
		if (Shm::shmwrite(actual_tid,0, -1)==-1) return -1;
	}

	// log the packet in my local queue
	packet *myQueue = myData->tlq;
	uint32_t *in = &(myData->in);
	packet *sendPacket = &(myQueue[*in]);
//    if(val == 2)
//    {
//    	printf("mem read in shmem =  %d  \n",++mem_read);
//    	fflush(stdout);
//    }
	sendPacket->ip = (uint64_t)ip;
	sendPacket->value = val;
	sendPacket->tgt = (uint64_t)addr;

	*in = (*in + 1) % locQ;
	myData->tlqsize++;

	return 0;
}

void
Shm::onThread_start (int tid)
{
	int i;
	for(i=0;i<MaxNumThreads;i++){
		if(tldata[i].avail == 1)
		{
			tldata[i].avail=0;
			break;
		}
	}
	THREAD_DATA *myData = &tldata[i];
	packet *shmem = myData->shm;
//	myData->avail =0;
//	printf("Thread %d start alloc to %d in = %d  out=%d sum=%d prod_ptr=%d\n",tid,i,myData->in,myData->out,myData->sum,myData->prod_ptr);
	memMapping[tid] = i;
	//get_lock(shmem);
	shmem[COUNT].value = 0; // queue size pointer
	shmem[COUNT + 1].value = 0; // flag[0] = 0
	shmem[COUNT + 2].value = 0; // flag[1] = 0
	//release_lock(shmem);

}

int
Shm::onThread_finish (int tid, long numCISC)
{
	int actual_tid = tid;
	tid = memMapping[tid];   //find the mapped mem segment
	THREAD_DATA *myData = &tldata[tid];

	// keep writing till we empty our local queue
	while (myData->tlqsize !=0) {
		if (Shm::shmwrite(actual_tid,0, -1)==-1) return -1;
	}


	// last write to our shared memory. This time write a -1 in the 'value' field of the packet
	int ret = Shm::shmwrite(actual_tid,1, numCISC);

	if(ret != -1){
		myData->avail = 1;
		myData->tlqsize = 0;
	}
	return ret;
}

int Shm::onSubset_finish (int tid, long numCISC)
{
	int actual_tid = tid;
		tid = memMapping[tid];   //find the mapped mem segment
		THREAD_DATA *myData = &tldata[tid];

		// keep writing till we empty our local queue
		while (myData->tlqsize !=0) {
			if (Shm::shmwrite(actual_tid,0, -1)==-1) return -1;
		}


		// last write to our shared memory. This time write a -2 in the 'value' field of the packet
		int ret = Shm::shmwrite(actual_tid,2, numCISC);

		if(ret != -1){
			myData->avail = 1;
			myData->tlqsize = 0;
		}
		return ret;
}

/* Read at 'out' of a local queue and write as many slots available in
 * shared memory. If none available then block
 * If last is 0 then normal write and if last is 1 then write -1 at the end
 * The numCISC's value is valid only if this is the last packet
 */
int
Shm::shmwrite (int tid, int last, long numCISC)
{
	static int num_shmem=0;
	//pthread_mutex_lock(&mul_lock);
	tid = memMapping[tid];
	//pthread_mutex_unlock(&mul_lock);
	int queue_size;
	int numWrite;

	THREAD_DATA *myData = &tldata[tid];
	packet* shmem = myData->shm;

	get_lock(shmem);
	queue_size = shmem[COUNT].value;
	release_lock(shmem);
	numWrite = COUNT - queue_size;
	// if numWrite is 0 this means cant write now. So should yield.
	if (numWrite==0) return -1;

	// if last packet then write -1 else write the actual packets
	if (last ==0) {

		// write 'numWrite' or 'local_queue_size' packets, whichever is less

		numWrite = numWrite<myData->tlqsize ? numWrite:myData->tlqsize;


		for (int i=0; i< numWrite; i++) {

//			if(myData->tlq[(myData->out+i)%locQ].value== 2){
//				printf("Mem Read going to write in shmem.cc %d\t%d\n",++num_shmem,tid);
//				fflush(stdout);
//			}
			// for checksum
			myData->sum+=myData->tlq[(myData->out+i)%locQ].value;

			// copy 1 packet from local buffer to the shared memory
			get_lock(shmem);
			memcpy(&(shmem[(myData->prod_ptr+i)%COUNT]),&(myData->tlq[(myData->out+i)%locQ]),
					sizeof(packet));
			release_lock(shmem);
		}
		// some bookkeeping of the threads state.
		myData->out = (myData->out + numWrite)%locQ;
		myData->tlqsize=myData->tlqsize-numWrite;

	}
	else if(last == 1){
		numWrite = 1;
		get_lock(shmem);
		shmem[myData->prod_ptr % COUNT].value = THREADCOMPLETE;
		shmem[myData->prod_ptr % COUNT].ip = numCISC;
		release_lock(shmem);

	}
	else if(last == 2){
		numWrite = 1;
		get_lock(shmem);
		shmem[myData->prod_ptr % COUNT].value = SUBSETSIMCOMPLETE;
		shmem[myData->prod_ptr % COUNT].ip = numCISC;
		release_lock(shmem);
	}

//	// some bookkeeping of the threads state.
//	myData->out = (myData->out + numWrite)%locQ;
//	myData->tlqsize=myData->tlqsize-numWrite;
	myData->prod_ptr = (myData->prod_ptr + numWrite) % COUNT;

	// update queue_size
	get_lock(shmem);
	queue_size = shmem[COUNT].value;
	queue_size += numWrite;

	myData->tot_prod += numWrite;

	shmem[COUNT].value = queue_size;
	shmem[COUNT+4].value = myData->tot_prod;
	release_lock(shmem);

	return 0;
}

bool
Shm::unload() {
	return (shmdt(tldata[0].shm)>-1);
}

Shm::~Shm ()
{
	for (int t=0; t<MaxNumThreads; t++) {
		delete tldata[t].tlq;
	}
	shmdt (tldata[0].shm);
}


} // namespace IPC
