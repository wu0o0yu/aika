package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.NegExcitatorySynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.input.InputNeuron;
import org.junit.Test;

public class MutualExclusionTest {


    @Test
    public void testPropagation() {
        Model m = new Model();

        InputNeuron in = new InputNeuron(m, "IN");
        PatternNeuron na = new PatternNeuron(m, "A");
        PatternNeuron nb = new PatternNeuron(m, "B");
        PatternNeuron nc = new PatternNeuron(m, "C");
        InhibitoryNeuron inhib = new InhibitoryNeuron(m, "I");

        Neuron.init(na.getProvider(), 1.0,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in.getProvider())
                        .setWeight(10.0)
                        .setRecurrent(false),
                new NegExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setRecurrent(true)
        );

        Neuron.init(nb.getProvider(), 1.5,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in.getProvider())
                        .setWeight(10.0)
                        .setRecurrent(false),
                new NegExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setRecurrent(true)
        );

        Neuron.init(nc.getProvider(), 1.2,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in.getProvider())
                        .setWeight(10.0)
                        .setRecurrent(false),
                new NegExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setRecurrent(true)
        );

        Neuron.init(inhib.getProvider(), 0.0,
                new InhibitorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(na.getProvider())
                        .setWeight(1.0)
                );




        Document doc = new Document(m, "test");

        in.addInput(doc,
                new Activation.Builder()
                    .setValue(1.0)
                    .setInputTimestamp(0)
                    .setFired(0)
        );

        System.out.println(doc.activationsToString());
    }
}
