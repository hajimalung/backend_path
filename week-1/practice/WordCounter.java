import java.util.Map;
import java.util.HashMap;

public class WordCounter {
    public static void main(String[] args) {
        String text = "Hello world! This is a sample text. Hello again!";
        Map<String, Integer> wordFrequency = wordCount(text);

        System.out.println("Total number of words: " + wordFrequency.size());
        System.out.println("Word frequencies:");
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static Map<String, Integer> wordCount(String text) {
        // Split the text into words using regex to handle punctuation and whitespace
        String[] words = text.split("\\W+");
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            word = word.toLowerCase();
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
        }
        return wordFrequency;
    }
}
