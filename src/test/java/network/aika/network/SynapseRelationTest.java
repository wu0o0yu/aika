package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Relation.InstanceRelation.Type.COMMON_ANCESTOR;

public class SynapseRelationTest {


    @Test
    public void testSynapseRelation() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron nB = Neuron.init(m.createNeuron("B"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
        );

        Neuron nD = Neuron.init(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(nB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
                        .setSynapseId(0)
                        .addInstanceRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addInstanceRelation(COMMON_ANCESTOR, 0)
        );


        inA.addInput(doc, 0, 1);

        doc.process();

        Assert.assertFalse(nD.getFinalActivations(doc).isEmpty());

    }

    @Test
    public void testSynapseRelation1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");


        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
                        .setSynapseId(0)
                        .addInstanceRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addInstanceRelation(COMMON_ANCESTOR, 0)
        );


        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        doc.process();

        Assert.assertTrue(nC.getFinalActivations(doc).isEmpty());

    }
}
