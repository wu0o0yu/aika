package org.aika.corpus;

import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.corpus.Range.Relation;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.corpus.Range.Operator.*;

public class RangeComparisonTest {


    @Test
    public void testRangeComparison() {

        testSynapse(new Range(0, 10), new Range(0, 10), Relation.EQUALS, true);
        testSynapse(new Range(0, 20), new Range(5, 15), Relation.CONTAINED_IN, true);

        // Overlaps
        testSynapse(new Range(0, 10), new Range(5, 15), Relation.OVERLAPS, true);
        testSynapse(new Range(0, 20), new Range(5, 15), Relation.OVERLAPS, true);
        testSynapse(new Range(0, 5), new Range(10, 15), Relation.OVERLAPS, false);

        // Overlaps but does not contain
        testSynapse(new Range(0, 10), new Range(5, 15), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), true);
        testSynapse(new Range(0, 20), new Range(5, 15), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), false);
        testSynapse(new Range(5, 15), new Range(0, 20), Relation.create(GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN), false);

    }


    public void testSynapse(Range ra, Range rb, Relation rr, boolean targetValue) {
        for (int dir = 0; dir < 2; dir++) {
            Model m = new Model();

            Neuron na = m.createNeuron("A");
            Neuron nb = m.createNeuron("B");

            Neuron nc = m.createNeuron("C");

            Neuron.init(nc, 5.0, INeuron.Type.EXCITATORY,
                    new Synapse.Builder()
                            .setNeuron(na)
                            .setWeight(1.0)
                            .setBias(0.0)
                            .setRangeMatch(rr),
                    new Synapse.Builder()
                            .setNeuron(nb)
                            .setWeight(10.0)
                            .setBias(-10.0)
                            .setRangeMatch(Relation.EQUALS)
                            .setRangeOutput(true)
            );

            Document doc = m.createDocument("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            if (dir == 0) {
                na.addInput(doc, ra.begin, ra.end);
                nb.addInput(doc, rb.begin, rb.end);
            } else {
                nb.addInput(doc, rb.begin, rb.end);
                na.addInput(doc, ra.begin, ra.end);
            }

            Assert.assertEquals(targetValue, !na.get().getAllActivations(doc).iterator().next().neuronOutputs.isEmpty());
        }
    }
}
