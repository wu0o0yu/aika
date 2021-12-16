package network.aika;

import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Random;


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

    private String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    @Test
    public void simplePhraseTest() {
        TextModel model = new TextModel();
        Config c = TestUtils.getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(false);

        Random r = new Random(1);

        for (int k = 0; k < 1000; k++) {
            String phrase = phrases[r.nextInt(phrases.length)];
            System.out.println("  " + phrase);

            Document doc = new Document(model, phrase);
            doc.setConfig(c);
            c.setEnableTraining(k > 100);

            int i = 0;
            TokenActivation lastToken = null;
            for(String t: doc.getContent().split(" ")) {
                int j = i + t.length();
                TokenActivation currentToken = doc.addToken(t, i, j);
                TokenActivation.addRelation(lastToken, currentToken);

                lastToken = currentToken;
                i = j + 1;
            }

            doc.process();
            doc.updateModel();

            System.out.println(doc.toString(true));
        }
    }
}
