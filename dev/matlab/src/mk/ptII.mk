# Makefile for Ptolemy II standalone distribution
#
# Version identification:
# $Id: ptII.mk.in,v 1.192.2.2 2007/02/04 03:37:54 cxh Exp $
#
# Copyright (c) 1996-2007 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY
# Date of creation: 7/31/96
# Author: Christopher Hylands

# NOTE: Don't edit this file if it is called ptII.mk, instead
# edit ptII.mk.in, which is read by configure

# Every Ptolemy II makefile should include ptII.mk

# Variables with @ around them are substituted in by the configure script

# Default top-level directory.  Usually this is the same as $PTII
prefix =	/user/fviale/home/eclipse_workspace/ProActive_Latest/dev/matlab/src

# Usually the same as prefix.  exec_prefix is part of the autoconf standard.
exec_prefix =	${prefix}

# Source directory we are building from.
srcdir =	.



BIN_INSTALL_DIR =	$(exec_prefix)/bin

# The home of the Java Developer's Kit (JDK)
# Generating Java documentation uses this makefile variable
# The line below gets substituted by the configure script
# Under Cygwin, PTJAVA_DIR _will_ contain /cygdrive/c
# NOTE: If your javac compiler is in /usr/bin, then PTJAVA_DIR is
# likely to be set to /usr.  Consider using PTJAVA_HOME if
# you are looking for Java jar files.  Note that PTJAVA_HOME is set to
# the jre/ directory, wheras PTJAVA_DIR is set to the directory 
# above the jre/ directory.
PTJAVA_DIR = 	/usr

# Value of the java.home Java property, which usually refers to the jre.
# Under Cygwin, PTJAVA_HOME will not contain /cygdrive/c
PTJAVA_HOME =	/usr/lib/jvm/java-1.4.2-gcj-1.4.2.0/jre

# Location of rt.jar, usually $(PTJAVA_HOME)/lib/rt.jar
# However, we have to be careful of backslashes and /cygwin
# Soot uses this variable to find java.lang.Object
JAVA_SYSTEM_JAR = ${PTJAVA_HOME}/lib/rt.jar

# Location of jce.jar, usually $(PTJAVA_HOME)/lib/jce.jar
# Java 1.5 has javax.crypto.Cipher in jce.jar
# However, we have to be careful of backslashes and /cygwin
# Soot uses this variable to find java.lang.Object
JAVAX_CRYPTO_JAR = ${PTJAVA_HOME}/lib/jce.jar

# JDK Version from the java.version property
JVERSION =	1.4.2

# Java CLASSPATH separator
# For Unix, this would be :
# For Cygwin, this would be ;
CLASSPATHSEPARATOR = :

# The home of the Java Foundation Classes (JFC) aka Swing
JFCHOME = 	/opt/swing

# CLASSPATH necessary to find the swing.jar file for JFC
JFCCLASSPATH = $(JFCHOME)/swing.jar

# The variables below are for the SunTest JavaScope code coverage tool
# See http://www.suntest.com/JavaScope
# The 'jsinstr' command, which instruments Java code.
JSINSTR = 	jsinstr
JSINSTRFLAGS = 	-IFLUSHCLASS=true
# The 'jsrestore' command which uninstruments Java code.
JSRESTORE =	jsrestore
# The pathname to the JavaScope.zip file
JSCLASSPATH = 	/users/ptdesign/vendors/sun/JavaScope/JavaScope.zip

# KVM is the JDK for PalmOS, see
# http://java.sun.com/products/kvm
KVM_CLASSES = $(KVM_DIR)/bin/api/classes

# Directory that contains the kvm used by PalmOS
KVM_DIR = ${PTII}/vendors/sun/j2me_cldc

# Directory the Palm Pilot Hot Sync program looks for downloads
PALM_ADD_ON_DIR = /Program\ Files/Palm/Add-on

# Set to the location of the QTJava.zip file
QTJAVA_ZIP = 

# Jar files used Soot - a Java Optimization Framework
SOOT_CLASSES = ${SOOT_DIR}/sootclasses.jar${CLASSPATHSEPARATOR}${SOOT_DIR}/jasminclasses.jar${CLASSPATHSEPARATOR}${SOOT_DIR}/polyglotclasses-1.3.2.jar${CLASSPATHSEPARATOR}${JAVA_SYSTEM_JAR}${CLASSPATHSEPARATOR}${JAVAX_CRYPTO_JAR}

# Directory that contains the Soot installation
SOOT_DIR = ${PTII}/lib

# Tini is a single board Java processor from Dallas Semi (www.dalsemi.com)
TINI_CLASSES = ${TINI_DIR}/bin/tini.jar

# Directory that contains the TINI installation
TINI_DIR = ${PTII}/vendors/dalsemi/tini

# Location of tools.jar, usually $(PTJAVA_HOME)/../lib/tools.jar
TOOLS_JAR =	${PTJAVA_HOME}/../lib/tools.jar

# Waba is another JDK for PalmOS, see
# http://www.wabasoft.clom
WABA_CLASSES = $(WABA_DIR)/classes

# Directory that contains the waba used by PalmOS
WABA_DIR = ${PTII}/vendors/misc/waba/wabasdk

# Lejos is a JDK for Lego Mindstorms, see
# http://lejos.sourceforge.net
LEJOS_DIR = ${PTII}/vendors/lejos/lejosBeta3

########## You should not have to change anything below this line ######

# Set to backtrack if Java 1.5 or later is present used in ptolemy/makefile
PTBACKTRACK_DIR =		backtrack

# Set to eclipse and used in ptolemy/backtrack/makefile if Eclipse was found
PTBACKTRACK_ECLIPSE_DIR =	

# Plugin Eclipse jar files, used by $PTII/doc
PTBACKTRACK_ECLIPSE_DOC_JARS =	

# Eclipse jar files used by ptolemy/backtrack 
PTBACKTRACK_ECLIPSE_JARS =	

# Ptolemy II packages that use backtrack/eclipse packages
# PTBACKTRACK_ECLIPSE_PACKAGES is used in $PTII/doc/makefile
PTBACKTRACK_ECLIPSE_PACKAGES =  

# Used by Mescal. Set to -I$PTII/mescal/include or -I/usr/local/include
# if we found boost/static_assert.hpp
PTBOOST_INCLUDE =	""

# Directory that contains the Cal jar file ptCal.jar
# used by bin/ptinvoke.in
CALTROP_DIR = ${PTII}/lib

# Directory that contains chic.jar.
# Chic is a modular verifier for behavioral compatibility checking of
# software and hardware components.
# Used in bin/ptinvoke.in
CHIC_DIR = ${PTII}/lib

# Directory that contains colt.jar
# Colt is a "Open Source Libraries for High Performance Scientific
# and Technical Computing in Java" used in bin/ptinvoke.in
COLT_DIR = ${PTII}/vendors/misc/colt

# Directory that contains the CaffeineMark Java Benchmark kit
# used by C code generation in copernicus/c
CMKIT_DIR = ${PTII}/vendors/cm

# Location of Eclipse, which is used by ptolemy/backtracking
ECLIPSE_DIR = 

# Directory that contains gc.h, used in copernicus/c
GC_INCLUDE_DIR = 

# GC_LD_DIRECTIVE is set to the values to pass to cc or ld if
# GC_malloc() can be found either in the default compiler location
# or in $PTII/lib.
# PTGC_LD_DIRECTIVE is used in ptolemy/copernicus/c/
GC_LD_DIRECTIVE = 

# The 'javac' compiler.
JAVAC = 	$(PTJAVA_DIR)/bin/javac

# Flags to pass to javac.  Usually something like '-g -depend'
JDEBUG =	-g
JOPTIMIZE =	-O
JFLAGS = 	$(JDEBUG) $(JOPTIMIZE) -warn:-serial

# The javadoc binary, used to create html documentation of java classes.
JAVADOC = 	/usr/bin/javadoc

# -breakiterator is only present in jdk1.4 and later
JDOCBREAKITERATOR = -breakiterator

# Increase the amount of memory that javadoc uses.
JDOCMEMORY =	-J-Xmx350m

# Allow @Pt.AcceptedRating and @Pt.ProposedRating tags.
JDOCTAG = 	

# Javadoc taglet
# Usually -tagletpath $(PTII) -taglet doc.doclets.RatingTaglet
JDOCTAGLET = 	

# Doccheck is a doclet that checks for bugs
# Location of the doccheck jar file, available from
# http://java.sun.com/developer/earlyAccess/doccheck/
DOCCHECKJAR =	$(PTII)/vendors/sun/doccheck1.2b2/doccheck.jar
JDOCCHECKFLAG = -doclet com.sun.tools.doclets.doccheck.DocCheck  \
	-docletpath $(DOCCHECKJAR)

# caltrop uses the assert keyword, which is new in Java 1.4
# ptalon uses generics, which are new in Java 1.5, so if
# Java 1.5 is present, use -source 1.5
JDOCSOURCEFLAGS = -source 1.4

# Doccheck is a doclet that checks for bugs, see
# http://java.sun.com/developer/earlyAccess/doccheck/
JDOCCHECKFLAGS = $(JDOCBREAKITERATOR) $(JDOCMEMORY) $(JDOCSOURCEFLAGS)

# Flags to pass to javadoc.
JDOCFLAGS = 	-author -version $(JDOCCHECKFLAGS) $(JDOCTAG) $(JDOCTAGLET)

# The jar command, used to produce jar files, which are similar to tar files
JAR =		$(PTJAVA_DIR)/bin/jar

# If Jar fails with a message about stack size being too small, 
# set JAR_FLAGS to -JXss20m
JAR_FLAGS = 

# Command to run that indexes a jar file named tmp.jar
# Usually it looks like '"$(JAR)" -i $@'
JAR_INDEX =	"$(JAR)" $(JAR_FLAGS) -i $@

# The 'java' interpreter.
JAVA =		$(PTJAVA_DIR)/bin/java

# Flags to use with java.  Try 'java -help' or 'java -X'
# A common value is -Xmx100m to set the maximum stack size
JAVAFLAGS = 

# The 'rmic' command - The Java Remote Method Invocation (RMI) command
RMIC =		$(PTJAVA_DIR)/bin/rmic

# Jar files for Java Advanced Imaging (JAI) used by
# ptolemy/actor/lib/jai/makefile
JAI_JARS =	

# Jar file that contains JDHL, see http://www.jhdl.org.
JHDL_JAR =	${PTII}/vendors/jhdl/ptjhdllib/JHDL.jar

# If Jini is present, the set to yes, otherwise, set to no
JINI_PRESENT =  no

# Directory that contains the Java Serial Comm API, see
# http://java.sun.com/products/javacomm/
COMMAPI_DIR =   ${PTII}/vendors/sun/commapi

# ImageJ Jar file, used to compile ptolemy/domains/gr/lib/vr
IMAGEJ_JAR =	

# Jini home directory, see http://www.sun.com/jini/
JINI_DIR =	$PTII/ptolemy/distributed/jini

# The directory of the Jini distribution that contains the jar files 
JINI_LIB =	$(JINI_DIR)/jar

# Do not include a trailng $(CLASSPATHSEPARATOR) here, or
# we may run into problems compiling under Solaris8 with JavaScope
# because we end up with a classpath with an empty element ::
# Include jini-ext.jar for javadoc.
JINI_JARS =     $(JINI_LIB)/jini-core.jar$(CLASSPATHSEPARATOR)$(JINI_LIB)/jini-jspaces.jar$(CLASSPATHSEPARATOR)$(JINI_LIB)/jini-ext.jar

# Jar files for Java Media Framework (JMF) used by ptolemy/actor/lib/makefile
JMF_JARS =	

# Jar file that contains Joystick interface,
# see http://sourceforge.net/projects/javajoystick/
JOYSTICK_JAR =	${PTII}/vendors/misc/joystick/Joystick.jar

# Jython home directory that contains jython.jar, see http://www.jython.org
JYTHON_DIR =    ${PTII}/lib

# JXTA home directory that contains jxta.jar, see http://www.jxta.org
JXTA_DIR =	${PTII}/vendors/sun/jxta

# Jar files used by JXTA
JXTA_JARS = $(JXTA_DIR)/jxta.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/log4j.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/beepcore.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/jxtasecurity.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/cryptix-asn1.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/cryptix32.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/jxtaptls.jar$(CLASSPATHSEPARATOR)$(JXTA_DIR)/minimalBC.jar

# The major type of OS we are running under.
# Under all forms Windows, this should be Windows; ynder Linux: Linux, etc.
# Used in ptolemy/matlab/makefile
MAJOR_OS_NAME =	Linux

# Location of the Matlab directory.  The matlab binary will be
# found at $(MATLAB_DIR)/bin/matlab
MATLAB_DIR = 	/usr/local/matlab2006b

# Location of Matlab's engine libraries (libeng.so, libmx.so)
MATLAB_LIBDIR =	/usr/local/matlab2006b/bin/glnx86

# Set to caltrop and used in $PTII/ptolemy/makefile if Cal was found.
PTCALTROP_DIR = 

# Set to gcc if gcc was found and used in $PTII/ptolemy/matlab/makefile.
PTCC =	/usr/bin/gcc

# Set to chic and used in $PTII/ptolemy/makefile if chic was found.
PTCHIC_DIR = 

# PTCM_DIR is set to cm and used in $PTII/ptolemy/copernicus/c/test/makefile
# if the CaffeineMark Java Benchmark kit is found.
PTCM_DIR =	

# Set to colt and used in $PTII/ptolemy/actor/lib/makefile if colt was found.
PTCOLT_DIR =    

# Colt jar files.  We ship $PTII/lib/ptcolt.jar, which is a subset
# of $PTII/vendors/misc/colt.jar
PTCOLT_JARS =	    

# Ptolemy II packages that use COLT
# PTCOLT_PACKAGES is used in ptII/doc/makefile
PTCOLT_PACKAGES =   

# Set to comm and used in
# $PTII/ptolemy/actor/lib/makefile if the Java Communications API was found
#
PTCOMM_DIR = 

# Ptolemy II packages that use the Java Serial Communication facility
# PTCOMM_PACKAGES is used in ptII/doc/makefile
PTCOMM_PACKAGES =	  	

# Set to copernicus and used in $PTII/ptolemy/makefile if Soot was found
PTCOPERNICUS_DIR = 

# Set to distributed and used in
# $PTII/ptolemy/makefile if jini was found
PTDISTRIBUTED_DIR = 

# Set to jini jar file and used
# $PTII/ptolemy/distributed/*/makefile if jini was found
PTDISTRIBUTED_JARS = 

# Ptolemy II packages that use the distributed
# PTDISTRIBUTED_PACKAGES is used in ptII/doc/makefile
PTDISTRIBUTED_PACKAGES = 

# PTDOCLETS_DIR is set to doclets and used in 
# $PTII/doc if tools.jar can be found.
PTDOCLETS_DIR =		 	

# Location of the local $PTII directory as a file:/// URL
# This variable is used with the Java Network Launching Protocol files
PTII_LOCALURL = file:////user/fviale/home/eclipse_workspace/ProActive_Latest/dev/matlab/src

# Set to gr and used in
# $PTII/ptolemy/domains/makefile if Java 3D was found
PTJAVA3D_DIR = 

# Set to kvm and used in $PTII/ptolemy/makefile if the PalmOS KVM was found
PTKVM_DIR =	

# JavaCC is the Java Compiler Compiler which is used by ptolemy.data.expr

# The default location is $(PTII)/vendors/sun/JavaCC
JAVACC_DIR =	/user/fviale/home/eclipse_workspace/ProActive_Latest/dev/matlab/src/vendors/sun/javacc-4.0
# Under Unix:
# JJTREE =	$(JAVACC_DIR)/bin/jjtree
# JAVACC =	$(JAVACC_DIR)/bin/javacc
# Under Cygwin32 NT the following should be used and JavaCC.zip must be in
# the CLASSPATH
# JJTREE = 	$(JAVA) COM.sun.labs.jjtree.Main
# JAVACC = 	$(JAVA) COM.sun.labs.javacc.Main

JJTREE =	"touch"
JAVACC =	"touch"

# Set to $PTII/lib if $PTII/lib/mapss.jar was found and used
# in $PTII/bin/ptinvoke.in
PSDF_DIR =	${PTII}/lib

# Set to -I$PTII/mescal/include or -I/usr/local/include if gmp.h is found
# and used in $PTII/mescal/relsat
# FIXME: There could be problems if PTII have spaces
PTGMP_INCLUDE = 	

# Set to -Wl,-R$PTII/mescal/lib or -Wl,-R/usr/local/lib if the gmp
# libraries are found and used in $PTII/mescal/relsat
PTGMP_LD_FLAGS =        

# Set to -I$PTII/mescal/lib or -I/usr/local/lib if the gmp libraries are found
# and used in $PTII/mescal/relsat
PTGMP_LIB =	

# Name of the jar file that includes the GR domain if Java 3D was found.
# Used in domains/makefile.
PTGRDOMAIN_JAR =	

# Ptolemy II packages that use the Java 3D facility
# PTGR_PACKAGES is used in ptII/doc/makefile
PTGR_PACKAGES =		

# Set to ptjacl and used in
# $PTII/ptolemy/actor/gui/makefile if ptjacl.jar was found 
PTJACL_DIR =	

# Jar file that contains Jacl
PTJACL_JAR =	/user/fviale/home/eclipse_workspace/ProActive_Latest/dev/matlab/src/lib/ptjacl.jar

# Jar files used in configs/test/makefile and vergil/test/makefile
PTJACL_JARS =   $(DIVA_JAR)$(CLASSPATHSEPARATOR)$(JYTHON_DIR)/jython.jar$(CLASSPATHSEPARATOR)$(JXTA_JARS)$(CLASSPATHSEPARATOR)$(SOOT_CLASSES)$(CLASSPATHSEPARATOR)$(PTCOLT_JARS)$(CLASSPATHSEPARATOR)

# jtclsh script to run Jacl for the test suite.
# We could use bin/ptjacl here, but instead we start it from within
# make and avoid problems
# configure sets "$(JAVA)" $(JAVAFLAGS) "-Dptolemy.ptII.dir=$(PTII)" $(JTCLSHFLAGS) tcl.lang.Shell to include JTCLSHFLAGS
# JTCLSHFLAGS gets set to -Dptolemy.ptII.isRunningNightlyBuild=true
# when we are running the nightly build.
JTCLSH =	CLASSPATH="$(CLASSPATH)$(AUXCLASSPATH)$(CLASSPATHSEPARATOR)$(PTJACL_JAR)" "$(JAVA)" $(JAVAFLAGS) "-Dptolemy.ptII.dir=$(PTII)" $(JTCLSHFLAGS) tcl.lang.Shell

# Saxon is the XSLT and XQuery Processor (http://saxon.sourceforge.net/)
# At runtime, ptolemy.util.XSLTUtilities, caltrop and hsif uses Saxon.
SAXON_JAR =	$(ROOT)/lib/saxon8.jar$(CLASSPATHSEPARATOR)$(ROOT)/lib/saxon8-dom.jar

# Set to jai and used in 
# $PTII/ptolemy/actor/lib/makefile if Java Advanced Imaging was found
PTJAI_DIR =	

# Ptolemy II packages that use Java Advanced Imaging
# PTJAI_PACKAGES is used in ptII/doc/makefile
PTJAI_PACKAGES =	

# PTJHDL_DIR is set to jhdl if the JHDL directory is found
PTJHDL_DIR =    

# PTJMF_DIR is set to jmf if the JMF directory is found
PTJMF_DIR =     

# Ptolemy II packages that use Java Media Framwork
# PTJMF_PACKAGES is used in ptII/doc/makefile
PTJMF_PACKAGES =	

# JNI architecture, used to compile C files
PTJNI_ARCHITECTURE =	linux

# Set to jni and used in 
# $PTII/makefile if gcc or cc was found
PTJNI_DIR =	jni

# Set to -ldl for use by jni/launcher/makefile under Linux
PTJNI_DL_LIBRARY = -ldl

# JNI lib architecture, used to run jni/launcher
PTJNI_LIB_ARCHITECTURE = i386

# Flag to use with TinyOS under Cygwin (-mno-cygwin)
#PTJNI_NO_CYGWIN =	
# Under Windows with Cygwin-1.3.22 and gcc-3.2, we do not use -mno-cygwin
PTJNI_NO_CYGWIN =


# JNI shared library C compiler flag, under Solaris this would be -fPIC.
PTJNI_SHAREDLIBRARY_CFLAG =	

# JNI shared library linker flag, under Solaris this would be -fPIC.
PTJNI_SHAREDLIBRARY_LDFLAG =	

# JNI shared library prefix, under Solaris this would be lib.
PTJNI_SHAREDLIBRARY_PREFIX =	lib

# JNI shared library suffix, under Windows this would be dll.
PTJNI_SHAREDLIBRARY_SUFFIX =	so

# JNI libraries needed to link, such as -lcygwin (Used by Viptos) 
PTJNI_LIBRARIES = 

# Set to joystick if Joystick.jar was found.
PTJOYSTICK_DIR =     

# Ptolemy II packages that use the Java Joystick facilty
# PTJOYSTICK_PACKAGES is used in ptII/doc/makefile
PTJOYSTICK_PACKAGES =	

# PTJYTHON_DIR is set to python (FIXME: should be jython) and used in
# $PTII/ptolemy/actor/lib/makefile if Jython was found.
PTJYTHON_DIR=	

# Set to jxta and used in
# $PTII/actor/lib/makefile if jxta.jar was found
PTJXTA_DIR =	

# Set to lego and used in $PTII/ptolemy/apps/makefile if
# the Java Communications API was found
PTLEGO_DIR =	

# Set to lejos and used in $PTII/ptolemy/apps/makefile if
# the Java Communications API and Lejos was found
PTLEJOS_DIR =	

# Set to gcc or cl and used in
# $PTII/ptolemy/matlab/makefile if the Matlab was found.
PTMATLAB_CC =	/usr/bin/gcc

# Set to matlab and used in $PTII/ptolemy/makefile if
# matlab was found.
PTMATLAB_DIR =	matlab

# Linker args for Matlab
# $PTII/ptolemy/matlab/makefile
PTMATLAB_LD_ARGS = ptmatlab.cc -fno-exceptions -c -o libptmatlab.so	-L"/usr/local/matlab2006b/bin/glnx86"  -lc -leng -lmx

# Set to mescal and used in $PTII/makefile 
# if $PTII/mescal was found
PTMESCAL_DIR=	

# Set to psdf and used in $PTII/ptolemy/domains/makefile if
# $PTII/lib/mapss.jar was found
PTPSDF_DIR =	

# PTSVG_DIR is set to svg and used in
# $PTII/diva/util/java2d/makefile if Batik SVG jar files were found
PTSVG_DIR =	

# Jar file used by diva/util/java2d/svg/makefile
PTSVG_JAR =	

# PTPTALON_DIR is set to ptalon and used in
# $PTII/ptolemy/actor/makefile if antlr.jar was found.
PTPTALON_DIR =  

# PTALON_PACKAGES is used in $PTII/doc/makefile
PTPTALON_PACKAGES =  

# Directory that contains the antlr.jar file, used by ptalon
ANTLR_DIR =	${PTII}/ptolemy/actor/ptalon/antlr

# Set to quicktime and used in $PTII/ptolemy/domains/gr/lib/makefile if
# QuickTime for Java was found.
PTQUICKTIME_DIR =	

# Ptolemy II packages that use the Quicktime facility
# PTQUICKTIME_PACKAGES is used in ptII/doc/makefile
PTQUICKTIME_PACKAGES = 	

# Set to tini and used in $PTII/ptolemy/apps/makefile if the Dallas Semi
# TINI was found
PTTINI_DIR =	@PTINI_DIR@

# PTTINYOS_DIR is set to ptinyos and used in
# $PTII/ptolemy/domains/makefile if TinyOS is found
PTTINYOS_DIR =	

# Set to waba and used in $PTII/ptolemy/makefile if the PalmOS WABA was found
PTWABA_DIR =	

# Set to x10 and used in
# $PTII/ptolemy/apps/superb/makefile if the X10 jar files were found
PTX10_DIR =	
X10_JAR =       

# Ptolemy II packages that use the X10 facilty.
# PTX10_PACKAGES is used in ptII/doc/makefile
PTX10_PACKAGES = 	

# Location of the diva.jar file.  Diva is (among other things) a graph
# visualization tool used by some of the demos.  For more information, see
# http://www-cad.eecs.berkeley.edu/diva/
DIVA_JAR = $(PTII)/lib/diva.jar

# JSAT jar location
MESCAL_DEPEND_JAR = $(PTII)/mescal/lib/jsat.jar$(CLASSPATHSEPARATOR)$(PTII)/mescal/lib/mescal_java_cup.jar$(CLASSPATHSEPARATOR)$(SOOT_CLASSES)$(CLASSPATHSEPARATOR)$(PTII)/mescal/lib/chloe.jar

# Commands used to install scripts and data
# Use $(ROOT) instead of $(PTII) for install so that we don't
# need to have PTII set under Ptolemy classic when installing Ptplot
INSTALL =		$(ROOT)/config/install-sh -c
INSTALL_PROGRAM =	${INSTALL}
INSTALL_DATA =		${INSTALL} -m 644

