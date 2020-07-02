package network;

import network.aika.Config;
import network.aika.text.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        Model m = new TextModel();
        PatternNeuron in = new PatternNeuron(m, "A", "IN", true);
        in.setBinaryFrequency(12);

        Document doc = new Document("",
                new Config()
        );

        Activation act = new Activation(doc, in);
        act.propagateInput();

        doc.train(m);

        System.out.println(doc.activationsToString());
    }
}
