package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.range.Range;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.range.Range.Relation.BEGIN_EQUALS;
import static network.aika.neuron.range.Range.Relation.END_EQUALS;
import static network.aika.neuron.range.Range.Relation.EQUALS;

public class TestNextWord {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(null, 1);

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron abN = Neuron.init(m.createNeuron("AB"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-9.5),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-9.5),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRangeRelation(BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRangeRelation(END_EQUALS)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.activationsToString(false, false, true));
    }
}
