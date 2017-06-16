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
package org.aika.network;


import org.aika.Iteration;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.Activation;
import org.aika.lattice.Node;
import org.aika.lattice.InputNode;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {


    public static Activation addActivationAndPropagate(Iteration t, Activation.Key ak) {
        return addActivationAndPropagate(t, ak, Collections.emptySet());
    }


    public static Activation addActivationAndPropagate(Iteration t, Activation.Key ak, Set<Activation> inputActs) {
        Node.addActivationAndPropagate(t, ak, inputActs);
        t.propagate();
        return Activation.get(t, ak.n, ak.rid, ak.r, Range.Relation.CONTAINS, ak.o, Option.Relation.EQUALS);
    }


    public static void removeActivationAndPropagate(Iteration t, Activation.Key ak) {
        Activation act = Activation.get(t, ak.n, 0, ak.r, Range.Relation.EQUALS, ak.o, Option.Relation.EQUALS);
        removeActivationAndPropagate(t, act);
    }


    public static void removeActivationAndPropagate(Iteration t, Activation act) {
        Node.removeActivationAndPropagate(t, act, act.inputs.values());
        t.propagate();
    }


    public static Activation addActivation(InputNode in, Iteration t, Activation inputAct) {
        in.addActivation(t, inputAct);
        t.propagate();
        if(in instanceof InputNode) {
            return Activation.get(t, in, inputAct.key.rid, inputAct.key.r, Range.Relation.CONTAINS, inputAct.key.o, Option.Relation.EQUALS);
        }
        return null;
    }


    public static void removeActivation(InputNode in, Iteration t, Activation inputAct) {
        in.removeActivation(t, inputAct);
        t.propagate();
    }


    public static InputNode addOutputNode(Iteration t, Neuron n, Integer relativeRid, Integer absoluteRid) {
        return addOutputNode(t, n, relativeRid, absoluteRid, true, Synapse.RangeSignal.START, Synapse.RangeVisibility.MAX_OUTPUT, Synapse.RangeSignal.END, Synapse.RangeVisibility.MAX_OUTPUT);
    }


    public static InputNode addOutputNode(Iteration t, Neuron n, Integer relativeRid, Integer absoluteRid, boolean matchRange, Synapse.RangeSignal startSignal, Synapse.RangeVisibility startVisibility, Synapse.RangeSignal endSignal, Synapse.RangeVisibility endVisibility) {
        return InputNode.add(t, new Synapse.Key(false, false, relativeRid, absoluteRid, matchRange, startSignal, startVisibility, endSignal, endVisibility), n);
    }

    public static Activation get(Iteration t, Node n, Range r, Option o) {
        return Activation.get(t, n, null, r, Range.Relation.OVERLAPS, o, Option.Relation.EQUALS);
    }
}
