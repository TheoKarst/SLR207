#include <iostream>
#include <fstream>
#include <sstream>
#include <sys/time.h>
#include <map>

using namespace std;

long currentTimeMillis(){
    struct timeval tp;
    gettimeofday(&tp, NULL);
    return tp.tv_sec * 1000 + tp.tv_usec / 1000;
}

int main(int argc, char * argv[]){
    if(argc != 2){
        cout << "This is a program written in C++ to count occurences of words in a file" << endl;
        cout << "usage: " << argv[0] << " [filename]" << endl;
        exit(1);
    }

    time_t startTime = currentTimeMillis();

    ifstream inFile;
    inFile.open(argv[1]);

    map<string, int> hashmap;

    string line;
    string word;

    while(getline(inFile, line)){
        stringstream lineStream(line);

        while (lineStream >> word)
            hashmap[word]++;
    }

    inFile.close();

    time_t endTime = currentTimeMillis();

    cout << "Count word duration: " << (endTime - startTime) << " ms" << endl;

    for (auto it = hashmap.begin(); it != hashmap.end(); it++)
    {
        cout << it->first << ": " << it->second << endl;
    }
}
