package org.aika.network;


import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Test;

public class TestNextWord {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(1);

        InputNeuron inA = m.createOrLookupInputNeuron("A");
        InputNeuron inB = m.createOrLookupInputNeuron("B");

        Neuron abN = m.initAndNeuron(m.createNeuron("AB"), 0.5,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setMinInput(0.95)
                        .setStartRangeMapping(Mapping.END)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        Document.APPLY_DEBUG_OUTPUT = true;
        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.networkStateToString(true, true, false, true));
    }
}
