package network.aika;

import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static network.aika.utils.TestUtils.getConfig;


public class SimplePhraseTest {

    public String[] phrases = new String[]{
            "der Hund",
            "die Katze",
            "der Vogel",
            "das Pferd",
            "die Maus",
            "der Elefant",
            "der Löwe",
            "das Pony",
            "die Spinne",
            "der Jaguar"
    };

    @Test
    public void simplePhraseTest() {
        TextModel model = new TextModel();
        Config c = getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(false);

        Random r = new Random(1);

        for (int k = 0; k < 1000; k++) {
            String phrase = phrases[r.nextInt(phrases.length)];
            System.out.println("  " + phrase);

            Document doc = new Document(model, phrase);
            doc.setConfig(c);
            c.setTrainingEnabled(k > 100);

            int i = 0;
            TokenActivation lastToken = null;
            for(String t: doc.getContent().split(" ")) {
                int j = i + t.length();
                TokenActivation currentToken = doc.addToken(t, i, j);
                TokenActivation.addRelation(lastToken, currentToken);

                lastToken = currentToken;
                i = j + 1;
            }

            doc.processFinalMode();
            doc.postProcessing();
            doc.updateModel();

            System.out.println(doc);
        }
    }
}
