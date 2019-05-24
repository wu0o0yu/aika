package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

public class PassiveInputNeuronTest {



    @Test
    public void testPassiveInputNeuron() {
        Model m = new Model();
        Document doc = new Document(m, "aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A", INPUT);

        Neuron inB = m.createNeuron("B", INPUT);
        inB.setPassiveInputFunction((s, oAct) -> 1.0);

        Neuron out = Neuron.init(
                m.createNeuron("OUT", EXCITATORY),
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
        );

        Activation outAct = out.getActivation(doc, 0, 1, false);

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertTrue(outAct.isFinalActivation());
    }
}
