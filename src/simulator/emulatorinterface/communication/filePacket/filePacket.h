#ifndef H_include_filePacket
#define H_include_filePacket

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "../IPCBase.h"
#include <zlib.h>
#include "../../../../emulator/pin/encoding.h"

namespace IPC
{

class FilePacket : public IPCBase
{
protected:
	const char *baseFileName;
	int maxNumActiveThreads;
	
	// Once subset sim complete boolean variable is set, we should not write to shared memory any further.
	volatile bool isSubsetsimComplete;

	gzFile *files;
	void createPacketFileForThread(int tid);
	void closeAllFiles();

public:
	FilePacket(int maxNumThreads, const char *baseFileName, void (*lock)(int), void (*unlock)(int));
	bool isSubsetsimCompleted(void);
	bool setSubsetsimComplete(bool val);

	int analysisFn (int tid,uint64_t ip, uint64_t value, uint64_t tgt);
	int analysisFnAssembly (int tid,uint64_t ip, uint64_t value, char *asmString);

	void onThread_start (int tid);
	int onThread_finish (int tid, long numCISC);
	int onSubset_finish (int tid, long numCISC);

	bool unload ();
	~FilePacket ();
};

}

#endif
