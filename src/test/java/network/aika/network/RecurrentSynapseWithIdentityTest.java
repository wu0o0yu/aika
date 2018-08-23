package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static network.aika.neuron.activation.Range.Relation.EQUALS;

public class RecurrentSynapseWithIdentityTest {

    @Test
    public void testRecurrentSynapseWithIdentity() {
        Model m = new Model();

        Neuron in = m.createNeuron("IN");
        Neuron n = m.createNeuron("N");

        Neuron feedbackINA = m.createNeuron("FINA");
        Neuron feedbackNA = m.createNeuron("FNA");

        Neuron feedbackINB = m.createNeuron("FINB");
        Neuron feedbackNB = m.createNeuron("FNB");

        Neuron feedbackOrN = m.createNeuron("FOrN");

        Neuron.init(n, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(feedbackOrN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setIdentity(true)
                        .addRangeRelation(EQUALS, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(n)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(EQUALS, 0)
        );


        Neuron.init(feedbackNA, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(n)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(feedbackINA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .addRangeRelation(EQUALS, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(feedbackNB)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(EQUALS, 0)
        );


        Neuron.init(feedbackNB, 4.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(n)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(feedbackINB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .addRangeRelation(EQUALS, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(feedbackNA)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(EQUALS, 0)
        );


        Neuron.init(feedbackOrN, 0.0, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT, INeuron.Type.EXCITATORY, INeuron.LogicType.DISJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(feedbackNA)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(feedbackNB)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true, true)
        );

        Document doc = m.createDocument("A ");

        in.addInput(doc, 0, 1);
        feedbackINA.addInput(doc, 0, 1);
        feedbackINB.addInput(doc, 0, 1);

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertEquals(2, n.getActivations(doc, false).size());
    }
}
