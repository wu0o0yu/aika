package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Range;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.range.Range.Relation.EQUALS;

public class PassiveInputNeuronTest {



    @Test
    public void testPassiveInputNeuron() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");
        Neuron.registerPassiveInputNeuron(inB, (s, oAct) -> 1.0);

        Neuron out = Neuron.init(m.createNeuron("OUT"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRangeRelation(Range.Relation.EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRangeRelation(EQUALS)
        );


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
        );

        Activation outAct = out.getActivation(doc, new Range(doc, 0, 1), false);

        System.out.println(doc.activationsToString(false, true, true));

        doc.process();

        Assert.assertTrue(outAct.isFinalActivation());
    }
}
