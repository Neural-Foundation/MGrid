#!/bin/sh

# $Id: p-grid.sh,v 1.3 2005/11/18 15:21:02 rschmidt Exp $

PWD=`pwd`

if [ -n "$PGRIDHOME" ]
then
	cd $PGRIDHOME
fi

java -Dfile.encoding=UTF-8 -jar p-grid.jar $*

cd $PWD

exit 0