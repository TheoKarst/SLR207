#include <iostream>
#include <fstream>
#include <sstream>
#include <map>

using namespace std;

int main(int argc, char * argv[]){
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

    for (auto it = hashmap.begin(); it != hashmap.end(); it++)
    {
        cout << it->first << ": " << it->second << endl;
    }

    inFile.close();
}
