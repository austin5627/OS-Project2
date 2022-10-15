#!/bin/bash

# Change this to your netid
export netid=$(whoami)

# Root directory of your project
if [[ $netid -eq "ash170000" ]]; then
  export PROJDIR=$HOME/CS-6378/cs6378proj2/
else
  export PROJDIR=$HOME/cs6378/proj2/
fi

# Directory where the config file is located on your local system
export CONFIGLOCAL=./config.txt

# Directory your java classes are in
export BINDIR=$PROJDIR

# Your main project class
export PROG=Node

# Directory to place log files
# ends with a slash
export OUTPUTDIR=$PROJDIR/

n=0
cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read -r i
    echo "$i"
    while [[ $n -lt $i ]]
    do
    	read -r line
    	node=$( echo "$line "| awk '{ print $1 }' )
    	host=$( echo "$line "| awk '{ print $2 }' )
    	port=$( echo "$line "| awk '{ print $3 }' )

        xterm -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $CONFIGLOCAL $node $port; exec bash" &

        n=$(( n + 1 ))
    done
)

#javac Node.java Launcher.java
#java Launcher