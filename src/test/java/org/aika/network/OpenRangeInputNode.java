package org.aika.network;


import org.aika.Neuron;
import org.aika.Provider;
import org.aika.lattice.NodeActivation;
import org.aika.Input;
import org.aika.Model;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.InputNode;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.aika.corpus.Range.Operator;
import static org.aika.corpus.Range.Mapping;
import static org.aika.corpus.Range.Mapping.END;


public class OpenRangeInputNode {

    Model m;
    Neuron in;
    InputNode inputNode;

    @Before
    public void init() {
        m = new Model();

        in = m.createNeuron("IN");
        m.initNeuron(m.createNeuron("OUT"),
                -0.001,
                new Input()
                        .setNeuron(in)
                        .setWeight(10.0f)
                        .setBiasDelta(0.0)
                        .setStartRangeMapping(Mapping.NONE)
                        .setEndRangeMapping(END)
                        .setStartRangeMatch(Operator.NONE)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true)
        );

        inputNode = in.get().outputNodes.firstEntry().getValue().get();
    }


    @Test
    public void testOpenRangeInputNode1() {
        Document doc = m.createDocument("                                ");

        in.addInput(doc, 3, 4);
        in.addInput(doc, 6, 7);
        in.addInput(doc, 10, 11);

        System.out.println(doc.nodeActivationsToString(false, true));

        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(null, 4), Operator.NONE, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(4, 7), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(7, 11), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        doc.clearActivations();
    }


    @Test
    public void testOpenRangeInputNode2() {
        Document doc = m.createDocument("                                ");

        in.addInput(doc, 3, 4);
        in.addInput(doc, 10, 11);
        in.addInput(doc, 6, 7);

        System.out.println(doc.nodeActivationsToString(false, true));

        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(null, 4), Operator.NONE, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(4, 7), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
        Assert.assertFalse(NodeActivation.select(doc, inputNode, null, new Range(7, 11), Operator.EQUALS, Operator.EQUALS, null, null).collect(Collectors.toList()).isEmpty());
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

        System.out.println(doc.nodeActivationsToString(false, true));
    }
}
