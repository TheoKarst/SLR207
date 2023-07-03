This folder contains several files to run MapReduce algorithm:

# Return a list of available machines, running tp_up on ssh.enst.fr. We can add as arguments the index of the first machine (to avoid always running MapReduce on the same machines), and the number of machines we want:
./list_computers.sh [from_computer] [count_computers]

# Cleans the computers listed in the given file, by closing ssh connections on this computer, closing servers that are running on the remote computers, and removing the tempoary folder /tmp/karst-21 on the remote computers:
./clean_computers.sh [list_computers]

# Deploys the given program on the given computers, in order for the master to be able to run MapReduce algorithm:
./deploy.sh [list_computers] [program.jar]

# Given an input file and a number of computers on the network, deploys and runs slave.jar on [n_computers] computers, and execute the master in order to run the MapReduce algorithm:
./run.sh [input_file] [n_computers]

Example of use: (run the MapReduce algorithm on 10 different computers, with domaine_public_fluvial.txt as input file):
./run.sh test_files/domaine_public_fluvial.txt 10

# The Makefile also provides some useful commands:

make test       # Run the MapReduce algorithm with the split files given in the folder test_splits
make clean      # Run ./clean_computers.sh, and remove unnecessary files on this computer

This folder also contains different folders:
- test_files: To test the MapReduce algorithm on different files
- test_splits:  Default split files also used for testing
- word_counters: Contains two algorithm (in Java and C++) to count the occurences of words sequentially, with a single computer
