#!/bin/bash

# Change this to your netid
export netid=ash170000

# Root directory of your project
export PROJDIR=$HOME/CS-6378/cs6378proj2

# Directory where the config file is located on the launching system
export CONFIGLOCAL=$PROJDIR/config.txt
# Directory where the config file is located on the remote system
export CONFIGREMOTE=$PROJDIR/config.txt

# Directory your compiled java classes are in
export BINDIR=$PROJDIR

# Your main project class
export PROG=App

# javac App.java ChannelThread.java Mutex.java AcceptThread.java

#scp -r ./out/production/cs6378proj2/ $netid@csgrads1.utdallas.edu:~/cs6378/proj2/

n=0
cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo "$i"
    while [[ $n -lt $(echo $i | sed "s/\s.*//") ]]
    do
    	read line
    	node=$( echo "$line "| awk '{ print $1 }' )
    	host=$( echo "$line "| awk '{ print $2 }' )
    	port=$( echo "$line "| awk '{ print $3 }' )

        xterm -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $CONFIGREMOTE $node $port; exec bash" &

        n=$(( n + 1 ))
    done
)
