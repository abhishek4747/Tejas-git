#ifndef H_include_mmap
#define H_include_mmap

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "../IPCBase.h"

namespace IPC
{

class mmap:public IPCBase
{
protected:

public:
	mmap();
	void analysisFn (int tid,uint64_t ip, int value, uint64_t tgt);
	void onThread_start (int tid);
	void onThread_finish (int tid);
	void shmwrite (int tid, int last);
	void get_lock(packet *map);
	void release_lock(packet *map);
	bool unload ();    
	~mmap();
};

}

#endif
