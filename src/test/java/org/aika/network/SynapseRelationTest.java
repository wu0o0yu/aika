package org.aika.network;


import org.aika.Document;
import org.aika.Model;
import org.aika.neuron.INeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Range;
import org.aika.neuron.activation.Selector;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.neuron.Synapse.Relation.Type.COMMON_ANCESTOR;

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
                        .addSynapseRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addSynapseRelation(COMMON_ANCESTOR, 0)
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
                        .addSynapseRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addSynapseRelation(COMMON_ANCESTOR, 0)
        );


        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        doc.process();

        Assert.assertTrue(nC.getFinalActivations(doc).isEmpty());

    }
}
