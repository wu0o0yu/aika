package network;

import network.aika.neuron.Templates;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

public class GradientTest {

    @Test
    public void gradientAndInduction() throws InterruptedException {
        TextModel m = new TextModel();

        Document doc = new Document("A B");

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

            i = j + 1;
        }
        doc.process(m);

    }
}
