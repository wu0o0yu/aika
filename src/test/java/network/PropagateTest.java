package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import org.junit.Test;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;

public class PropagateTest {


    @Test
    public void testPropagation() {
        Model m = new Model();

        PatternNeuron in = new PatternNeuron(m, "IN");
        PatternPartNeuron na = new PatternPartNeuron(m, "A");

        Neuron.init(na.getProvider(), 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setPropagate(true)
                        .setNeuron(in)
                        .setWeight(10.0)
                );


        Document doc = new Document(m, "test");

        in.addInputActivation(doc,
                new Activation.Builder()
                    .setValue(1.0)
                    .setInputTimestamp(0)
                    .setFired(0)
        );

        System.out.println(doc.activationsToString());
    }
}
