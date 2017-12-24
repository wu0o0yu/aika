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
package org.aika.lattice;


import org.aika.Utils;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.InterprNode.Relation;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.Node.ThreadState;

import java.util.*;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.*;


public class NodeActivation<T extends Node> implements Comparable<NodeActivation> {

    public final int id;

    public final Key<T> key;

    public final Document doc;

    public long visitedProcessing;
    public long visitedNeuronTrain;

    public TreeMap<Key, NodeActivation> inputs = new TreeMap<>();
    public TreeMap<Key, NodeActivation> outputs = new TreeMap<>();


    public NodeActivation(int id, Document doc, Key<T> key) {
        this.id = id;
        this.doc = doc;
        this.key = key;
    }


    public NodeActivation(int id, Document doc, T n, Range pos, Integer rid, InterprNode o) {
        this.id = id;
        this.doc = doc;
        key = new Key<>(n, pos, rid, o);
    }


    public void link(Collection<NodeActivation> inputActs) {
        for(NodeActivation iAct: inputActs) {
            inputs.put(iAct.key, iAct);
            iAct.outputs.put(key, this);
        }
    }


    public static <T extends Node, A extends NodeActivation<T>> A get(Document doc, T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, InterprNode.Relation or) {
        Stream<A> s = select(doc, n, rid, r, begin, end, o, or);
        return s.findFirst()
                .orElse(null);
    }


    public static <T extends Node, A extends NodeActivation<T>> A get(Document doc, T n, Key ak) {
        return get(doc, n, ak.rid, ak.range, Operator.EQUALS, Operator.EQUALS, ak.interpretation, InterprNode.Relation.EQUALS);
    }


    public static Stream<NodeActivation> select(Document doc, Integer rid, Range r, Operator begin, Operator end, InterprNode o, Relation or) {
        Stream<NodeActivation> results;
        if(rid != null) {
            Key bk = new Key(Node.MIN_NODE, Range.MIN, rid, InterprNode.MIN);
            Key ek = new Key(Node.MAX_NODE, Range.MAX, rid, InterprNode.MAX);

            results = doc.activationsByRid.subMap(bk, true, ek, true)
                    .values()
                    .stream();
        } else {
            results = doc.activatedNodes
                    .stream()
                    .flatMap(node -> getActivationsStream(node, doc));
        }

        return results.filter(act -> act.filter(null, rid, r, begin, end, o, or));
    }


    public static <T extends Node, A extends NodeActivation<T>> Stream<A> select(Document doc, T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, Relation or) {
        ThreadState<T, A> th = n.getThreadState(doc.threadId, false);
        if(th == null) return Stream.empty();
        return select(th, n, rid, r, begin, end, o, or);
    }


    public static <T extends Node, A extends NodeActivation<T>> Stream<A> select(ThreadState<T, A> th, T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, Relation or) {
        Stream<A> results;
        int s = th.activations.size();

        if(s == 0) return Stream.empty();
        else if(s == 1) {
            results = th.activations
                    .values()
                    .stream();
        } else if(rid != null) {
            Key bk = new Key(n, Range.MIN, rid, InterprNode.MIN);
            Key ek = new Key(n, Range.MAX, rid, InterprNode.MAX);

            if(th.activationsRid != null) {
                results = th.activationsRid.subMap(bk, true, ek, true)
                        .values()
                        .stream();
            } else return Stream.empty();
        } else {
            if(begin == null && end == null) {
                results = th.activations.values()
                        .stream();
            } else {
                return getActivationsByRange(th, n, rid, r, begin, end, o, or);
            }
        }

        return results.filter(act -> act.filter(n, rid, r, begin, end, o, or));
    }


    public static <T extends Node, A extends NodeActivation<T>> Stream<A> getActivationsByRange(ThreadState<T, A> th, T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, InterprNode.Relation or) {
        Collection<A> s;
        if((begin == GREATER_THAN_EQUAL || begin == EQUALS) && r.begin != Integer.MIN_VALUE && r.begin <= r.end) {
            int er = (end == Operator.LESS_THAN_EQUAL || end == Operator.EQUALS) && r.end != Integer.MAX_VALUE ? r.end : Integer.MAX_VALUE;
            s = th.activations.subMap(
                    new NodeActivation.Key(n, new Range(r.begin, Integer.MIN_VALUE), null, InterprNode.MIN),
                    true,
                    new NodeActivation.Key(n, new Range(er, Integer.MAX_VALUE), Integer.MAX_VALUE, InterprNode.MAX),
                    true
            )
                    .values();
        } else if((begin == Operator.LESS_THAN_EQUAL || begin == Operator.EQUALS) && r.begin != Integer.MIN_VALUE && r.begin <= r.end) {
            s = th.activations.descendingMap().subMap(
                    new NodeActivation.Key(n, new Range(r.begin, Integer.MAX_VALUE), null, InterprNode.MAX),
                    true,
                    new NodeActivation.Key(n, new Range(Integer.MIN_VALUE, Integer.MIN_VALUE), null, InterprNode.MIN),
                    true
            )
                    .values();
        } else {
            s = th.activations.values();
        }

        return s.stream().filter(act -> act.filter(n, rid, r, begin, end, o, or));
    }


    private static <T extends Node, A extends NodeActivation<T>> Stream<A> getActivationsStream(T n, Document doc) {
        ThreadState<T, A> th = n.getThreadState(doc.threadId, false);
        return th == null ? Stream.empty() : th.activations.values().stream();
    }


    public <T extends Node> boolean filter(T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, InterprNode.Relation or) {
        return (n == null || key.node == n) &&
                (rid == null || (key.rid != null && key.rid.intValue() == rid.intValue())) &&
                (r == null || ((begin == null || begin.compare(key.range.begin, r.begin)) && (end == null || end.compare(key.range.end, r.end)))) &&
                (o == null || or.compare(key.interpretation, o));
    }


    public String toString(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ACT ");
        sb.append(",(");
        sb.append(key.range);
        sb.append("),");
        sb.append(doc.getContent().substring(Math.max(0, key.range.begin - 3), Math.min(doc.length(), key.range.end + 3)));
        sb.append(",");
        sb.append(key.node);
        sb.append(">");
        return sb.toString();
    }


    @Override
    public int compareTo(NodeActivation act) {
        return key.compareTo(act.key);
    }


    public static int compare(NodeActivation a, NodeActivation b) {
        if(a == b) return 0;
        if(a == null && b != null) return -1;
        if(a != null && b == null) return 1;
        return a.compareTo(b);
    }


    public static final class Key<T extends Node> implements Comparable<Key> {
        public final T node;
        public final Range range;
        public final Integer rid;
        public final InterprNode interpretation;


        public Key(T node, Range range, Integer rid, InterprNode interpretation) {
            this.node = node;
            this.range = range;
            this.rid = rid;
            this.interpretation = interpretation;
        }


        @Override
        public int compareTo(Key k) {
            int x = node.compareTo(k.node);
            if(x != 0) return x;
            x = Range.compare(range, k.range, false);
            if(x != 0) return x;
            x = Utils.compareInteger(rid, k.rid);
            if(x != 0) return x;
            return interpretation.compareTo(k.interpretation);
        }


        public String toString() {
            return (node != null ? node.getNeuronLabel() : "") + range + " " + rid + " " + interpretation;
        }
    }
}
