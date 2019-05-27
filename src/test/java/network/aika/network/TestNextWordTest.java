package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.*;

public class TestNextWordTest {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(null, 1);

        Neuron inA = m.createNeuron("A", INPUT);
        Neuron inB = m.createNeuron("B", INPUT);

        Neuron abN = Neuron.init(m.createNeuron("AB", EXCITATORY), 6.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(END_TO_BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(END_EQUALS)
        );

        Document doc = new Document(m, "aaaa bbbb  ", 0);

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.activationsToString());
    }
}
