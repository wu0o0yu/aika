package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static network.aika.ActivationFunction.RECTIFIED_LINEAR_UNIT;
import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

public class WeakConflictTest {


    @Ignore
    @Test
    public void testWeakConflict() {
        Model m = new Model();

        Neuron in = m.createNeuron("IN");

        Neuron na = m.createNeuron("A");
        Neuron nb = m.createNeuron("B");

        Neuron.init(na, 10.0, RECTIFIED_LINEAR_UNIT, EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nb)
                        .setWeight(-2.0)
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

        Neuron.init(nb, 10.0, RECTIFIED_LINEAR_UNIT, EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Document doc = new Document(m,"X ");

        in.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertEquals(1, na.getActivations(doc, true).count());
    }
}
