package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.*;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import org.junit.Test;

public class PatternTest {


    @Test
    public void testPattern() {
        Model m = new Model();

        PatternNeuron inA = new PatternNeuron(m, "IN A");
        PatternNeuron inB = new PatternNeuron(m, "IN B");
        PatternNeuron inC = new PatternNeuron(m, "IN C");

        PatternPartNeuron relN = new PatternPartNeuron(m, "Rel");

        InhibitoryNeuron inputInhibN = new InhibitoryNeuron(m, "INPUT INHIB");

        Neuron.init(relN, 1.0,
                new PrimaryInputSynapse.Builder()
                        .setNeuron(inputInhibN)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(inputInhibN)
                        .setWeight(10.0)
        );


        PatternPartNeuron eA = new PatternPartNeuron(m, "E A");
        PatternPartNeuron eB = new PatternPartNeuron(m, "E B");
        PatternPartNeuron eC = new PatternPartNeuron(m, "E C");

        PatternNeuron out = new PatternNeuron(m, "OUT");


        Neuron.init(eA, 1.0,
                new PrimaryInputSynapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        Neuron.init(eB, 1.0,
                new PrimaryInputSynapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0),
                new SamePatternSynapse.Builder()
                        .setNeuron(eA)
                        .setWeight(10.0),
                new SecondaryInputSynapse.Builder()
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(out)
                        .setWeight(10.0)
        );

        Neuron.init(eC, 1.0,
                new PrimaryInputSynapse.Builder()
                        .setNeuron(inC)
                        .setWeight(10.0),
                new SamePatternSynapse.Builder()
                        .setNeuron(eB)
                        .setWeight(10.0),
                new SecondaryInputSynapse.Builder()
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
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

        Activation actB = inB.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
        );

        relN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(1)
                        .setFired(0)
                        .addInputLink(actA)
                        .addInputLink(actB)
        );

        Activation actC = inC.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
        );

        relN.addInput(doc,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(2)
                        .setFired(0)
                        .addInputLink(actB)
                        .addInputLink(actC)
        );


        System.out.println(doc.activationsToString());
    }
}
