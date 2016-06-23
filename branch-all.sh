#!/bin/bash

NEWBRANCH=develop
HERE=`pwd`

AGAVE_ROOT=`pwd`

cd $AGAVE_ROOT

for i in `ls -d $AGAVE_ROOT/agave-*`
do
	echo "Entering $i submodule directory"

  cd $i;

  CRNT=$(git status | grep $NEWBRANCH)

  if [[ -z $CRNT ]]; then

    echo "Branching $i to $NEWBRANCH"
		git branch $NEWBRANCH

		echo "Checking out $i:$NEWBRANCH"
		git checkout $NEWBRANCH

		echo "Updating submodules of $i:$NEWBRANCH"
		git submodule update --init --recursive

		echo "Pushing $i:$NEWBRANCH origin/$NEWBRANCH"
		git push -v --tags $NEWBRANCH:$NEWBRANCH

  else

    echo "$i has already been branched...skipping"

  fi

	echo -e "\n\n"
  cd $AGAVE_ROOT

done

cd $HERE


