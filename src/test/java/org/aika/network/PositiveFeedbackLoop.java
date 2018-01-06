package org.aika.network;

import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

public class PositiveFeedbackLoop {


    @Test
    public void testSimplePosFeedbackLoop() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron nC = m.createNeuron("N C");
        Neuron nD = m.createNeuron("N D");

        m.initNeuron(nC, 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        m.initNeuron(nD, 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );


        Document doc = m.createDocument("Bla ");
        inA.addInput(doc, 0, 3);
        inB.addInput(doc, 0, 3);

        doc.process();

        Assert.assertFalse(nC.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(nD.getFinalActivations(doc).isEmpty());
    }
}
