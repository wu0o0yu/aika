package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Mapping;
import network.aika.neuron.INeuron;
import org.junit.Test;

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
                        .setBias(-9.5)
                        .addRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS, 1)
                        .setRangeOutput(true, false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setRangeOutput(false, true)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.activationsToString(false, false, true));
    }
}
