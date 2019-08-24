package network.aika.training.utils;

import network.aika.neuron.Neuron;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Parser {


    public static List<String> loadExamplesAsWords() throws IOException {
        String[] files = new String[]{
                "Aschenputtel",
                "BruederchenUndSchwesterchen",
                "DasTapfereSchneiderlein",
                "DerFroschkoenig",
                "DerGestiefelteKater",
                "DerGoldeneSchluessel",
                "DerSuesseBrei",
                "DerTeufelMitDenDreiGoldenenHaaren",
                "DerWolfUndDieSiebenJungenGeisslein",
                "DieBremerStadtmusikanten",
                "DieDreiFedern",
                "DieSterntaler",
                "DieWeisseSchlange",
                "DieZwoelfBrueder",
                "Dornroeschen",
                "FrauHolle",
                "HaenselUndGretel",
                "HansImGlueck",
                "JorindeUndJoringel",
                "KatzeUndMausInGesellschaft",
                "MaerchenVonEinemDerAuszogDasFuerchtenZuLernen",
                "Marienkind",
                "Rapunzel",
                "Rotkaeppchen",
                "Rumpelstilzchen",
                "SchneeweisschenUndRosenrot",
                "Schneewitchen",
                "TischleinDeckDich",
                "VonDemFischerUndSeinerFrau"
        };


        ArrayList<String> words = new ArrayList<>();
        for (String fn : files) {
            File f = new File("./src/test/resources/maerchen/" + fn + ".txt");
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

    /*
    public static ArrayList<String> loadExamplesAsWords() throws IOException {
        String[] files = new String[]{
                "Aschenputtel",
                "BruederchenUndSchwesterchen",
                "DasTapfereSchneiderlein",
                "DerFroschkoenig",
                "DerGestiefelteKater",
                "DerGoldeneSchluessel",
                "DerSuesseBrei",
                "DerTeufelMitDenDreiGoldenenHaaren",
                "DerWolfUndDieSiebenJungenGeisslein",
                "DieBremerStadtmusikanten",
                "DieDreiFedern"/*,
                "DieSterntaler",
                "DieWeisseSchlange",
                "DieZwoelfBrueder",
                "Dornroeschen",
                "FrauHolle",
                "HaenselUndGretel",
                "HansImGlueck",
                "JorindeUndJoringel",
                "KatzeUndMausInGesellschaft",
                "MaerchenVonEinemDerAuszogDasFuerchtenZuLernen",
                "Marienkind",
                "Rapunzel",
                "Rotkaeppchen",
                "Rumpelstilzchen",
                "SchneeweisschenUndRosenrot",
                "Schneewitchen",
                "TischleinDeckDich",
                "VonDemFischerUndSeinerFrau"
        };


        ArrayList<String> inputs = new ArrayList<>();
        for (String fn : files) {
            InputStream is = Parser.class.getResourceAsStream("../../../resources/maerchen/" + fn + ".txt");
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String txt = writer.toString();

            txt = txt.replace('.', ' ');
            txt = txt.replace(',', ' ');
            txt = txt.replace('?', ' ');
            txt = txt.replace('!', ' ');
            txt = txt.replace('"', ' ');
            txt = txt.replace('-', ' ');
            txt = txt.replace(':', ' ');
            txt = txt.replace(';', ' ');
            txt = txt.replace('\n', ' ');
            txt = txt.replace("  ", " ");
            txt = txt.replace("  ", " ");

            for(String word: txt.split(" ")) {
                inputs.add(word);
            }
        }
        return inputs;
    }
*/

    public static TDocument parse(MetaModel model, String txt, DictionaryLookup lookup) {
        TDocument doc = new TDocument(model, txt);
        txt = txt + ' ';

        char lc = ' ';
        int begin = 0;
        int end = 0;
        for(int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);

            if(i + 1 == txt.length()) {
                end = i;
            }

            if((isSeparator(lc) && !isSeparator(c)) || i + 1 == txt.length()) {
                if(begin != end) {
                    String wordStr = txt.substring(begin, end);
                    Neuron wn = lookup.lookup(doc, wordStr);

                    wn.addInput(doc, begin, i);
                }
                begin = i;
            } else if(!isSeparator(lc) && isSeparator(c)) {
                end = i;
            }

            lc = c;
        }

        doc.process();

        return doc;
    }


    private static boolean isSeparator(char c) {
        return c == ' ' || c == '(' || c == ')' || c == '/' || c == ',' || c == ';' || c == ':' || c == '.' || c == '!' || c == '?' || c == '-' || c == '\n';
    }


    public interface DictionaryLookup {
        Neuron lookup(TDocument doc, String word);
    }
}
