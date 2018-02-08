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

import org.aika.corpus.Document;
import org.aika.corpus.InterpretationNode;
import org.aika.corpus.Range;
import org.aika.lattice.Node;
import org.aika.lattice.NodeActivation;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.EQUALS;
import static org.aika.corpus.Range.Operator.GREATER_THAN_EQUAL;
import static org.aika.lattice.Node.MAX_NODE;
import static org.aika.lattice.Node.MIN_NODE;


/**
 * The {@code Selector} class retrieves activations either from the document or the associated neuron.
 *
 * @author Lukas Molzberger
 */
public class Selector {


    public static Activation get(Document doc, INeuron n, Integer rid, Range r, Range.Relation rr, InterpretationNode o, InterpretationNode.Relation or) {
        Stream<Activation> s = select(doc, n, rid, r, rr, o, or);
        return s.findFirst()
                .orElse(null);
    }


    public static Activation get(Document doc, INeuron n, NodeActivation.Key ak) {
        return get(doc, n, ak.rid, ak.range, Range.Relation.EQUALS, ak.interpretation, InterpretationNode.Relation.EQUALS);
    }


    public static Stream<Activation> select(Document doc, INeuron n, Integer rid, Range r, Range.Relation rr, InterpretationNode o, InterpretationNode.Relation or) {
        INeuron.ThreadState th = n.getThreadState(doc.threadId, false);
        if (th == null) return Stream.empty();
        return select(th, n, rid, r, rr, o, or);
    }


    public static Stream<Activation> select(INeuron.ThreadState th, INeuron n, Integer rid, Range r, Range.Relation rr, InterpretationNode o, InterpretationNode.Relation or) {
        Stream<Activation> results;
        int s = th.activations.size();

        Node node = n.node.get();
        if (s == 0) return Stream.empty();
        else if (s == 1) {
            results = th.activations
                    .values()
                    .stream();
        } else if (rid != null) {
            NodeActivation.Key bk = new NodeActivation.Key(node, Range.MIN, rid, InterpretationNode.MIN);
            NodeActivation.Key ek = new NodeActivation.Key(node, Range.MAX, rid, InterpretationNode.MAX);

            if (th.activationsRid != null) {
                results = th.activationsRid.subMap(bk, true, ek, true)
                        .values()
                        .stream();
            } else return Stream.empty();
        } else {
            if (rr == null) {
                results = th.activations.values()
                        .stream();
            } else {
                return getActivationsByRange(th, n, rid, r, rr, o, or);
            }
        }

        return results.filter(act -> act.filter(node, rid, r, rr, o, or));
    }


    public static Stream<Activation> getActivationsByRange(INeuron.ThreadState th, INeuron n, Integer rid, Range r, Range.Relation rr, InterpretationNode o, InterpretationNode.Relation or) {
        Collection<Activation> s;
        Node node = n.node.get();
        if ((rr.beginToBegin == GREATER_THAN_EQUAL || rr.beginToBegin == EQUALS) && r.begin != Integer.MIN_VALUE && r.begin <= r.end) {
            int er = (rr.endToEnd == Range.Operator.LESS_THAN_EQUAL || rr.endToEnd == Range.Operator.EQUALS) && r.end != Integer.MAX_VALUE ? r.end : Integer.MAX_VALUE;
            s = th.activations.subMap(
                    new NodeActivation.Key(node, new Range(r.begin, Integer.MIN_VALUE), null, InterpretationNode.MIN),
                    true,
                    new NodeActivation.Key(node, new Range(er, Integer.MAX_VALUE), Integer.MAX_VALUE, InterpretationNode.MAX),
                    true
            )
                    .values();
        } else if ((rr.beginToBegin == Range.Operator.LESS_THAN_EQUAL || rr.beginToBegin == Range.Operator.EQUALS) && r.begin != Integer.MIN_VALUE && r.begin <= r.end) {
            s = th.activations.descendingMap().subMap(
                    new NodeActivation.Key(node, new Range(r.begin, Integer.MAX_VALUE), null, InterpretationNode.MAX),
                    true,
                    new NodeActivation.Key(node, new Range(Integer.MIN_VALUE, Integer.MIN_VALUE), null, InterpretationNode.MIN),
                    true
            )
                    .values();
        } else {
            s = th.activations.values();
        }

        return s.stream().filter(act -> act.filter(node, rid, r, rr, o, or));
    }


    private static Collection<Activation> getActivations(INeuron n, Document doc) {
        INeuron.ThreadState th = n.getThreadState(doc.threadId, false);
        return th == null ? Collections.emptyList() : th.activations.values();
    }


    public static Collection<Activation> select(Linker.SortGroup sortGroup, Activation act) {
        switch(sortGroup) {
            case RANGE_BEGIN: {
                NodeActivation.Key bk = new NodeActivation.Key(MIN_NODE, new Range(act.key.range.begin, Integer.MIN_VALUE), Integer.MIN_VALUE, InterpretationNode.MIN);
                NodeActivation.Key ek = new NodeActivation.Key(MAX_NODE, new Range(act.key.range.begin, Integer.MAX_VALUE), Integer.MAX_VALUE, InterpretationNode.MAX);

                return act.doc.activationsByRangeBegin.subMap(bk, true, ek, true).values();
            }
            case RANGE_END: {
                NodeActivation.Key bk = new NodeActivation.Key(MIN_NODE, new Range(Integer.MIN_VALUE, act.key.range.end), Integer.MIN_VALUE, InterpretationNode.MIN);
                NodeActivation.Key ek = new NodeActivation.Key(MAX_NODE, new Range(Integer.MAX_VALUE, act.key.range.end), Integer.MAX_VALUE, InterpretationNode.MAX);

                return act.doc.activationsByRangeEnd.subMap(bk, true, ek, true).values();
            }
            case RID_ZERO: {
                NodeActivation.Key bk = new NodeActivation.Key(MIN_NODE, Range.MIN, act.key.rid, InterpretationNode.MIN);
                NodeActivation.Key ek = new NodeActivation.Key(MAX_NODE, Range.MAX, act.key.rid, InterpretationNode.MAX);

                return act.doc.activationsByRid.subMap(bk, true, ek, true).values();
            }
            default:
                throw new RuntimeException("Unsupported Sort Group.");
        }
    }
}
