#!/bin/bash

login="karst-21"
domain="enst.fr"

if [ $# -ne 2 ]
then
	echo "This script lists the name of [count_computers] that are up"
	echo "usage: ./list_computers.sh [from_computer] [count_computers]"
	exit
fi

# List which computers are up:
fromIndex=$1
count=$2
ssh karst-21@ssh.enst.fr tp_up | head -n $((fromIndex+count)) | tail -n $count
