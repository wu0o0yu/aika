package network;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import org.junit.jupiter.api.Test;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        Model m = new Model();
        m.setTrainingConfig(
                new Config()
                        .setMaturityThreshold(10)
        );

        PatternNeuron in = new PatternNeuron(m, "IN", true);
        in.binaryFrequency = 12;

        Document doc = new Document("");

        in.propagate(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
        );

        doc.train(m);

        System.out.println(doc.activationsToString());
    }
}
