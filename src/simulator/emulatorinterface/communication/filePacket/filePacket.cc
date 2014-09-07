#include "filePacket.h"

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

FilePacket::FilePacket(int maxNumActiveThreads, const char *baseFileName, void (*lock)(int), void (*unlock)(int)) : IPCBase(maxNumActiveThreads, lock, unlock)
{
	files = new gzFile[maxNumActiveThreads];
	this->baseFileName = baseFileName;
	this->maxNumActiveThreads = maxNumActiveThreads;
	isSubsetsimComplete = false;

	// If there is an existing file which can be overlapped due to the execution of pintool,
	// flag an error and exit
	for(int tid=0; tid<maxNumActiveThreads; tid++) {
		char fileName[1000];
		sprintf(fileName, "%s_%d.gz", baseFileName, tid);
		FILE *f = fopen(fileName, "r");
		if(f!=NULL) {
			printf("Cannot overwrite an existing trace file : %s\n", fileName);
			exit(1);
		}
	}

	// Initialize the files array
	for(int tid=0; tid<maxNumActiveThreads; tid++) {
		files[tid] = NULL;
	}
}

void 
FilePacket::createPacketFileForThread(int tid)
{
	if(files[tid]==NULL) {
		char fileName[1000];
		sprintf(fileName, "%s_%d.gz", baseFileName, tid);
		FILE *fd = fopen(fileName, "w");
		if(fd==NULL) {
			perror("error in creating file !! ");
			exit(1);
		}

		files[tid] = gzdopen(fileno(fd), "w6");
	}
}

/* If local queue is full, write to the shared memory and then write to localQueue.
 * else just write at localQueue at the appropriate index i.e. at 'in'
 */
int
FilePacket::analysisFn (int tid,uint64_t ip, uint64_t val, uint64_t addr)
{
	(*lock)(tid);
	createPacketFileForThread(tid);	
	if(val==INSTRUCTION) {
		printf("Error in writing INSTRUCTION packet to trace file !!");
		exit(1);
	}

	//printf("%ld %ld %ld\n", ip, val, addr); fflush(stdout);

	gzprintf(files[tid], "%ld %ld %ld\n", ip, val, addr);
	(*unlock)(tid);
	return 0;
}

int
FilePacket::analysisFnAssembly (int tid,uint64_t ip, uint64_t val, char *asmString)
{
	(*lock)(tid);
	createPacketFileForThread(tid);	

	//printf("%ld %ld %s\n", ip, val, asmString); fflush(stdout);

	gzprintf(files[tid], "%ld %ld %s\n", ip, val, asmString);

	(*unlock)(tid);
	return 0;
}

void
FilePacket::onThread_start (int tid)
{
}

int
FilePacket::onThread_finish (int tid, long numCISC)
{
	return 0;
}

int FilePacket::onSubset_finish (int tid, long numCISC)
{
	while(analysisFn(tid, 0, SUBSETSIMCOMPLETE, numCISC)==-1) {
		continue;
	}
	
	return 0;
}

void
FilePacket::closeAllFiles() {
	printf("close all files \n"); fflush(stdout);
	for(int i=0; i<MaxNumActiveThreads; i++) {
		gzclose(files[i]);
	}
}

bool
FilePacket::unload()
{
	if(files==NULL) {
		return true;
	}

	closeAllFiles();
	delete files;
}

FilePacket::~FilePacket ()
{
	if(files==NULL) {
		return;
	}

	unload();
}

bool
FilePacket::setSubsetsimComplete(bool val)
{
	isSubsetsimComplete = val;
}

bool
FilePacket::isSubsetsimCompleted()
{
	return isSubsetsimComplete;
}


} // namespace IPC
