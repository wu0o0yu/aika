package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import org.junit.Test;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;

public class PatternTest {


    @Test
    public void testPattern() {
        Model m = new Model();

        PatternNeuron inA = new PatternNeuron(m, "IN A");
        PatternNeuron inB = new PatternNeuron(m, "IN B");
        PatternNeuron inC = new PatternNeuron(m, "IN C");


        InhibitoryNeuron inputInhibN = new InhibitoryNeuron(m, "INPUT INHIB", PatternNeuron.type);
        Neuron.init(inputInhibN, 0.0,
                new InhibitorySynapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0),
                new InhibitorySynapse.Builder()
                        .setNeuron(inC)
                        .setWeight(1.0)
        );

        PatternPartNeuron relN = new PatternPartNeuron(m, "Rel");
        Neuron.init(relN, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(inputInhibN)
                        .setWeight(10.0)
        );


        PatternPartNeuron eA = new PatternPartNeuron(m, "E A");
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B");
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C");

        PatternNeuron out = new PatternNeuron(m, "OUT");


        Neuron.init(eA, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(inA)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        Neuron.init(eB, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(inB)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eA)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        Neuron.init(eC, 1.0,
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(inC)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(eB)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(INPUT_PATTERN)
                        .setRecurrent(false)
                        .setNegative(false)
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setPatternScope(SAME_PATTERN)
                        .setRecurrent(true)
                        .setNegative(false)
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        Neuron.init(out, 1.0,
                new PatternSynapse.Builder()
                        .setNeuron(eA)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eB)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eC)
                        .setWeight(10.0)
        );


        Document doc = new Document(m, "ABC");

        Activation actA = inA.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
        );

        Activation inInhibA = inputInhibN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(SAME_PATTERN, actA)
        );

        Activation actB = inB.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
        );


        Activation inInhibB = inputInhibN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(SAME_PATTERN, actB)
        );

        relN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
                        .addInputLink(INPUT_PATTERN, inInhibA)
                        .addInputLink(SAME_PATTERN, inInhibB)
        );

        Activation actC = inC.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
        );

        Activation inInhibC = inputInhibN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(SAME_PATTERN, actC)
        );

        relN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
                        .addInputLink(INPUT_PATTERN, inInhibB)
                        .addInputLink(SAME_PATTERN, inInhibC)
        );


        System.out.println(doc.activationsToString());
    }
}
