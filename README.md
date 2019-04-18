# stream-alerts

### Problem
Set off an alert, when a word occurs for more than X number of times in a sliding window of M minutes in a stream of text (like Tweets)

### Assumptions
* Time at which the tweet is generated and the time at which the tweet is processed is the same. The difference between them is zero (negligible). 
* This solution is running on a system that has has infinite resources (Not a distributed Systems problem)
* No usage of any third party softwares whether open source or properitary. (For example: Map-Reduce, Spark, Storm or Kafka QL etc)


### Approach


![](https://github.com/abkolan/stream-alerts/blob/master/media/image.png?raw=true)

1. Each Tweet recieved at a stream. The stream reads from `STDIN`
2. Each Tweet is then tokenized to separate out the words, using `java.util.StringTokenizer`. 
3. If StopWord scanning is enabled, Stop words from the English language are ignored and are not taken into account. Stop words are from [NLTK's list of english stopwords](https://gist.github.com/sebleier/554280)
4. Each token is emited with the TimeStamp in epoch, rounded of to the nearest second.  
For example:  
1555588622438 - Thursday, April 18, 2019 5:27:02.438 PM is rounded off to 1555588622000 - Thursday, April 18, 2019 5:27:02 PM
4. An in-memory KV map is maintained with the word as the key and a SortedMap as the value. The SortedMap has KV map with rounded off epoch as the key and a counter of the occurrences of the word seen at that second. 
  * When a counter against a word is added the SortedMap is trimmed from the start to delete older entries that are not in the sliding window. This would ensure that the SortedMap would store at a maximum of 60 * M entries. 
  * The remaining values in the SortedMap are added, if the breach the count an Alert is raised. In our case we print the word. 
  * The In-Memory KV map used is `LoadingCache` from the [Guava collections](https://github.com/google/guava) with an expiry. This would ensure that the we would not have older values that wouldn't be needed after sufficiently large elapsed interval. 10 x M in our case.

### Building and Running
**Prerequisites**
 
* JAVA on UNIX/Linux or Mac or Linux Substem for Windows<sup>*</sup>.
* bash + GNU Core Utilities for simulation.  


<sup>*</sup>not tested on Linux Substem on Windows
#### Running the project

1. **stream-alerts** is a java maven project, the project can be built into an executable jar by using.  
`mvn package`

2. This repo comes with a sample tweets (From Kaggle, the dataset has been modified only to include tweet text as is).  
The sample tweets can be extracted by running:   
`tar -xf sample-tweets.tar.gz`

3. This repo also comes with a script `gen-tweets.sh` that would randomly generate 1000000 tweets per second from the `sample-tweets` file. Tweets per minute can be modifed in the `gen-tweets.sh`

4. Run the simulation
`./gen-tweets.sh | 
java -jar target/stream-alert-0.0.1-jar-with-dependencies.jar <NUM-OCCURRENCES> <TIME-INTERVAL-IN-MIN> <IGNORE-STOP-WORDS>`


  Example:
  `./gen-tweets.sh | java -jar target/stream-alert-0.0.1-jar-with-dependencies.jar 10000 1 true`
  
  Excerpt of the output
  ```
   ALERT
   back
   ALERT
   love
   ALERT
   good
   ALERT
   time
   ALERT
  going
  ```

  The above example would print out alerts when a non-stopword word occurrences is equal to or greater than 10000 for a sliding period of 1 minute, from the simulated tweets. The counts are accurate upto a second.

