#!/bin/bash

login="karst-21"
domain="enst.fr"
remoteFolder="/tmp/$login/"
keysfile="keys"

# Get the fullpath to the .jar file, the name of the file and it's extension:
fullpath=$2
filename=$(basename $2)
extension=${2#*.}

if [ $# -ne 2 ] || [ $extension != "jar" ]
then
	echo "This script deploys and runs program.jar on all the computers listed in the given file"
	echo "usage: ./deploy.sh list_computers program.jar"
	exit
fi

# Generate a random secret key to have a secured connection between all the computers:
openssl rand 32 > $keysfile

computers=$(cat $1)

for computer in $computers; do
	host="$login@$computer.$domain"
	command1=("ssh" "$host" "rm -rf $remoteFolder; mkdir $remoteFolder")
	command2=("scp" "$fullpath" "$host:$remoteFolder$filename")
    command3=("scp" "$keysfile" "$host:$remoteFolder$keysfile")
    command4=("ssh" "$host" "java -jar $remoteFolder$filename")
    
	
	echo ${command1[*]}
	${command1[@]}
	echo ${command2[*]}
	${command2[@]}
    echo ${command3[*]}
	${command3[@]}
    echo ${command4[*]}
	${command4[@]} &

	echo ""
done
