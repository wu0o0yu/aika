package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

public class PositiveFeedbackLoop {


    @Test
    public void testSimplePosFeedbackLoop() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron nC = m.createNeuron("N C");
        Neuron nD = m.createNeuron("N D");

        Neuron.init(nC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        Neuron.init(nD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );


        Document doc = m.createDocument("Bla ");
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        doc.process();

        Assert.assertFalse(nC.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(nD.getFinalActivations(doc).isEmpty());
    }
}
