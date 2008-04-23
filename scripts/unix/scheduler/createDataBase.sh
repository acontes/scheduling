#!/bin/sh

echo
echo --- Create DataBase----------------------------------------------


  CONFIG_FILE=$1

workingDir=..
PROACTIVE=$workingDir/../..
CLASSPATH=.
. $workingDir/env.sh

CLASSPATH=$workingDir/../../scheduler-plugins-src/org.objectweb.proactive.scheduler.plugin/bin/:$CLASSPATH

if [ -e "$1" ]; then
	$JAVACMD org.objectweb.proactive.extensions.scheduler.util.CreateDataBase $CONFIG_FILE
else
	echo "You must give a configuration file to create database ! Use scheduler_db.cfg as exemple."
fi
echo
