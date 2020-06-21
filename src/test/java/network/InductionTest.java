package network;

import network.aika.Config;
import network.aika.text.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import org.junit.jupiter.api.Test;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        Model m = new Model();
        PatternNeuron in = new PatternNeuron(m, "IN", true);
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
