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

        Neuron.init(nC, 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron.init(nD, 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );


        Document doc = m.createDocument("Bla ");
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        doc.process();

        Assert.assertFalse(nC.getActivations(doc, true).isEmpty());
        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }
}
