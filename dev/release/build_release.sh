#! /bin/sh

TMP=/tmp

# /mnt/scratch is a tmpfs mount point for faster builds on schubby
if [ -w "/mnt/scratch" ] ; then
	TMP=/mnt/scratch
fi

SCHEDULER_DIR=$1
VERSION=$2
JAVA_HOME=$3
if [ ! -z "$4" ] ; then
	TMP=$4
fi

RELEASE_BASENAME="ProActiveScheduling"

TMP_DIR=""

echo " [i] SCHEDULER_DIR: $SCHEDULER_DIR"
echo " [i] VERSION:       $VERSION"
echo " [i] JAVA_HOME:     $JAVA_HOME"
echo " [i] TMP:           $TMP"
function warn_and_exit {
	echo "$1" 1>&2
	exit 1
}

function warn_print_usage_and_exit {
	echo "$1" 1>&2
	echo "" 1>&2
	echo "Usage: $0 SCHEDULER_DIR VERSION JAVA_HOME" 1>&2
	exit 1
}


if [ -z "$SCHEDULER_DIR" ] ; then
	warn_print_usage_and_exit "SCHEDULER_DIR is not defined"
fi

if [ -z "$VERSION" ] ; then
	warn_print_usage_and_exit "VERSION is not defined"
fi

if [ -z "$JAVA_HOME" ] ; then
	warn_print_usage_and_exit "JAVA_HOME is not defined"
fi
export JAVA_HOME=${JAVA_HOME}


TMP_DIR="${TMP}/RELEASE_BASENAME-${VERSION}"
output=$(mkdir ${TMP_DIR} 2>&1)
if [ "$?" -ne 0 ] ; then
	if [ -e ${TMP_DIR} ] ; then
		echo " [w] ${TMP_DIR} already exists. Delete it !"
		rm -Rf ${TMP_DIR}
		mkdir ${TMP_DIR}
		if [ "$?" -ne 0 ] ; then
			warn_and_exit "Cannot create ${TMP_DIR}: $output"
		fi
	else
		warn_and_exit "Cannot create ${TMP_DIR}"
	fi
fi

cp -Rf ${SCHEDULER_DIR} ${TMP_DIR}

cd ${TMP_DIR} || warn_and_exit "Cannot move in ${TMP_DIR}"
if [ "$(find src/ -name "*.java" | xargs grep serialVersionUID | grep -v `echo $VERSION | sed 's@\(.\)\.\(.\)\..@\1\2@'` | wc -l)" -gt 0 ] ; then
	if [ -z "${RELAX}" ] ; then
		warn_and_exit " [E] serialVersionUID are NOT defined"
	fi
fi


cd compile || warn_and_exit "Cannot move in compile"
./build clean
./build -Dversion="${VERSION}" deploy.all
./build -Dversion="${VERSION}" manualPdf

cd ${TMP_DIR} || warn_and_exit "Cannot move in ${TMP_DIR}"
echo " [i] Clean"

# Subversion
find . -type d -a -name ".svn" -exec rm -Rf {} \;

# Git
rm -Rf .git

# Remove useless parts of ProActive
rm ./doc-src/ProActiveRefBook.doc
rm -rf ./lib/common/client.jar # usefull only for nordugrid...
find . -type f -a -name "*.svg" -exec rm {} \; # svg are converted in png by hands

# Remove non GPL stuff
rm -Rf ./compile/lib/clover.*

# Remove temporary files
rm compile/junit*properties
rm -Rf classes/
rm -Rf docs/tmp/
rm -Rf doc-src/*_snippets/

sed -i "s/{version}/$VERSION/" README.txt

cd ${TMP}
tar cvfz $RELEASE_BASENAME-${VERSION}.tar.gz $RELEASE_BASENAME-${VERSION}
zip -r   $RELEASE_BASENAME-${VERSION}.zip    $RELEASE_BASENAME-${VERSION}
