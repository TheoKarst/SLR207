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

# Generate a random secret key (symmetric key for AES) to have a secured connection between all the computers:
openssl rand 32 > $keysfile

computers=$(cat $1)

for computer in $computers; do
	host="$login@$computer.$domain"

    # Kill the previous versions of the slave servers, and create a new empty folder for our files:
	command1=("ssh" "$host" "lsof -ti | xargs kill -9; rm -rf $remoteFolder; mkdir $remoteFolder")

    # Send the jar file to the remote host, and the keys file to allow him to decipher our messages:
	command2=("scp" "$fullpath" "$host:$remoteFolder$filename")
    command3=("scp" "$keysfile" "$host:$remoteFolder$keysfile")

    # Start the server on the remote host:
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
