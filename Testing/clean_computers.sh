#!/bin/bash

login="karst-21"
domain="enst.fr"
remoteFolder="/tmp/$login/"

if [ $# -ne 1 ]
then
	echo "This script cleans the remote computers, and close the servers"
	echo "usage: ./clean.sh [list_computers]"
	exit
fi

# Close the ssh connections on the local computer:
list_pid=$(ps w | grep "[j]ava -jar /tmp/karst-21" | grep -E -o "^ *[0-9]+")
for pid in $list_pid; do kill -9 $pid; done

# Clean the remote computers:
computers=$(cat $1)

for computer in $computers; do
	host="$login@$computer.$domain"

    # Kill the previous versions of the slave servers, and delete the tempoary folder containing our files:
	command1=("ssh" "$host" "lsof -ti | xargs kill -9 2>/dev/null; rm -rf $remoteFolder")

    echo ${command1[*]}
	${command1[@]}
done
