// Do not change these encodings, change Encoding.java too, if needed.

#define MEMREAD	2
#define MEMWRITE	3
#define	TAKEN	4
#define	NOTTAKEN	5
#define	REGREAD	6
#define	REGWRITE	7

// these are for function entry, For function exit x+1 will be used
#define	BCAST	10
#define	SIGNAL	12
#define	LOCK	14
#define	UNLOCK	16
#define	JOIN	18
#define	CONDWAIT	20
#define	BARRIERWAIT	22
