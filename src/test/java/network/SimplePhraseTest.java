package network;

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
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


    @Test
    public void simplePhraseTest() {
        TextModel model = new TextModel();

        Random r = new Random(1);

        for (int k = 0; k < 1000; k++) {
            String p = phrases[r.nextInt(phrases.length)];
            System.out.println("  " + p);

            Neuron.debugOutput = p.equalsIgnoreCase("der Hund");

            Document doc = new Document(p,
                    new Config()
                            .setAlpha(0.99)
                            .setLearnRate(-0.1)
                            .setInductionThreshold(1.4)
            );

            int i = 0;
            for (String w : p.split(" ")) {
                int j = i + w.length() + 1;
                j = Math.min(j, p.length());

                doc.processToken(model, i, j, w);
                i = j;
            }
            doc.process();

            doc.train(model);

            if (Neuron.debugOutput) {
                System.out.println(doc.activationsToString(true));

//                System.out.println(doc.gradientsToString());
//                System.out.println();
            }
        }
    }
}
