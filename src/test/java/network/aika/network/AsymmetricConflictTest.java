package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static network.aika.ActivationFunction.RECTIFIED_LINEAR_UNIT;
import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

public class AsymmetricConflictTest {


    @Test
    public void testAsymmetricConflict() {
        Model m = new Model();

        Neuron in = m.createNeuron("IN", INPUT);
        Neuron inNeg = m.createNeuron("IN-NEG", INPUT);

        Neuron outA = m.createNeuron("OUT-A", EXCITATORY, RECTIFIED_LINEAR_UNIT);
        Neuron outB = m.createNeuron("OUT-B", EXCITATORY, RECTIFIED_LINEAR_UNIT);

        Neuron.init(outA, 5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(outB)
                        .setWeight(-100.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inNeg)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(2)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(outB, 4.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(outA)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Document doc = new Document(m, "X ");

        in.addInput(doc, 0, 1);
        inNeg.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertEquals(0, outA.getActivations(doc, true).count());
        Assert.assertEquals(1, outB.getActivations(doc, true).count());
    }
}
