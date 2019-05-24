package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.AncestorRelation;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.AncestorRelation.IS_ANCESTOR_OF;
import static network.aika.neuron.relation.AncestorRelation.IS_DESCENDANT_OF;
import static network.aika.neuron.relation.Relation.END_TO_BEGIN_EQUALS;
import static network.aika.neuron.relation.Relation.EQUALS;

public class SinglePositionSuppression {


    @Test
    public void testSinglePositionSuppression() {
        Model m = new Model();

        Neuron inABegin = m.createNeuron("IN-A-BEGIN");
        Neuron inAEnd = m.createNeuron("IN-A-END");
        Neuron.init(inAEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inABegin)
                        .setIdentity(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(END_TO_BEGIN_EQUALS)
        );

        Neuron inBBegin = m.createNeuron("IN-B-BEGIN");
        Neuron inBEnd = m.createNeuron("IN-B-END");
        Neuron.init(inBEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inBBegin)
                        .setIdentity(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(END_TO_BEGIN_EQUALS)
        );

        Neuron inCBegin = m.createNeuron("IN-C-BEGIN");
        Neuron inCEnd = m.createNeuron("IN-C-END");
        Neuron.init(inCEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inCBegin)
                        .setIdentity(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(END_TO_BEGIN_EQUALS)
        );

        Neuron inDBegin = m.createNeuron("IN-D-BEGIN");
        Neuron inDEnd = m.createNeuron("IN-D-END");
        Neuron.init(inDEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inDBegin)
                        .setIdentity(true)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(END_TO_BEGIN_EQUALS)
        );


        Neuron mBegin = m.createNeuron("M-BEGIN");
        Neuron mEnd = m.createNeuron("M-END");

        Neuron nBegin = m.createNeuron("N-BEGIN");
        Neuron nEnd = m.createNeuron("N-END");

        Neuron inhibit = m.createNeuron("Inhib");


        Neuron.init(mBegin, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inABegin)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inAEnd)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inBBegin)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inBEnd)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(4)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inCBegin)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(5)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(5)
                        .setNeuron(inCEnd)
                        .setWeight(3.0)
        );


        Neuron.init(mEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inABegin)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inAEnd)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inBBegin)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inBEnd)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(4)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inCBegin)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(5)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(5)
                        .setNeuron(inCEnd)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(5)
                        .setTo(OUTPUT)
                        .setRelation(new PositionRelation.Equals(0, 0))
        );



        Neuron.init(nBegin, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inBBegin)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inBEnd)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inCBegin)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inCEnd)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(4)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inDBegin)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(5)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(5)
                        .setNeuron(inDEnd)
                        .setWeight(3.0)
        );


        Neuron.init(nEnd, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inBBegin)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inBEnd)
                        .setWeight(3.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inCBegin)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(3)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inCEnd)
                        .setWeight(4.0),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(4)
                        .setRelation(new PositionRelation.Equals(0, 0)),
                new Synapse.Builder()
                        .setSynapseId(4)
                        .setNeuron(inDBegin)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(5)
                        .setRelation(new AncestorRelation.IsAncestorOf(false, true)),
                new Synapse.Builder()
                        .setSynapseId(5)
                        .setNeuron(inDEnd)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(5)
                        .setTo(OUTPUT)
                        .setRelation(new PositionRelation.Equals(0, 0))
        );



        Document doc = new Document(m, "abcd");

        Activation actInABegin = inABegin.addInput(doc, 0, 1);
        Activation actInBBegin = inBBegin.addInput(doc, 1, 2);
        Activation actInCBegin = inCBegin.addInput(doc, 2, 3);
        Activation actInDBegin = inDBegin.addInput(doc, 3, 4);

        doc.process();

        System.out.println(doc.activationsToString());
    }
}
