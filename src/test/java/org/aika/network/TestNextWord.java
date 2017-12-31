package org.aika.network;


import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Mapping;
import org.aika.neuron.INeuron;
import org.junit.Test;

public class TestNextWord {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(null, 1);

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron abN = m.initNeuron(m.createNeuron("AB"), 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setRangeMatch(Range.Relation.END_EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setBeginRangeMapping(Mapping.END)
                        .setRangeMatch(Range.Relation.BEGIN_EQUALS)
                        .setBeginRangeOutput(true)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        Document.APPLY_DEBUG_OUTPUT = true;
        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.neuronActivationsToString(true, false, true));
    }
}
