import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Comparable;

public class WordCount {

    public static class Word implements Comparable<Word>{
        public final String word;
        public final int occurences;

        public Word(String word, int occurences){
            this.word = word;
            this.occurences = occurences;
        }

        @Override
        public int compareTo(Word other) {
            if(occurences == other.occurences)
                return word.compareTo(other.word);

            return other.occurences - occurences;            
        }

        @Override
        public String toString(){
            return word + ": " + occurences;
        }
    }

    public static void main(String args[]){
        long startTime = System.currentTimeMillis();
        HashMap<String, Integer> wordsCount = countWords(args[0]);
        long endTime = System.currentTimeMillis();
        System.out.println("Count word duration: " + (endTime-startTime));

        startTime = System.currentTimeMillis();
        ArrayList<Word> sorted = sortWords(wordsCount);
        endTime = System.currentTimeMillis();
        System.out.println("Sort word duration: " + (endTime-startTime));
        
        for(Word word : sorted)
            System.out.println(word);
    }

    public static HashMap<String, Integer> countWords(String filename){
        HashMap<String, Integer> wordsCount = new HashMap<String, Integer>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                for(String word : line.split(" ")){
                    if(word.length() == 0)          // Ignore empty words
                        continue;

                    Integer count = wordsCount.get(word);
                    if(count == null)
                        wordsCount.put(word, 1);
                    else
                        wordsCount.put(word, count + 1);
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return wordsCount;
    }

    public static ArrayList<Word> sortWords(HashMap<String, Integer> wordsCount){
        ArrayList<Word> sorted = new ArrayList<Word>();

        for(String word : wordsCount.keySet())
            sorted.add(new Word(word, wordsCount.get(word)));

        Collections.sort(sorted);

        return sorted;
    }
}