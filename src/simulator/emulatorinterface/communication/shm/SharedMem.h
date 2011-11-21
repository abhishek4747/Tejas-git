/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class emulatorinterface_communication_shm_SharedMem */

#ifndef _Included_emulatorinterface_communication_shm_SharedMem
#define _Included_emulatorinterface_communication_shm_SharedMem
#ifdef __cplusplus
extern "C" {
#endif
#undef emulatorinterface_communication_shm_SharedMem_MaxNumJavaThreads
#define emulatorinterface_communication_shm_SharedMem_MaxNumJavaThreads 1L
#undef emulatorinterface_communication_shm_SharedMem_EmuThreadsPerJavaThread
#define emulatorinterface_communication_shm_SharedMem_EmuThreadsPerJavaThread 32L
#undef emulatorinterface_communication_shm_SharedMem_COUNT
#define emulatorinterface_communication_shm_SharedMem_COUNT 1000L
/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmget
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmget
  (JNIEnv *, jobject, jint, jlong);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmat
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmat
  (JNIEnv *, jobject, jint);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmread
 * Signature: (IJII)Lemulatorinterface/communication/Packet;
 */
JNIEXPORT jobject JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmread
  (JNIEnv *, jclass, jint, jlong, jint, jint);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmreadvalue
 * Signature: (IJII)I
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmreadvalue
  (JNIEnv *, jclass, jint, jlong, jint, jint);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmwrite
 * Signature: (IJIII)I
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmwrite
  (JNIEnv *, jclass, jint, jlong, jint, jint, jint);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmd
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmd
  (JNIEnv *, jclass, jlong);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    shmdel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_emulatorinterface_communication_shm_SharedMem_shmdel
  (JNIEnv *, jclass, jint);

/*
 * Class:     emulatorinterface_communication_shm_SharedMem
 * Method:    asmmfence
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_emulatorinterface_communication_shm_SharedMem_asmmfence
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
