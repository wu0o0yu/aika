package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import org.junit.Test;

import static network.aika.neuron.PatternScope.*;

public class MutualExclusionTest {


    @Test
    public void testPropagation() {
        Model m = new Model();

        PatternNeuron in = new PatternNeuron(m, "IN");
        PatternPartNeuron na = new PatternPartNeuron(m, "A");
        PatternPartNeuron nb = new PatternPartNeuron(m, "B");
        PatternPartNeuron nc = new PatternPartNeuron(m, "C");
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, "I", PatternPartNeuron.type);

        Neuron.init(na, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(nb, 1.5,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(nc, 1.2,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(in)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(CONFLICTING_PATTERN)
                        .setRecurrent(true)
                        .setNegative(true)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
        );

        Neuron.init(inhib, 0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(na)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(nb)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(nc)
                        .setWeight(1.0)
                );


        Document doc = new Document(m, "test");

        Activation inAct = in.addInputActivation(doc,
                new Activation.Builder()
                    .setValue(1.0)
                    .setInputTimestamp(0)
                    .setFired(0)
        );

        doc.process();

        System.out.println(doc.activationsToString());
    }
}
