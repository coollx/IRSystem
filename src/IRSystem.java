import java.util.*;
import java.io.*;

public class IRSystem {
    private int N = 0; //total number of documents in system
    private String docFiles; // document path
    private String stopWordFile; //stop words path
    private static Tokenizer tokenizer; //singleton tokenizer, we only a single instance of tokenizer in memory
    private Set<String> vocabulary; //set of vocabulary 
    private Map<String, Document> documentMap; //associated each document is stored with it's ID
    private Map<String, Integer> documentFrequency;  //map of document frequency of each term
    private Map<String, List<Pair<Document, Integer>>> invertedIndex; //as you can see


    public IRSystem(String documents,String stopWord) {
        this.docFiles = documents;
        this.stopWordFile = stopWord;
        tokenizer = new Tokenizer(stopWordFile);
        vocabulary = new HashSet<>();
        documentMap = new HashMap<>();
        documentFrequency = new HashMap<>();
        invertedIndex = new HashMap<>();

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
            }
        } catch (FileNotFoundException e) {
			//TODO
			e.printStackTrace();
		}  catch (IOException e) {
			//TODO
			e.printStackTrace();		
		}

        //The norm of each document is calulated once we built the inverted index
        for (Document doc : documentMap.values()) {
            doc.calcNorme(documentFrequency, N);
        }

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
        for (int i = 0; i < K; i++) {
            ret.add(pq.poll());
        }

        //System.out.println(similarityMap.get("34931669714604032")/documentLength.get("34931669714604032"));
        return ret;
    }

    //We used the agumented term frequency as specified by assignment description
    public static double tf_i_q(String term, Map<String, Integer> freqMap, int maxFreq) {
        return 0.5 + 0.5 * freqMap.get(term) / maxFreq;
        //return freqMap.get(term);
        //System.out.println(freqMap.get(term));
        //return (freqMap.get(term) + 0.0) / maxFreq;
    }

    //tf_idf formula was found on lecture slide 3
    public static double tf_idf(int tf, double idf) {
        return (1 + Math.log10(tf + 0.0)) * idf;
    }


    public static void main(String[] args) {
        //IRSystem ir = new IRSystem("test.txt", "StopWords.txt");
        IRSystem ir = new IRSystem("files/Trec_microblog11.txt", "files/StopWords.txt");
        //IRSystem ir = new IRSystem("test2.txt", "StopWords.txt");


        String[] qs = {
            "BBC World Service staff cuts", 
            "2022 FIFA soccer", 
            "Haiti Aristide return",
            "Mexico drug war",
            "NIST computer security",
            "Egyptian protesters attack museum"
        };


        for (String q : qs) {
            System.out.println(q);
            List<Pair<Document, Double>> res = ir.retriveTopK(q, 5);

            for (Pair<Document, Double> p : res) {
                System.out.println(p);
            }

            System.out.println();
        }
    }
}
