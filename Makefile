#
# If you need to debug some classpath, includes, or command line arguments
# option, then comment out MAKEFLAGS line below, or set V=1 on the command
# line before make.
#
ifdef V
else
MAKEFLAGS=-s
endif

.PHONY: all run test clean distclean

# --------------------------------------------------------------------
# Variable setup. You may want to set your editor to wrap to see the
# full CLASSPATH
# --------------------------------------------------------------------

-include .config

#
# if (and only if) GCJ native works then we build it; if you reach in directly
# and build a deep native target without GCJ being present, that's just silly
# and you deserve the breakage :).
#
ifdef GCJ
all: build/classes-dist build/native
else
all: build/classes-dist
endif

# [this  will be called by the above include if .config is missing.
# we don't call ./configure automatically to allow scope for
# manual configuration and overrides]
.config:
	echo
	echo "You need to run ./configure to check prerequisites"
	echo "and setup preferences before you can build xseq."
	( if [ ! -x configure ] ; then chmod +x configure ; echo "I just made it executable for you." ; fi )
	echo
	exit 1

# Variables we expect to be set in .config are:
#	JAVAGNOME_JARS
#	JUNIT_JARS
#	JNI_PATH
#	JAVAC[_CMD]	[expected to be 9 chars wide]
#	JAVA[_CMD]	[expected to be 9 chars wide]

# [This is just a quick sanity check]
build/config: build/dirs .config
	@echo "CHECK     build system configuration"
	( if [ ! "$(JAVAGNOME_JARS)" ] ; then echo "Sanity check failed. Run ./configure" ; exit 1 ; fi )
	touch $@

# [not in use at present; code and unit tests in stash/]
# BERKELEY_JARS=/usr/lib/db-4.2.jar

# HARDCODE icky manual override!
SMACK_JARS=/opt/sun-jdk-1.4.2.04/jre/lib/jsse.jar:/home/andrew/src/jabber/smack-1.5.0/smack.jar:/home/andrew/src/jabber/smack-1.5.0/smackx-debug.jar:/home/andrew/src/jabber/smack-1.5.0/smackx.jar

# [note that Berkeley DB isn't used just now, so its jars are not here]
CLASSPATH=$(JAVAGNOME_JARS):$(SMACK_JARS)

SOURCES_DIST=$(shell find src -name '*.java')
SOURCES_LIBS=$(shell find lib -name '*.java')
SOURCES_TESTS=$(shell find tests -name '*.java')

# [we now go to the bother of listing the .class targets individually in order
# to allow us to use gcj, which doesn't compile all the things it needs to 
# (as javac does) even though it has to finds things by scanning. This
# can considerably slow a javac build depending on the order which classes
# are encountered; oh well]
CLASSES_DIST=$(shell echo $(SOURCES_DIST) | sed -e's/\.java/\.class/g' -e's/src/tmp\/classes/g')
CLASSES_LIBS=$(shell echo $(SOURCES_LIBS) | sed -e's/\.java/\.class/g' -e's/lib/tmp\/classes/g')
CLASSES_TESTS=$(shell echo $(SOURCES_TESTS) | sed -e's/\.java/\.class/g' -e's/tests/tmp\/classes/g')

# [same thing, but this time the individual .o targets. There's no native unti
# test wrapper; _TESTS just picks up the test harness sources]
NATIVE_DIST=$(shell echo $(SOURCES_DIST) | sed -e's/\.java/\.o/g' -e's/src/tmp\/native/g')
NATIVE_LIBS=$(shell echo $(SOURCES_LIBS) | sed -e's/\.java/\.o/g' -e's/lib/tmp\/native/g')
NATIVE_TESTS=$(shell echo $(SOURCES_TESTS) | perl -p -e's/ /\n/g' | grep 'xseq/ui' | sed -e's/\.java/\.o/g' -e's/tests/tmp\/native/g')

#
# convenience target: setup pre-reqs
#
build/dirs:
	@echo "MKDIR     preping temporary files and build directories"
	-test -d build || mkdir build
	-test -d tmp/classes || mkdir -p tmp/classes
	-test -d tmp/native || mkdir -p tmp/native
	touch $@

# [these are only necessary as a defence against the system having evolved
# since it was ./configured. Java is so bad at identifying the root cause 
# being missing files that were expected that such a safety check helps
# innocent builders maintain their sanity.]
build/check-jars:
	@echo "CHECK     prerequite core jar files"
	( if [ ! "$(CLASSPATH)" ] ; then echo "\"CLASSPATH\" variable is an empty. How did you get here?" ; exit 1 ; fi )
	( for i in `echo $(CLASSPATH) | sed -e's/:/ /g'` ; do if [ ! -f $$i ] ; then echo $$i not found. ; exit 1 ; fi ; done )
	touch $@

build/check-jars-tests:
	@echo "CHECK     prerequite unit test jar files"
	( if [ ! "$(JUNIT_JARS)" ] ; then echo "\"JUNIT_JARS\" variable is an empty. How did you get here?" ; exit 1 ; fi )
	( for i in `echo $(JUNIT_JARS) | sed -e's/:/ /g'` ; do if [ ! -f $$i ] ; then echo $$i not found. ; exit 1 ; fi ; done )
	touch $@

# --------------------------------------------------------------------
# Source compilation
# --------------------------------------------------------------------

# [anything Java JVM runtime should depend on this target]
build/classes: build/classes-libs build/classes-dist build/classes-tests

build/native: build/native-libs build/native-dist build/native-tests xseq

#
# build the third party libraries whose code we ship.
#
build/classes-libs: build/config build/check-jars $(CLASSES_LIBS)
	touch $@

tmp/classes/%.class: lib/%.java
	@echo "$(JAVAC_CMD) $<"
	$(JAVAC) -d tmp/classes -classpath tmp/classes:$(CLASSPATH):lib $<


build/native-libs: build/config build/check-jars $(NATIVE_LIBS)
	touch $@

tmp/native/%.o: lib/%.java
	@echo "$(GCJ_CMD) $<"
	if [ ! -d `dirname $@` ] ; then mkdir -p `dirname $@` ; fi
	$(GCJ) -classpath $(CLASSPATH):lib -o $@ -c $<


#
# build the sources (that are part of the distributed app)
#
build/classes-dist: build/config build/check-jars build/classes-libs $(CLASSES_DIST)
	touch $@

tmp/classes/%.class: src/%.java
	@echo "$(JAVAC_CMD) $<"
	$(JAVAC) -d tmp/classes -classpath tmp/classes:$(CLASSPATH):lib:src $<


build/native-dist: build/config build/check-jars build/native-libs $(NATIVE_DIST)
	touch $@

tmp/native/%.o: src/%.java
	@echo "$(GCJ_CMD) $<"
	if [ ! -d `dirname $@` ] ; then mkdir -p `dirname $@` ; fi
	$(GCJ) -classpath $(CLASSPATH):lib:src -o $@ -c $<


#
# build the test sources
#
build/classes-tests: build/config build/check-jars-tests build/classes-dist $(CLASSES_TESTS)
	touch $@

tmp/classes/%.class: tests/%.java
	@echo "$(JAVAC_CMD) $<"
	$(JAVAC) -d tmp/classes -classpath tmp/classes:$(CLASSPATH):$(JUNIT_JARS):lib:src:tests $<

# actually, there are no "native" unit tests, but the Runners are here at the moment.
build/native-tests: build/config build/check-jars build/native-libs build/native-dist $(NATIVE_TESTS)
	touch $@

tmp/native/%.o: tests/%.java
	@echo "$(GCJ_CMD) $<"
	if [ ! -d `dirname $@` ] ; then mkdir -p `dirname $@` ; fi
	$(GCJ) -classpath $(CLASSPATH):$(JUNIT_JARS):lib:src:tests -o $@ -c $<


xseq: build/native-libs build/native-dist build/native-tests
	@echo "$(GCJ_LINK_CMD) $@"
	$(GCJ) \
		-Wl,-rpath=$(GCJ_LIB_PATH) \
		-Wl,-rpath=$(JAVAGNOME_LIB_PATH) \
		-L$(GCJ_LIB_PATH) \
		-L$(JAVAGNOME_LIB_PATH) \
		-lgtkjava-2.6 -lgladejava-2.10 \
		-classpath $(CLASSPATH):lib:src:tests \
		-fjni -O \
		--main=xseq.ui.WindowRunner -o xseq \
		$(NATIVE_LIBS) $(NATIVE_DIST) $(NATIVE_TESTS)
	

# --------------------------------------------------------------------
# Runtime convenience targets
# --------------------------------------------------------------------

test: build/unittest-xml build/unittest-domain build/unittest-network

# the point is to *run* these, so we don't touch a stamp file.
build/unittest-xml: build/classes-tests
	@echo "$(JAVA_CMD) AllXmlTests [JUnit]"
	LD_LIBRARY_PATH=$(JNI_PATH) \
	$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.services.AllXmlTests

build/unittest-domain: build/classes-tests
	@echo "$(JAVA_CMD) AllDomainTests [JUnit]"
	LD_LIBRARY_PATH=$(JNI_PATH) \
	$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.domain.AllDomainTests

build/unittest-network: build/classes-tests
	@echo "$(JAVA_CMD) AllNetworkTests [JUnit]"
	LD_LIBRARY_PATH=$(JNI_PATH) \
	$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.network.AllNetworkTests

# [this is classes and not classes-dist because at the moment any use of this 
# target is to setup, but needing a the support of a test environment, which
# WindowRunner provides]
debug: build/classes
	@echo "$(JAVA_CMD) WindowRunner --debug=main,events"
	LD_LIBRARY_PATH=$(JNI_PATH) \
	$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):$(SMACK_JARS):tmp/classes xseq.ui.XmlSequenceWindowRunner --debug=all

	#@echo "$(JAVA_CMD) WindowRunner --debug=main,events"
	#@echo "$(JAVA_CMD) TimerRunner --debug=all"
	#$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.ui.WindowRunner --debug=main,events
	#$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.ui.TimerRunner --debug=all

# [to be changed to classes-dist and xseq.client.ProcedureClient]
run: build/classes
	@echo "$(JAVA_CMD) WindowRunner"
	LD_LIBRARY_PATH=$(JNI_PATH) \
	$(JAVA) -classpath $(CLASSPATH):$(JUNIT_JARS):tmp/classes xseq.ui.WindowRunner

# --------------------------------------------------------------------
# House keeping
# --------------------------------------------------------------------

# [note that we don't remove .config here, as a) darcs doesn't pick it up
# so if it's hanging around it won't cause problems, and b) if it is removed 
# here, then `make clean all` fails]
clean:
	@echo "RM        temporary build directories"
	-rm -rf build
	-rm -rf tmp
	-rm -rf hs_err_*
	-rm -f xseq

distclean: clean
	@echo "RM        build configuration information"
	-rm -f .config .config.tmp
	@echo "RM        development artifacts"
	-rm -f share/*.gladep share/*.glade.bak share/*.gladep.bak

# --------------------------------------------------------------------
# Distribution target
# --------------------------------------------------------------------

# NOTE That this will make a tarball of *only* those sources which 
# have been recorded in Darcs - so if you've locally bumped the version, 
# but not committed that change, then the wrong version number will go out!
dist: distclean
	@echo "CHECK     pristine, fully recorded tree"
	darcs what -s | perl -n -e 'if (!/^No changes!/) { print "\nFailed: you can only run make dist from a\npristene tree or one with all changes recorded\n\n" ; exit 1 }'
	@echo "PREP      files for distribution"
	darcs dist --dist-name xseq-$(VERSION) >/dev/null
	mv xseq-$(VERSION).tar.gz /tmp
	cd /tmp && tar xzf xseq-$(VERSION).tar.gz
	cd /tmp/xseq-$(VERSION) && rm -r stash && chmod +x configure
	@echo "TAR       distribution tarball"
	tar czf xseq-$(VERSION).tar.gz -C /tmp xseq-$(VERSION)
	@echo "RM        temporary files"
	rm -r /tmp/xseq-$(VERSION)*

tarball: distclean
	@echo "TAR       backup tarball"
	tar czf xseq-$(VERSION)-snapshot.tar.gz .

