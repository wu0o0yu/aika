package network;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Util {


    public static List<String> loadExamplesAsWords(File dir) throws IOException {
        ArrayList<String> words = new ArrayList<>();
        for (File f : dir.listFiles()) {
            InputStream is = new FileInputStream(f);
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String txt = writer.toString();
            txt = txt + " ";

            int wb = 0;
            char lc = ' ';
            for(int i = 0; i < txt.length(); i++) {
                char c = txt.charAt(i);

                if(Character.isLetter(c) && !Character.isLetter(lc)) {
                    wb = i;
                } else if(!Character.isLetter(c) && Character.isLetter(lc)) {
                    String word = txt.substring(wb, i);

                    words.add(word.toLowerCase());
                }

                lc = c;
            }
        }
        return words;
    }

}
