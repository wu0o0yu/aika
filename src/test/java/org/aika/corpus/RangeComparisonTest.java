package org.aika.corpus;

import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Test;

import static org.aika.corpus.Range.Operator.*;

public class RangeComparisonTest {


    @Test
    public void testRangeComparison() {

        testSynapse(new Range(0, 10), new Range(0, 10), EQUALS, NONE, EQUALS, NONE, true);
        testSynapse(new Range(0, 20), new Range(5, 15), GREATER_THAN_EQUAL, NONE, LESS_THAN_EQUAL, NONE, true);

        // Overlaps
        testSynapse(new Range(0, 10), new Range(5, 15), NONE, LESS_THAN_EQUAL, NONE, GREATER_THAN_EQUAL, true);
        testSynapse(new Range(0, 20), new Range(5, 15), NONE, LESS_THAN_EQUAL, NONE, GREATER_THAN_EQUAL, true);
        testSynapse(new Range(0, 5), new Range(10, 15), NONE, LESS_THAN_EQUAL, NONE, GREATER_THAN_EQUAL, false);

        // Overlaps but does not contain
        testSynapse(new Range(0, 10), new Range(5, 15), GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN, true);
        testSynapse(new Range(0, 20), new Range(5, 15), GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN, false);
        testSynapse(new Range(5, 15), new Range(0, 20), GREATER_THAN, LESS_THAN, GREATER_THAN, GREATER_THAN, false);

    }



    public void testSynapse(Range ra, Range rb, Range.Operator bb, Range.Operator be, Range.Operator ee, Range.Operator eb, boolean targetValue) {
        for (int dir = 0; dir < 2; dir++) {
            Model m = new Model();

            Neuron na = m.createNeuron("A");
            Neuron nb = m.createNeuron("B");

            Neuron nc = m.createNeuron("C");

            m.initNeuron(nc, 5.0, INeuron.Type.EXCITATORY,
                    new Input()
                            .setNeuron(na)
                            .setWeight(1.0)
                            .setBias(0.0)
                            .setBeginToBeginRangeMatch(bb)
                            .setBeginToEndRangeMatch(be)
                            .setEndToEndRangeMatch(ee)
                            .setEndToBeginRangeMatch(eb),
                    new Input()
                            .setNeuron(nb)
                            .setWeight(10.0)
                            .setBias(-10.0)
                            .setBeginToBeginRangeMatch(EQUALS)
                            .setBeginToEndRangeMatch(NONE)
                            .setEndToEndRangeMatch(EQUALS)
                            .setEndToBeginRangeMatch(NONE)
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
