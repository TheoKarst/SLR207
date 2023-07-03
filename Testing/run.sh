#!/bin/bash

START_COMPUTER_INDEX=80        # Index of the first computer we take from the list given by the command tp_up
COMPUTERS_FILE=computers
KEYS_FILE=keys
REDUCES_FOLDER=reduces
SPLITS_FOLDER=splits
SLAVE=slave.jar
MASTER=master.jar

if [ $# -ne 2 ]
then
	echo "This script deploys and runs the MapReduce algorithm with the given number of computers, on the given input file:"
	echo "usage: ./run.sh [input_file] [n_computers]"
	exit
fi

echo "rm -rf $REDUCES_FOLDER $SPLITS_FOLDER"
rm -rf $REDUCES_FOLDER $SPLITS_FOLDER

echo -e "\nListing $2 computers that are up, and save them in the file $COMPUTERS_FILE"
./list_computers.sh $START_COMPUTER_INDEX $2 > $COMPUTERS_FILE
cat $COMPUTERS_FILE

echo -e "\nDeploying slave.jar on the remote computers:"
./deploy.sh $COMPUTERS_FILE $SLAVE

# Wait for the slaves to be successfully deployed:
sleep 1

echo -e "\nRunning the master with the input: $1"
java -jar $MASTER $KEYS_FILE $COMPUTERS_FILE $1

echo -e "\nResults of the MapReduce algorithm:"
cat reduces/*

