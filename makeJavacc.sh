#!/bin/bash

for i in $(find . | grep \.javacc)
do
	export WD=$(pwd)
	cd $(dirname $i)
	javacc $(basename $i)
	cd $WD
done
