package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.range.Range;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.range.Range.Relation.EQUALS;

public class PositiveFeedbackLoopTest {


    @Test
    public void testSimplePosFeedbackLoop() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron nC = m.createNeuron("N C");
        Neuron nD = m.createNeuron("N D");

        Neuron.init(nC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRangeRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRangeRelation(EQUALS)
        );

        Neuron.init(nD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRangeRelation(Range.Relation.EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRangeRelation(EQUALS)
        );


        Document doc = m.createDocument("Bla ");
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        doc.process();

        Assert.assertFalse(nC.getActivations(doc, true).isEmpty());
        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }
}
