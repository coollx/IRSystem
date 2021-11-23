import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.*;

public class QueryParser implements Iterable<String>  {
    List<String> queryList;

    public QueryParser(String queryFile) {
        queryList = new ArrayList<>();
        try {
            BufferedReader bufferR = new BufferedReader(new FileReader(queryFile));
            while (bufferR.readLine() != null) {

                String number = bufferR.readLine().split(" ")[2];
                String title = bufferR.readLine().replaceAll("<title>", "").replaceAll("</title>", "");
                String querytime = bufferR.readLine().replaceAll("<querytime>", "").replaceAll("</querytime>", "");
                String querytweettime = bufferR.readLine().replaceAll("<querytweettime>", "").replaceAll("</querytweettime>", "");
                
                //read </top> and the blank line between two topics
                bufferR.readLine();
                bufferR.readLine();
                
                //System.out.println(title);
                queryList.add(title);
            }

            bufferR.close();
                
        } catch ( IOException | NullPointerException e ) {
            e.printStackTrace();
        }


    }
       
    @Override
    public Iterator<String> iterator() {
        return queryList.iterator();
    }
}
