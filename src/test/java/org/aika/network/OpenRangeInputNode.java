package org.aika.network;


import org.aika.Activation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.InputNode;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.aika.corpus.Range.Operator;
import static org.aika.corpus.Range.Mapping;
import static org.aika.corpus.Range.Mapping.END;


public class OpenRangeInputNode {

    Model m;
    InputNeuron in;
    InputNode inputNode;

    @Before
    public void init() {
        m = new Model();

        in = m.createOrLookupInputSignal("IN");
        m.createOrNeuron(new Neuron("OUT"),
                new Input()
                        .setNeuron(in)
                        .setWeight(10.0)
                        .setStartRangeMapping(Mapping.NONE)
                        .setEndRangeMapping(END)
                        .setStartRangeMatch(Operator.NONE)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        inputNode = in.outputNodes.firstEntry().getValue();
    }


    @Test
    public void testOpenRangeInputNode1() {
        Document doc = m.createDocument("                                ");

        in.addInput(doc, 3, 4);
        in.addInput(doc, 6, 7);
        in.addInput(doc, 10, 11);

        System.out.println(doc.networkStateToString(false, true));

        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(null, 4), Operator.NONE, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(4, 7), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(7, 11), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        doc.clearActivations();
    }


    @Test
    public void testOpenRangeInputNode2() {
        Document doc = m.createDocument("                                ");

        in.addInput(doc, 3, 4);
        in.addInput(doc, 10, 11);
        in.addInput(doc, 6, 7);

        System.out.println(doc.networkStateToString(false, true));

        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(null, 4), Operator.NONE, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(4, 7), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(Activation.select(doc, inputNode, null, new Range(7, 11), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        doc.clearActivations();
    }


    @Test
    public void testOpenRangeInputNode3() {
        Document doc = m.createDocument("                                ");

        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);

        in.addInput(doc, 9, 10, o012);
        in.addInput(doc, 24, 25, o01);
        in.addInput(doc, 4, 5, o0);

        System.out.println(doc.networkStateToString(false, true));
    }
}
