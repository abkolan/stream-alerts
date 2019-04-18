package net.semikolan;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class Main {

  private static int ALERT_NUM_OCCURRENCES = 5000000;
  private static int TIME_INTERVAL_MINUTES = 5;

  //Used in head trimming the sorted map
  private final static int DELTA = TIME_INTERVAL_MINUTES * 60 * 1000 + 1;

  private static LoadingCache<String, SortedMap<Long, Integer>> wordMap;

  //Create an In-Memory Cache with expiry of 10m without writes
  static {
    wordMap = CacheBuilder.newBuilder()
        .expireAfterWrite(TIME_INTERVAL_MINUTES * 10, TimeUnit.MINUTES) //expiry
        .build(new CacheLoader<String, SortedMap<Long, Integer>>() {
          @Override
          public SortedMap<Long, Integer> load(String s) {
            return new TreeMap<>();
          }
        });
  }

  private static boolean considerStopWords = false;

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    for (String arg : args) {
      System.out.println(arg);
    }
    if (args.length == 1 && args[0].equals("test")) {
      driver();
    } else {
      if (args.length == 3) {
        ALERT_NUM_OCCURRENCES = Integer.parseInt(args[0]);
        TIME_INTERVAL_MINUTES = Integer.parseInt(args[1]);
        considerStopWords = Boolean.parseBoolean(args[2]);
      }
      // Reading the stream from STDOUT, used to pipe random tweets
      // using gen-tweets.sh
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader bufReader = new BufferedReader(isReader);
      String inputStr = "";
      long epoch = 0;
      while (inputStr != null) {
        try {
          inputStr = bufReader.readLine();
          epoch = System.currentTimeMillis();
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (inputStr != null) {
          process(inputStr, epoch);
        } else {
          System.out.println("<empty>");
        }
      }
    }
  }

  //A Test driver
  private static void driver() throws ExecutionException, InterruptedException {
    System.out.println("Test mode");
    ALERT_NUM_OCCURRENCES = 5;
    TIME_INTERVAL_MINUTES = 1;
    System.out.println("Sending Mock Data");
    process("When you sense there’s something more", System.currentTimeMillis());
    Thread.sleep(1000);
    process("when the life that used to satisfy you no longer seems enough", System.currentTimeMillis());
    Thread.sleep(2000);
    process("when security seems suffocating and pleasures lose their taste", System.currentTimeMillis());
    Thread.sleep(3000);
    process("when dreams of success can’t motivate you anymore", System.currentTimeMillis());
    Thread.sleep(500);
    process("When you ache with a sadness that doesn’t seem to have a source", System.currentTimeMillis());
    process("when you feel a hunger you’ve never known before", System.currentTimeMillis());
    Thread.sleep(10000);
  }

  private static void process(String inputStr, long epoch) throws ExecutionException {
    inputStr = inputStr.toLowerCase();
    StringTokenizer tokenizer = new StringTokenizer(inputStr, " \t\r\n.?!\",");
    while (tokenizer.hasMoreElements()) {
      String word = tokenizer.nextElement().toString();
      if (considerStopWords) {
        if (!word.isEmpty() && !stopWords.contains(word) && !word.startsWith("@")) {
          eval(word, roundUp(epoch));
        }
      } else {
        if (!word.isEmpty() && !word.startsWith("@")) {
          eval(word, roundUp(epoch));
        }
      }
    }
  }

  private static void eval(String word, long epoch) throws ExecutionException {
    SortedMap<Long, Integer> windowCounter = wordMap.get(word);
    if (windowCounter.containsKey(epoch)) {
      Integer val = windowCounter.get(epoch);
      windowCounter.put(epoch, val + 1);
    } else {
      windowCounter.put(epoch, 1);
    }
    //Delete older fixed window of 1s
    windowCounter.headMap(epoch - DELTA).clear();
    //Compute the sum of the Window counter
    int sum = windowCounter.values().stream().reduce(0, Integer::sum);
    if (sum >= ALERT_NUM_OCCURRENCES) {
      System.out.println("ALERT");
      System.out.println(word);
    }
  }

  // Round up the epoch to the nearest second
  // This can be configurable depends on size of the fixed window
  private static long roundUp(long epoch) {
    epoch = 1000 * (epoch / 1000);
    return epoch;
  }

  //NLTK's list of english stopwords
  private static String[] stopWordsEnglish = {
      "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves",
      "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
      "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are",
      "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an",
      "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about",
      "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up",
      "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when",
      "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
      "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don't", "should",
      "now", "i'm", "i", "me"
  };
  private static final Set<String> stopWords = new HashSet<>(Arrays.asList(stopWordsEnglish));
}
