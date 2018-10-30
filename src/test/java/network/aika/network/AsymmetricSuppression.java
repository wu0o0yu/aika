package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.range.Range;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class AsymmetricSuppression {


    @Ignore
    @Test
    public void testAsymmetricSuppression() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");

        Neuron outN = Neuron.init(m.createNeuron("OUT"),
                0.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        Document doc = m.createDocument("a");

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString(true, false, true));

        Assert.assertTrue(outN.getActivations(doc, true).isEmpty());

        doc.clearActivations();
    }
}
