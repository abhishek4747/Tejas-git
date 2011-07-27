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


/*
 * shmget a shared memory area using the keys from common.h. Creates
 * a key using ftok.Creates a dummy shared memory segment and then
 * deletes it to ensure a fresh memory segment. Now create a fresh
 * segment with the parameter size. return the shmid for this segment
*/
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmget
(JNIEnv * env, jobject jobj, jint size1, jlong coremap) {

	uint64_t mask = coremap;
	if (sched_setaffinity(0, sizeof(mask), (cpu_set_t *)&mask) <0) {
		perror("sched_setaffinity");
	}

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

	//size1 is the number of packets needed in the segment.
	int size=sizeof(packet)*size1;
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
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index,jint NUMINTS) {
	packet *addr;
	addr=(packet *)(intptr_t)pointer;

	// TODO these could be saved to avoid calling again and again
	jclass cls;
	cls = (*env)->FindClass(env,"emulatorinterface/communication/Packet");
	jmethodID constr;
	constr = (*env)->GetMethodID(env,cls,"<init>","(JIJ)V");
	jvalue args[3];
	addr = &(addr[tid*(NUMINTS+5)+index]);
	args[0].j = (*addr).ip;
	args[1].i = (*addr).value;
	args[2].j = (*addr).tgt;
	jobject object;
	object = (*env)->NewObjectA(env,cls,constr,args);
	return object;
}

// Returns just the value, needed when we want to read just the "value" for lock managment
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmreadvalue
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index,jint NUMINTS) {
	packet *addr;
	addr=(packet *)(intptr_t)pointer;

	return (addr[tid*(NUMINTS+5)+index].value);
}

// Write at 'index' the value 'val'. One big segment is created for all
// threads and being indexed by the thread ids.
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmwrite 
(JNIEnv * env, jobject jobj,jint tid,jlong pointer,jint index,jint val,jint NUMINTS) {
	packet *addr;
	addr=(packet *)(intptr_t)pointer;
	addr[tid*(NUMINTS+5)+index].value=val;
	return 1;
}

// hardware barriers dont seem to work.So using compiler barriers.
JNIEXPORT void JNICALL Java_emulatorinterface_communication_shm_SharedMem_asmmfence 
(JNIEnv * env, jobject jobj) {
	asm volatile("" ::: "memory");
}

