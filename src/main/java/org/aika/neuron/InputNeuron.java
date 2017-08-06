/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aika.neuron;


import org.aika.Activation;
import org.aika.Activation.State;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.Synapse.Key;

import java.util.Collections;

import static org.aika.corpus.Range.Operator.EQUALS;


/**
 * The <code>InputNeuron</code> class is used to feed information into the neural network. The input neuron has no input synapses. To add or remove input activations simply use the <code>addInput</code> or <code>removeInput</code> methods. An InputNeuron is typically created using the method <code>createOrLookupInputNeuron</code> in the model.
 *
 * @author Lukas Molzberger
 */
public class InputNeuron extends Neuron {


    public InputNeuron() {}


    public InputNeuron(String label) {
        this.label = label;
    }


    public InputNeuron(String label, boolean isBlocked) {
        super(label, isBlocked, true);
    }


    public static InputNeuron create(Document doc, InputNeuron n) {
        n.m = doc.m;

        InputNode node = InputNode.add(doc, new Key(false, false, null, 0, Operator.NONE, Mapping.NONE, true, Operator.NONE, Mapping.NONE, true), null);
        node.neuron = n;

        n.node = node;
        n.publish(doc);

        n.initialized = true;
        return n;
    }


    public void remove(Document doc) {
        unpublish(doc);
    }


    public void propagateAddedActivation(Document doc, Activation act) {
        State s = new State(1.0, 0, NormWeight.ZERO_WEIGHT);
        act.rounds.set(0, s);
        act.finalState = s;
        act.upperBound = 1.0;
        act.lowerBound = 1.0;

        for(InputNode out: outputNodes.values()) {
            out.addActivation(doc, act);
        }
    }


    public void addInput(Document doc, int begin, int end) {
        addInput(doc, begin, end, null, doc.bottom);
    }


    public void addInput(Document doc, int begin, int end, InterprNode o) {
        addInput(doc, begin, end, null, o);
    }


    public void addInput(Document doc, int begin, int end, Integer rid) {
        addInput(doc, begin, end, rid, doc.bottom);
    }


    public void addInput(Document doc, int begin, int end, Integer rid, InterprNode o) {
        Node.addActivationAndPropagate(doc, new Activation.Key(node, new Range(begin, end), rid, o), Collections.emptySet());

        doc.propagate();
    }

    public void addInput(Document doc, int begin, int end, Integer rid, InterprNode o, double value) {
        Range r = new Range(begin, end);
        Node.addActivationAndPropagate(doc, new Activation.Key(node, r, rid, o), Collections.emptySet());

        doc.propagate();

        Activation act = Activation.get(doc, node, rid, r, EQUALS, EQUALS, o, InterprNode.Relation.EQUALS);
        State s = new State(value, 0, NormWeight.ZERO_WEIGHT);
        act.rounds.set(0, s);
        act.finalState = s;
    }


    public void removeInput(Document doc, int begin, int end) {
        removeInput(doc, begin, end, null, doc.bottom);
    }


    public void removeInput(Document doc, int begin, int end, InterprNode o) {
        removeInput(doc, begin, end, null, o);
    }


    public void removeInput(Document doc, int begin, int end, Integer rid) {
        removeInput(doc, begin, end, rid, doc.bottom);
    }


    public void removeInput(Document doc, int begin, int end, Integer rid, InterprNode o) {
        Range r = new Range(begin, end);
        Activation act = Activation.get(doc, node, rid, r, EQUALS, EQUALS, o, InterprNode.Relation.EQUALS);
        Node.removeActivationAndPropagate(doc, act, Collections.emptySet());

        doc.propagate();
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("in(");
        sb.append(id);
        if(label != null) {
            sb.append(",");
            sb.append(label);
        }
        sb.append(")");
        return sb.toString();
    }
}
