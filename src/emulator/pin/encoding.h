// Do not change these encodings, change Encoding.java too, if needed.

#define THREADCOMPLETE -1
#define SUBSETSIMCOMPLETE -2

#define MEMREAD	2
#define MEMWRITE	3
#define	TAKEN	4
#define	NOTTAKEN	5
#define	REGREAD	6
#define	REGWRITE	7
#define TIMER	8

// these are for function entry, For function exit x+1 will be used
#define	BCAST	10
#define	SIGNAL	12
#define	LOCK	14
#define	UNLOCK	16
#define	JOIN	18
#define	CONDWAIT	20
#define	BARRIERWAIT	22
#define BARRIERINIT 26
#define ASSEMBLY 27
#define INSTRUCTION 28
#define INTERRUPT 30
#define PROCESS_SWITCH 31
#define DOM_SWITCH 32
#define CPL_SWITCH 34


const char* findType(int type){
	switch(type) {
	case(MEMREAD) :
			return "MEMREAD";
	case(MEMWRITE) :
				return "MEMWRITE";
	case(TAKEN) :
				return "TAKEN";
	case(NOTTAKEN) :
				return "NOTTAKEN";
	case(REGREAD) :
				return "REGREAD";
	case(REGWRITE) :
				return "REGWRITE";
	case(BCAST) :
				return "BCAST ENTER";
	case(BCAST+1) :
				return "BCAST EXIT";
	case(SIGNAL) :
				return "SIGNAL ENTER";
	case(SIGNAL+1) :
				return "SIGNAL EXIT";
	case(LOCK) :
				return "LOCK ENTER";
	case(LOCK+1) :
				return "LOCK EXIT";
	case(UNLOCK) :
				return "UNLOCK ENTER";
	case(UNLOCK+1) :
				return "UNLOCK EXIT";
	case(JOIN) :
				return "JOIN ENTER";
	case(JOIN+1) :
				return "JOIN EXIT";
	case(CONDWAIT) :
				return "WAIT ENTER";
	case(CONDWAIT+1) :
				return "WAIT EXIT";
	case(BARRIERWAIT) :
				return "BARRIER ENTER";
	case(BARRIERWAIT+1) :
				return "BARRIER EXIT";
	case(TIMER) :
				return "Timer packet";
	case(BARRIERINIT) :
				return "BARRIER INIT";
	default:
		return "ADD THIS IN encoding.h";
	}
}
