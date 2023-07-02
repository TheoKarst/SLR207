
all: WordCounter WordCounter.class

%: %.cpp
	g++ -O3 $< -o $@

%.class: %.java
	javac $<

clean:
	rm -rf *.class WordCounter