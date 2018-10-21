package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.range.Range;
import network.aika.neuron.INeuron;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import static network.aika.neuron.range.Position.Operator.*;
import static network.aika.neuron.range.Range.Output.DIRECT;

public class LinkerTest {

/*
    @Test
    public void testLinker() {

        testSynapse(new Range(doc, 0, 20), new Range(5, 15), Relation.IS_ANCESTOR_OF, true);
        testSynapse(new Range(0, 10), new Range(0, 10), Relation.EQUALS, true);

        // Overlaps
        testSynapse(new Range(0, 10), new Range(5, 15), Relation.OVERLAPS, true);
        testSynapse(new Range(0, 20), new Range(5, 15), Relation.OVERLAPS, true);
        testSynapse(new Range(0, 5), new Range(10, 15), Relation.OVERLAPS, false);

        // Overlaps but does not contain
        testSynapse(new Range(0, 10), new Range(5, 15), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), true);
        testSynapse(new Range(0, 20), new Range(5, 15), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), false);
        testSynapse(new Range(5, 15), new Range(0, 20), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), false);

    }
*/

    public void testSynapse(Range ra, Range rb, Range.Relation rr, boolean targetValue) {
        for (int dir = 0; dir < 2; dir++) {
            Model m = new Model();

            Neuron na = m.createNeuron("A");
            Neuron nb = m.createNeuron("B");

            Neuron nc = m.createNeuron("C");

            Neuron.init(nc, 5.0, INeuron.Type.EXCITATORY,
                    new Synapse.Builder()
                            .setSynapseId(0)
                            .setNeuron(na)
                            .setWeight(1.0)
                            .setBias(0.0),
                    new Synapse.Builder()
                            .setSynapseId(1)
                            .setNeuron(nb)
                            .setWeight(10.0)
                            .setBias(-10.0)
                            .setRangeOutput(DIRECT),
                    new Relation.Builder()
                            .setFrom(1)
                            .setTo(0)
                            .setRangeRelation(rr)
            );

            Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            if (dir == 0) {
                na.addInput(doc, ra.begin.getFinalPosition(), ra.end.getFinalPosition());
                nb.addInput(doc, rb.begin.getFinalPosition(), rb.end.getFinalPosition());
            } else {
                nb.addInput(doc, rb.begin.getFinalPosition(), rb.end.getFinalPosition());
                na.addInput(doc, ra.begin.getFinalPosition(), ra.end.getFinalPosition());
            }

            doc.process();

            Assert.assertEquals(targetValue, na.get().getActivations(doc, false).iterator().next().getOutputLinks(false).count() != 0);
        }
    }


    @Test
    public void testLinkInputActivation() {

        Model m = new Model();

        Neuron na = m.createNeuron("A");
        Neuron nb = m.createNeuron("B");

        Neuron.init(nb, -0.5, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(na)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(DIRECT)
        );

        Document doc = m.createDocument("X");

        na.addInput(doc, 0, 1);
        nb.addInput(doc, 0, 1);

        Assert.assertTrue(nb.getActivations(doc, false).iterator().next().getInputLinks(false, false).findAny().isPresent());

    }
}
