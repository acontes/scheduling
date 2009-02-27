#!/bin/sh

CLASSPATH=.
workingDir=`dirname $0`
. $workingDir/env.sh

opt="-Xms128m -Xmx2048m"

$JAVACMD $opt org.ow2.proactive.resourcemanager.utils.RMStarter $@

echo
