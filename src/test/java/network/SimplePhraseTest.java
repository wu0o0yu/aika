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

        ManuelInductionModel inductionModel = new ManuelInductionModel(model);

        Random r = new Random(1);

        for (int k = 0; k < 1000; k++) {
            String phrase = phrases[r.nextInt(phrases.length)];
            System.out.println("  " + phrase);

            Neuron.debugOutput = phrase.equalsIgnoreCase("der Hund");

            Document doc = new Document(phrase);

            inductionModel.initToken(doc);

            doc.process(model);
/*
            if(k < 100) {
                doc.count();
                model.addToN(doc.length());
            } else {
                doc.train(model);
                m.addToN(length());
            }
*/
            if (Neuron.debugOutput) {
                System.out.println(doc.activationsToString(true));

//                System.out.println(doc.gradientsToString());
//                System.out.println();
            }
        }
    }
}
