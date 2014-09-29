#!/bin/sh

# $Id: helloWorld.sh,v 1.3 2005/11/18 15:21:02 rschmidt Exp $

PWD=`pwd`

if [ -n "$PGRIDHOME" ]
then
	cd $PGRIDHOME
fi

java -Dfile.encoding=UTF-8 -classpath .:p-grid.jar test.demo.HelloWorld $*

cd $PWD

exit 0