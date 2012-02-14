/*
 * This is the JNI file which has the implementation of the native functions declared in
 * SharedMem.java. The functions name must be according to the full package names. We also use
 * a callback in the shmread function for Packet's constructor.
 */
#define _GNU_SOURCE
#include <jni.h>
#include "SharedMem.h"
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/resource.h>
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <sys/time.h>
#include <stdint.h>
#include "../common.h"
#include <string.h>

// TODO these could be saved to avoid calling again and again,or do multiple reads to avoid
// multiple JNI calls.
jclass cls;
jmethodID constr;
jlong shmAddress;
jint gCOUNT;
jint gMaxNumJavaThreads;
jint gEmuThreadsPerJavaThread;

/*
 * shmget a shared memory area using the keys from common.h. Creates
 * a key using ftok.Creates a dummy shared memory segment and then
 * deletes it to ensure a fresh memory segment. Now create a fresh
 * segment with the parameter size. return the shmid for this segment
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmget
(JNIEnv * env, jobject jobj, jint COUNT,jint MaxNumJavaThreads,jint EmuThreadsPerJavaThread,
		jlong coremap) {
	uint64_t mask = coremap;


/*
	if (sched_setaffinity(0, sizeof(mask), (cpu_set_t *)&mask) <0) {
		perror("sched_setaffinity");
	}
*/


	int shmid;
	key_t key=ftok(ftokpath,ftok_id);
	if ( key == (key_t)-1 )
	{
		perror("ftok");
		return (-1);
	}


	// first create a dummy and delete
	shmid = shmget(key,32, IPC_CREAT | 0666);
	struct shmid_ds  sds;
	shmctl(shmid,IPC_RMID,&sds);

	//set the global variables
	gCOUNT = COUNT;
	gMaxNumJavaThreads = MaxNumJavaThreads;
	gEmuThreadsPerJavaThread = EmuThreadsPerJavaThread;

	//size1 is the number of packets needed in the segment.
	int size=sizeof(packet)*(COUNT+5)*MaxNumJavaThreads*EmuThreadsPerJavaThread;
	if ((shmid = shmget(key, size, IPC_CREAT | IPC_EXCL | 0666)) < 0) {
		perror("shmget-:");
		return (-1);
	}

	return (shmid);
}

// Attach a memory segment using the shmid generated by the shmget
// returns the pointer of the segment.
JNIEXPORT jlong JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmat 
(JNIEnv * env, jobject jobj, jint shmid) {
	packet *shm;
	if ((shm = (packet *)shmat(shmid, NULL, 0)) == (packet *) -1) {
		perror("shmat");
		return (-1);
	}
	intptr_t ret=(intptr_t)shm;
	shmAddress = ret;
	return (ret);
}

// Detach a segment using the shm pointer        
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmd 
(JNIEnv * env, jobject jobj, jlong pointer) {

	packet *addr;
	addr=(packet *)(intptr_t)pointer;
	register int rtrn;
	if ((rtrn=shmdt(addr))==-1) {
		perror("shmdt");
		exit(1);
	}

	return (rtrn);
}

// Delete a segment using shmid
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmdel
(JNIEnv * env, jobject jobj, jint shmid) {

	struct shmid_ds  shmid_ds;
	register int rtrn;
	if ((rtrn = shmctl(shmid, IPC_RMID, &shmid_ds)) == -1) {
		perror("shmdel");
		exit(1);
	}

	return (rtrn);
}

// Return a packet object
JNIEXPORT jobject JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmread
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index) {
	packet *addr;
	addr=(packet *)(intptr_t)pointer;

	cls = (*env)->FindClass(env,"emulatorinterface/communication/Packet");
	constr = (*env)->GetMethodID(env,cls,"<init>","(JJJ)V");

	jvalue args[3];
	addr = &(addr[tid*(gCOUNT+5)+index]);
	args[0].j = (*addr).ip;
	args[1].j = (*addr).value;
	args[2].j = (*addr).tgt;
	jobject object;
	object = (*env)->NewObjectA(env,cls,constr,args);
	return object;
}

// Return a packet object
JNIEXPORT void JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmreadMult
(JNIEnv * env, jclass jcls,jint tid,jlong pointer,jint index,jint num,jlongArray ret) {

	 jlongArray result;

	 packet *addr;
	 addr=(packet *)(intptr_t)pointer;
	 uint64_t *orig = (uint64_t *)&(addr[tid*(gCOUNT+5)]);
	 addr = &(addr[tid*(gCOUNT+5)+index]);

	 uint64_t *transfer = (uint64_t*)addr;

	 uint64_t *int1;
	 int1 = (uint64_t*)malloc(sizeof(uint64_t)*num*3);
	 //Do copying here
	 if (index+num<gCOUNT) {
	 		 //(*env)->SetLongArrayRegion(env, result, 0, num*3, transfer);
	 		 memcpy(int1,transfer,sizeof(uint64_t)*num*3);
	 	 }
	 	 else {
	 		 int part1 = gCOUNT-index;
	 		 int part2 = num - part1;
	 		 //(*env)->SetLongArrayRegion(env, result, 0, part1*3, transfer);
	 		 //(*env)->SetLongArrayRegion(env, result, part1*3, part2*3, orig);
	 		 memcpy(int1,transfer,sizeof(uint64_t)*part1*3);
	 		 memcpy(int1+part1*3,orig,sizeof(uint64_t)*part2*3);
	 	 }

	 (*env)->SetLongArrayRegion(env,ret,0,num*3,(jlong*)int1);
	 free(int1);
/*
	 // move from the temp structure to the java structure
	 if (index+num<gCOUNT) {
		 (*env)->SetLongArrayRegion(env, result, 0, num*3, transfer);
	 }
	 else {
		 int part1 = gCOUNT-index;
		 int part2 = num - part1;
		 (*env)->SetLongArrayRegion(env, result, 0, part1*3, transfer);
		 (*env)->SetLongArrayRegion(env, result, part1*3, part2*3, orig);
	 }
	 return result;
*/


	}

int shmreadvalue(int tid, long pointer, int index){
	packet *addr;
	addr=(packet *)(intptr_t)pointer;

	return (addr[tid*(gCOUNT+5)+index].value);
}
// Returns just the value, needed when we want to read just the "value" for lock managment
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmreadvalue
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index) {
	return shmreadvalue(tid,pointer, index);
}

void shmwrite(int tid, long pointer, int index, int val){
	packet *addr;
	addr=(packet *)(intptr_t)pointer;
	addr[tid*(gCOUNT+5)+index].value=val;
}
// Write at 'index' the value 'val'. One big segment is created for all
// threads and being indexed by the thread ids.
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmwrite 
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index,jint val) {
/*
	packet *addr;
	addr=(packet *)(intptr_t)pointer;
	addr[tid*(NUMINTS+5)+index].value=val;
*/
	shmwrite( tid, pointer, index,val);
	return 1;
}

// Return number of packets
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_numPacketsAlternate
(JNIEnv * env, jobject jobj,jint tidApp) {
	shmwrite(tidApp,shmAddress,gCOUNT+2,1);
			asm volatile("" ::: "memory");
			shmwrite(tidApp,shmAddress,gCOUNT+3,0);
			asm volatile("" ::: "memory");
			while( (shmreadvalue(tidApp,shmAddress,gCOUNT+1) == 1) &&
					(shmreadvalue(tidApp,shmAddress,gCOUNT+3) == 0)) {
			}

			int size = shmreadvalue(tidApp, shmAddress, gCOUNT);

					//release_lock(tidApp, shmAddress, COUNT);
					shmwrite(tidApp,shmAddress, gCOUNT+2,0);
					return size;
}

// hardware barriers dont seem to work.So using compiler barriers.
JNIEXPORT void JNICALL Java_emulatorinterface_communication_shm_SharedMem_asmmfence 
(JNIEnv * env, jobject jobj) {
	asm volatile("" ::: "memory");
}
