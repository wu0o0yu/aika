package network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import org.junit.Test;

public class PropagateTest {


    @Test
    public void testPropagation() {
        Model m = new Model();

        ExcitatoryNeuron in = new ExcitatoryNeuron(m, "IN");
        ExcitatoryNeuron na = new ExcitatoryNeuron(m, "A");

        Neuron.init(na.getProvider(), 1.0,
                new ExcitatorySynapse.Builder()
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setRecurrent(false)
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
