package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.range.Range.Relation.CONTAINS;
import static network.aika.neuron.range.Range.Relation.EQUALS;

public class InputRelationTest {


    @Test
    public void testInputRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron nC = m.createNeuron("C");
        Neuron nD = m.createNeuron("D");

        Neuron.init(nC, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(10)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Synapse.Builder()
                        .setSynapseId(11)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Relation.Builder()
                        .setFrom(10)
                        .setTo(11)
                        .setRangeRelation(CONTAINS),
                new Relation.Builder()
                        .setFrom(10)
                        .setTo(OUTPUT)
                        .setRangeRelation(EQUALS)
        );

        Neuron.init(nD, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setFromInput(11)
                        .setTo(1)
                        .setRangeRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRangeRelation(EQUALS)
        );


        Document doc = m.createDocument("aabbbbaa ");

        inA.addInput(doc, 0, 8);
        inB.addInput(doc, 2, 6);

        doc.process();

        Assert.assertFalse(nD.getActivations(doc,  true).isEmpty());

        System.out.println(doc.activationsToString(true, false, false));

        doc.clearActivations();
    }
}
