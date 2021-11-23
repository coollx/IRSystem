import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class IRSystem {
    private int N = 0; //total number of documents in system
    private String docFiles; // document path
    private String stopWordFile; //stop words path
    private static Tokenizer tokenizer; //singleton tokenizer, we only a single instance of tokenizer in memory
    private Set<String> vocabulary; //set of vocabulary 
    private Map<String, Document> documentMap; //associated each document is stored with it's ID
    private Map<String, Integer> documentFrequency;  //map of document frequency of each term
    private Map<String, List<Pair<Document, Integer>>> invertedIndex; //as you can see
    private double averageDocLength;

    public IRSystem(String documents,String stopWord) {
        this.docFiles = documents;
        this.stopWordFile = stopWord;
        tokenizer = new Tokenizer(stopWordFile);
        vocabulary = new HashSet<>();
        documentMap = new HashMap<>();
        documentFrequency = new HashMap<>();
        invertedIndex = new HashMap<>();
        averageDocLength = 0;

        analyzeDocuments();

    }

    private void analyzeDocuments() {
        try(BufferedReader br = new BufferedReader(new FileReader(docFiles))) {
            for(String line; (line = br.readLine()) != null; ) {
                N++;
                String[] content = line.split("\\t");
                String docID = content[0];
                String text = content[1];

                //create document object
                Document doc = new Document(docID, text);
                //extend vocabulary with current document
                vocabulary.addAll(doc.getTokenList());
                //extend inverted index with current document
                buildInvertedIndex(doc);
                //add document into doc map
                documentMap.put(docID, doc);
                //add document length to average
                averageDocLength += doc.getTokenList().size();
            }
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		}  catch (IOException e) {
			e.printStackTrace();		
		}

        //The norm of each document is calulated once we built the inverted index
        for (Document doc : documentMap.values()) {
            doc.calcNorme(documentFrequency, N);
        }

        averageDocLength /= documentMap.size();
        System.out.println("Indexing done, vovabulary size = " + vocabulary.size());
    }


    //take one document and extend inverted index 
    private void buildInvertedIndex(Document doc) {
        Map<String, Integer> freqMap = doc.getFreqMap();
        for (String token : freqMap.keySet()) {
            //for every element in frequency map, increment it's document frequency by 1
            documentFrequency.put(token, documentFrequency.getOrDefault(token, 0) + 1);

            //update inverted index
            //insert a new array list to our hashmap if we don't have an associate list with that token.
            if (!invertedIndex.containsKey(token)) {
                invertedIndex.put(token, new ArrayList<>());
            }
            //update the number of occurence of each term in inverted index
            invertedIndex.get(token).add(new Pair<Document, Integer>(doc, freqMap.get(token)));
        }
    }

    //helper function
    private Map<String, Integer> getFreqMap(List<String> doc) {
        //create a map to store each word in the document with it's frequency
        Map<String, Integer> ret = new HashMap<>();

        for (String token : doc) {
            ret.put(token, ret.getOrDefault(token, 0) + 1);
        }
        
        return ret;
    }


    //retrive top K result of query q, the ranking was computed using cosine similarity function
    private List<Pair<Document, Double>> retriveTopK(String q, int K) {
        //store each document and it's word vector
        Map<Document, Double> similarityMap = new HashMap<>();

        //remove stop words, tokenization using porter stemmer
        List<String> query = tokenizer.getTokens(q);

        //get frequency map of the query
        Map<String, Integer> freqMap = getFreqMap(query);

        //get max frequency from query
        int maxFreq = 0;
        for (String term : query) {
            maxFreq = Math.max(maxFreq, freqMap.get(term));
        }

        //a variable to calculated norm of the query is initialized to 0
        double queryNorm = 0;

        for (String term : query) {
            //skip query terms that we never seen
            if (! invertedIndex.containsKey(term)) {
                continue;
            }

            //compute idf of term by document frequency
            int df_t = documentFrequency.get(term);
            double idf = Math.log10((N + 0.0) / (df_t + 0.0));

            //compute w_t_q: weight of term in query
            double w_t_q = tf_i_q(term, freqMap, maxFreq) * idf;
            //update query norm
            queryNorm += w_t_q * w_t_q;

            for (Pair<Document, Integer> pair : invertedIndex.get(term)) {
                Document doc = pair.getKey();
                int tf = pair.getValue();
                //calculate tf_idf of term t to document d
                double w_t_d = tf_idf(tf, idf);

                //update the consine score, update similarityMap
                similarityMap.put(doc, similarityMap.getOrDefault(doc, 0.0) + w_t_d * w_t_q);
            }
        }

        //compute query norm
        queryNorm = Math.sqrt(queryNorm);

        //use a priority queue to store all documents
        PriorityQueue<Pair<Document, Double>> pq = new PriorityQueue<>();
        //insert every element in the hashtable to priority queue to get top k elements
        for (Document doc : similarityMap.keySet()) {
            pq.add(new Pair<Document, Double>(doc, similarityMap.get(doc) / doc.getNorme() / queryNorm));
        }

        //initilize a list to store final result
        List<Pair<Document, Double>> ret = new ArrayList<>();

        //take the top K element from heap
        int size = pq.size();
        for (int i = 0; i < Math.min(K, size); i++) {
            ret.add(pq.poll());
        }

        return ret;
    }


    //retrive top K result of query q, the ranking was computed using bm25 algorithm
    private List<Pair<Document, Double>> retriveTopKbm25(String q, int K, double k, double b) {
        //store each document and it's word vector
        Map<Document, Double> similarityMap = new HashMap<>();

        //remove stop words, tokenization using porter stemmer
        List<String> query = tokenizer.getTokens(q);

        //get frequency map of the query
        Map<String, Integer> freqMap = getFreqMap(query);
        //get max frequency from query
        int maxFreq = 0;
        for (String term : query) {
            maxFreq = Math.max(maxFreq, freqMap.get(term));
        }

        for (String term : query) {
            if (! invertedIndex.containsKey(term)) {
                continue;
            }

            //compute idf of term by document frequency
            int df_t = documentFrequency.getOrDefault(term, 0);

            //compute w_t_q: weight of term in query by bm25 algorithm
            double w_t_q = Math.log(1 + (0.5 + N - df_t) / (0.5 + df_t) );

            for (Pair<Document, Integer> pair : invertedIndex.get(term)) {
                Document doc = pair.getKey();
                int tf = pair.getValue();
                //calculate tf_idf of term t to document d
                int docLength = doc.getTokenList().size();
                double w_t_d = ((1.0 + k) * tf )/(0.0 + tf + k*(1 - b + b * docLength / averageDocLength));

                //update the consine score, update similarityMap
                similarityMap.put(doc, similarityMap.getOrDefault(doc, 0.0) + w_t_d * w_t_q);
            }
        }

        

        //use a priority queue to store all documents
        PriorityQueue<Pair<Document, Double>> pq = new PriorityQueue<>();
        //insert every element in the hashtable to priority queue to get top k elements
        for (Document doc : similarityMap.keySet()) {
            pq.add(new Pair<Document, Double>(doc, similarityMap.get(doc)));
        }

        //initilize a list to store final result
        List<Pair<Document, Double>> ret = new ArrayList<>();

        //take the top K element from heap
        int size = pq.size();
        for (int i = 0; i < Math.min(K, size); i++) {
            ret.add(pq.poll());
        }
        return ret;
    }


    //We used the agumented term frequency as specified by assignment description
    public static double tf_i_q(String term, Map<String, Integer> freqMap, int maxFreq) {
        return 0.5 + 0.5 * freqMap.get(term) / maxFreq;
    }

    //tf_idf formula was found on lecture slide 3
    public static double tf_idf(int tf, double idf) {
        return (1 + Math.log10(tf + 0.0)) * idf;
    }

    //take the query file and out put retrivial results.
    public void runQuery(String queryFile, String outputFile, int topK, boolean eval, String method, boolean refine) {
        //create query parser using uery file
        QueryParser parser = new QueryParser(queryFile);


        try {
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
            int queryNumber = 1;
        
            for (String query : parser) {
                //process extension query
                if (refine) {
                    List<Pair<Document, Double>> topRank = null;
                    if (method.equals("1")) {
                        topRank = retriveTopK(query, 10);
                    } else if (method.equals("2")) {
                        topRank = retriveTopKbm25(query, 10, 0.3, 0.5);
                    }
                    
                    for (Pair<Document, Double> p : topRank) {
                        query += p.getKey().getRawText() + " ";
                    }
                }


                List<Pair<Document, Double>> res = new ArrayList<>();
                if (method.equals("1")) {
                    res = retriveTopK(query, topK);
                } else if (method.equals("2")) {
                    //the parameters were chosen via a grid search approach
                    res = retriveTopKbm25(query, topK, 0.3, 0.5);
                }

                int rank = 1;
                for (Pair<Document, Double> result : res) {
                    String docID = result.getKey().getID();
                    double score = result.getValue();
                    if (eval) { //out put evaluation file
                        writer.printf("%d Q0 %s %d %.3f muRun\n", queryNumber, docID, rank, score);
                    } else { //output normal result file
                        writer.printf("MB%03d Q0 %s %d %.3f muRun\n", queryNumber, docID, rank, score);
                    }
                    rank++;
                }
                queryNumber++;
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void printVocabulary(int n) {
        int i = 0;
        for (String word : vocabulary) {
            System.out.print(word + " ");
            if (i++ % 10 == 0) {
                System.out.println();
            }
            if (i == 100) {
                return;
            }
        }
    }

    


    public static void main(String[] args) {
        //initialize an information retrivial system
        IRSystem ir = new IRSystem("files/Trec_microblog11.txt", "files/StopWords.txt");
        //ir.printVocabulary(100);
        //run query on given queries, the last parameter denote two different options for calculating rank
        //0 - calculate ranking using regular tf-idf method
        //1 - calculate rankign using bm25 algorithme, which has a better performance compared to regular tf-idf
        String option = args.length == 0 ? "2" : args[0];
        ir.runQuery("files/topics_MB1-49.txt", "result.txt", 1000, false, option, true);
        // String[] qs = {"BBC World Service staff cuts", "TSA airport screening"};
        // for (String q : qs) {
        //     List<Pair<Document, Double>> res = ir.retriveTopKbm25(q, 10, 0.3, 0.5);
        //     for (Pair<Document, Double> p : res) {
        //         System.out.println("Doc ID: " + p.getKey().getID());
        //         System.out.println("Doc content: " + p.getKey().getRawText());
        //         System.out.println("Doc tokens: " + Arrays.toString(p.getKey().getTokenList().toArray()));
        //         System.out.println("Rank score: " + p.getValue() + "\n");

        //     }

        //     System.out.println("----------------------------------------");
        // }
    }
}
