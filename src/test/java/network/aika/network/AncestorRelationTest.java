package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Collectors;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.AncestorRelation.COMMON_ANCESTOR;
import static network.aika.neuron.relation.AncestorRelation.IS_ANCESTOR_OF;
import static network.aika.neuron.relation.AncestorRelation.NOT_ANCESTOR_OF;
import static network.aika.neuron.relation.Relation.EQUALS;


public class AncestorRelationTest {


    @Test
    public void testAncestorRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A", INPUT);

        Neuron inB = Neuron.init(m.createNeuron("B", EXCITATORY),
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron outC = Neuron.init(m.createNeuron("C", EXCITATORY),
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(IS_ANCESTOR_OF),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Document doc = new Document(m, "aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, 0, 1, false);

        System.out.println(doc.activationsToString());

        Assert.assertNotNull(outC1);
    }

    @Test
    public void testAncestorRelation1() {
        Model m = new Model();
        Document doc = new Document(m, "aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A", INPUT);

        Neuron nB = Neuron.init(m.createNeuron("B", EXCITATORY), 5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron nC = Neuron.init(m.createNeuron("C", EXCITATORY), 5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron nD = Neuron.init(m.createNeuron("D", EXCITATORY), 5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setSynapseId(0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setSynapseId(1),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(COMMON_ANCESTOR),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inA.addInput(doc, 0, 1);

        doc.process();

        Assert.assertFalse(nD.getActivations(doc, true).collect(Collectors.toList()).isEmpty());
    }


    @Test
    public void testAncestorRelation2() {
        Model m = new Model();
        Document doc = new Document(m, "aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A", INPUT);

        Neuron inB = m.createNeuron("B", INPUT);

        Neuron nC = Neuron.init(m.createNeuron("C", EXCITATORY), 5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setSynapseId(0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setSynapseId(1),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(COMMON_ANCESTOR),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        doc.process();

        Assert.assertTrue(nC.getActivations(doc, true).collect(Collectors.toList()).isEmpty());
    }


    @Test
    public void testNotAncestorOfRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A", INPUT);

        Neuron inB = m.createNeuron("B", INPUT);
/*
        Neuron.setWeight(inB,
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );
*/
        Neuron outC = Neuron.init(m.createNeuron("C", EXCITATORY),
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setRecurrent(false),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(NOT_ANCESTOR_OF),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        Document doc = new Document(m, "aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, 0, 1, false);

        System.out.println(doc.activationsToString());

        Assert.assertNotNull(outC1);
    }

}
