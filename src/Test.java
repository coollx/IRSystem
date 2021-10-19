import java.io.*;

public class Test {
    public static void main(String[] args) {
        try(BufferedReader br = new BufferedReader(new FileReader("Trec_microblog11.txt"))) {
            int i = 0;
            for(String doc; i < 5 && (doc = br.readLine()) != null; i++) {

                System.out.println(doc.split("\\t")[1]);
            }
            
        } catch (FileNotFoundException e) {
			//TODO
			e.printStackTrace();
		}  catch (IOException e) {
			//TODO
			e.printStackTrace();		
		}
    }
}
