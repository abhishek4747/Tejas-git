#include <pthread.h>
#include <stdio.h>
#define NUM_THREADS    3

//pthread_barrier_t * my_barrier;

void *DoSomething(void *threadid)
{
   long tid;
   tid = (long)threadid;
   printf("Hello World! It's me, thread #%ld!\n", tid);
   fflush(stdout);
   int i,j,k,sum;
   for (i=0; i<5000; i++) {
	   //for (j=0; j<10000; j++) {
			   int a = i;
			   a++;
	   //}
   }
   pthread_exit(NULL);
}

int main (int argc, char *argv[])
{
//	my_barrier = (pthread_barrier_t*)malloc(sizeof(pthread_barrier_t));
   pthread_t threads[NUM_THREADS];
   int rc;
   void *status;
   long t;
   for(t=0; t<NUM_THREADS; t++){
      //printf("In main: creating thread %ld\n", t);
      rc = pthread_create(&threads[t], NULL, DoSomething, (void *)t);
      if (rc){
         printf("ERROR; return code from pthread_create() is %d\n", rc);
         exit(-1);
      }
   }

   for(t=0; t<NUM_THREADS; t++) {
	   rc = pthread_join(threads[t], &status);
	   if (rc) {
		   printf("ERROR; return code from pthread_join()  is %d\n", rc);
		   exit(-1);
	   }
   }
   printf("EXIT\n");

}
