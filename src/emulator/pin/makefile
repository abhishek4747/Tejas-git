
TARGET_COMPILER?=gnu

##############################################################
#
# include *.config files
#
##############################################################

ifeq ($(TARGET_COMPILER),gnu)
	include makefile.gnu.config
	CXXFLAGS ?= -Wall -Werror -Wno-unknown-pragmas $(DBG) $(OPT)
	PIN_LIBS += -lrt
endif

##############################################################
#
# Tools sets
#
##############################################################


#prelimCausality and causality were creating problems in building.
#TOOL_ROOTS = PgmInstrumentor  prelimcausalityTool causalityTool printingTool
TOOL_ROOTS = causalityTool

# Tools which are built specially, e.g. with more than one source file.
# As well as being defined here they need specific build rules for the tool.
SPECIAL_TOOL_ROOTS = 

TOOLS = $(TOOL_ROOTS:%=$(OBJDIR)%$(PINTOOL_SUFFIX))
STATIC_TOOLS = $(STATIC_TOOL_ROOTS:%=$(OBJDIR)%$(SATOOL_SUFFIX))
SPECIAL_TOOLS = $(SPECIAL_TOOL_ROOTS:%=$(OBJDIR)%$(PINTOOL_SUFFIX))
APPS_BINARY_FILES = $(APPS:%=$(OBJDIR)%)

##############################################################
#
# build rules
#
##############################################################
all: $(OBJDIR)
	-$(MAKE) make_all
tools: $(OBJDIR)
	-$(MAKE) make_tools
apps: $(OBJDIR)
	-$(MAKE) make_apps
test: $(OBJDIR)
	-$(MAKE) make_test

make_all: communication make_tools make_apps
make_tools: $(TOOLS) $(STATIC_TOOLS) $(SPECIAL_TOOLS)
make_apps: $(APPS_BINARY_FILES)
make_test: $(TOOL_ROOTS:%=%.test) $(STATIC_TOOL_ROOTS:%=%.test) $(SPECIAL_TOOL_ROOTS:%=%.test)

##############################################################
#
# communication
#
##############################################################

TOPDIR = $(shell pwd)/../../simulator/

TOPBINDIR = $(shell pwd)/../../../bin/

COMMDIR = $(TOPDIR)emulatorinterface/communication/
COMMBINDIR = $(TOPBINDIR)emulatorinterface/communication/

MMAPDIR = $(COMMDIR)mmap/
NETWORKDIR = $(COMMDIR)network/
PIPEDIR = $(COMMDIR)pipe/
SHMDIR = $(COMMDIR)shm/
SHMBINDIR = $(COMMBINDIR)shm/
FILEDIR = $(COMMDIR)filePacket/

VPATH = $(TOPDIR):$(COMMDIR):$(SHMDIR):$(FILEDIR):$(PIPEDIR):$(MMAPDIR):$(NETWORKDIR)
PINCOMM_INCLUDE = -I$(COMMDIR) -I$(SHMDIR) -I$(FILEDIR) -I$(PIPEDIR) -I$(MMAPDIR) -I$(NETWORKDIR)

JNIPACKAGE = emulatorinterface.communication.shm.SharedMem
JNINCLUDE = -I/usr/lib/jvm/java-6-openjdk/include -I/usr/lib/jvm/java-6-openjdk/include/linux -I/usr/lib/jvm/java-6-openjdk-amd64/include/ -I/usr/lib/jvm/java-7-openjdk-amd64/include/ -I/usr/lib/jvm/java-7-openjdk/include/ -I/usr/lib/jvm/java-7-openjdk-i386/include/ -I/usr/lib/jvm/java-6-openjdk-i386/include/
JNILinkingFlags = -shared -Wall -fPIC
JAVAH = javah -jni

communication: cppside $(MYOBJDIR)libshmlib.so

#library is produced in the $(MYOBJDIR)
$(MYOBJDIR)libshmlib.so: $(SHMDIR)SharedMem.h $(SHMDIR)JNIShm.c common.h
	$(CC) $(JNILinkingFlags) $(SHMDIR)JNIShm.c $(JNINCLUDE) -o $(MYOBJDIR)libshmlib.so

$(SHMDIR)SharedMem.h: $(SHMBINDIR)SharedMem.class
	$(JAVAH) -classpath $(TOPBINDIR) -o $@ $(JNIPACKAGE)

CEXEC = shmem.o mmap.o network.o pipe.o filePacket.o
PINLINK = $(patsubst %.o,$(MYOBJDIR)%.o,$(CEXEC))
cppside: $(PINLINK)

$(MYOBJDIR)%.o: %.cc %.h IPCBase.h common.h encoding.h $(MYOBJDIR)make-directory
	g++ -c -fPIC $< -o $@
##############################################################
#
# applications
#
##############################################################


##############################################################
#
# build rules
#
##############################################################

$(APPS): $(OBJDIR)make-directory

$(MYOBJDIR)make-directory:
	mkdir -p $(MYOBJDIR)
	touch $(MYOBJDIR)make-directory
	
$(OBJDIR)make-directory:
	mkdir -p $(OBJDIR)
	touch $(OBJDIR)make-directory
$(OBJDIR):
	mkdir -p $(OBJDIR)

$(OBJDIR)%.o : %.cpp $(OBJDIR)make-directory $(PINLINK)
	$(CXX) -c $(CXXFLAGS) $(PIN_CXXFLAGS) $(PINCOMM_INCLUDE) ${OUTOPT}$@ $<

ZLIB_FLAG=-lz


$(TOOLS): %$(PINTOOL_SUFFIX) : %.o
	${PIN_LD} $(PIN_LDFLAGS) $(LINK_DEBUG) ${LINK_OUT}$@ $< $(PINLINK) ${PIN_LPATHS} $(PIN_LIBS) $(ZLIB_FLAG) $(DBG)


## cleaning
clean:
	-rm -rf $(OBJDIR) $(MYOBJDIR) $(SHMDIR)SharedMem.h *.out *.tested *.failed *.makefile.copy *.out.*.* *.o
