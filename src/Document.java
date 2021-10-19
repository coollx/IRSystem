import java.util.*;
public class Document {
    //singleton tokenizer, we only keep one tokenizer instance in memory.
    private static Tokenizer tkn = new Tokenizer("files/StopWords.txt");

    //document ID
    private String docID;
    //document's raw text
    private String rawText;
    //list of raw text tokenized using porter stemmer, stop words removed.
    private List<String> tokenList;
    //token frequency map
    private Map<String, Integer> freqMap;
    //tf-idf vector
    private List<Double> tf_idf;
    //norme of tf-idf vector
    private double norme;

    public Document(String id, String text) {
        docID = id;
        rawText = text;
        tokenList = tkn.getTokens(text);
        tf_idf = new ArrayList<>();
        norme = 0;

        freqMap = new HashMap<>();
        for (String token : tokenList) {
            freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
        }
    }

    public String getID() {
        return docID;
    }

    public String getRawText() {
        return rawText;
    }

    public List<String> getTokenList() {
        return tokenList;
    }

    public Map<String, Integer> getFreqMap() {
        return freqMap;
    }

    public int length() {
        return tokenList.size();
    }


    public void calcNorme(Map<String, Integer> documentFrequency, int N) {
        for (String token : tokenList) {
            tf_idf.add((1 + Math.log10(0.0 + freqMap.get(token))) * Math.log10((0.0 + N) / (0.0 + documentFrequency.get(token))));
        }
        double temp = 0;
        for (double d : tf_idf) {
            temp += d * d;
        }
        norme = Math.sqrt(temp);
    }

    public double getNorme() {
        return norme;
    }

    @Override
    public String toString() {
        return "Doc# " + getID() + ": " + Arrays.toString(tokenList.toArray());
    }

}
