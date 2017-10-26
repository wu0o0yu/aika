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

    public int id;

    public final Key<T> key;

    public Document doc;

    public boolean isRemoved;
    public int removedId;
    public long visitedNeuronTrain = -1;

    public static int removedIdCounter = 1;
    public static long visitedCounter = 1;

    public boolean isTrainingAct;

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


    public void unlink(Collection<NodeActivation> inputActs) {
        for (NodeActivation iAct : inputActs) {
            inputs.remove(iAct.key);
            iAct.outputs.remove(key);
        }
    }


    public void unlink() {
        for (NodeActivation act : inputs.values()) {
            act.outputs.remove(key);
        }
        for (NodeActivation act : outputs.values()) {
            act.inputs.remove(key);
        }
    }


    public static <T extends Node, A extends NodeActivation<T>> A get(Document doc, T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, InterprNode.Relation or) {
        Stream<A> s = select(doc, n, rid, r, begin, end, o, or);
        return s.findFirst()
                .orElse(null);
    }


    public static <T extends Node, A extends NodeActivation<T>> A get(Document doc, T n, Key ak) {
        return get(doc, n, ak.rid, ak.r, Operator.EQUALS, Operator.EQUALS, ak.o, InterprNode.Relation.EQUALS);
    }


    public static NodeActivation getNextSignal(Node n, Document doc, Integer from, Integer rid, InterprNode o, boolean dir, boolean inv) {
        ThreadState th = n.getThreadState(doc.threadId, false);
        if(th == null) return null;

        Key bk = new Key(null, new Range(from, dir ? Integer.MIN_VALUE : Integer.MAX_VALUE).invert(inv), rid, o);
        NavigableMap<Key, NodeActivation> tmp = (inv ? th.activationsEnd : th.activations);
        tmp = dir ? tmp.descendingMap() : tmp;
        for(NodeActivation act: tmp.tailMap(bk, false).values()) {
            if(act.filter(n, rid, null, null, null, o, InterprNode.Relation.CONTAINED_IN)) {
                return act;
            }
        }
        return null;
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
        Stream<A> s;
        if((begin == GREATER_THAN || begin == EQUALS || end == FIRST) && r.begin != null) {
            int er = (end == Operator.LESS_THAN || end == Operator.EQUALS || end == FIRST) && r.end != null ? r.end : Integer.MAX_VALUE;
            s = th.activations.subMap(
                    new NodeActivation.Key(n, new Range(r.begin, null), null, InterprNode.MIN),
                    true,
                    new NodeActivation.Key(n, new Range(er, Integer.MAX_VALUE), Integer.MAX_VALUE, InterprNode.MAX),
                    true
            )
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
        } else if((begin == Operator.LESS_THAN || begin == Operator.EQUALS) && r.begin != null) {
            s = th.activations.descendingMap().subMap(
                    new NodeActivation.Key(n, new Range(r.begin, Integer.MAX_VALUE), null, InterprNode.MAX),
                    true,
                    new NodeActivation.Key(n, new Range(null, null), null, InterprNode.MIN),
                    true
            )
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
        }  else if(end == LAST) {
            s = th.activationsEnd.tailMap(
                    new NodeActivation.Key(n, new Range(null, r.begin), null, InterprNode.MIN),
                    true
            )
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
        } else if(begin == LAST || begin == FIRST || end == FIRST) {
            // TODO
            throw new RuntimeException("Not implemented yet!");
        } else {
            s = th.activations.values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
        }

        return s;
    }


    private static <T extends Node, A extends NodeActivation<T>> Stream<A> getActivationsStream(T n, Document doc) {
        ThreadState<T, A> th = n.getThreadState(doc.threadId, false);
        return th == null ? Stream.empty() : th.activations.values().stream();
    }


    public <T extends Node> boolean filter(T n, Integer rid, Range r, Operator begin, Operator end, InterprNode o, InterprNode.Relation or) {
        return (n == null || key.n == n) &&
                (rid == null || (key.rid != null && key.rid.intValue() == rid.intValue())) &&
                (r == null || ((begin == null || begin.compare(key.r.begin, key.r.end, r.begin, r.end)) && (end == null || end.compare(key.r.end, key.r.begin, r.end, r.begin)))) &&
                (o == null || or.compare(key.o, o));
    }


    public String toString(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ACT ");
        sb.append(",(");
        sb.append(key.r);
        sb.append("),");
        sb.append(doc.getContent().substring(Math.max(0, key.r.begin - 3), Math.min(doc.length(), key.r.end + 3)));
        sb.append(",");
        sb.append(key.n);
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
        public final T n;
        public final Range r;
        public final Integer rid;
        public final InterprNode o;

        private int refCount = 0;


        public Key(T n, Range r, Integer rid, InterprNode o) {
            this.n = n;
            this.r = r;
            this.rid = rid;
            this.o = o;
            countRef();
            if(o != null) {
                o.countRef();
            }
        }


        public void countRef() {
            refCount++;
        }


        public void releaseRef() {
            assert refCount > 0;
            refCount--;
            if(refCount == 0) {
                o.releaseRef();
            }
        }


        @Override
        public int compareTo(Key k) {
            int x = n.compareTo(k.n);
            if(x != 0) return x;
            x = Range.compare(r, k.r, false);
            if(x != 0) return x;
            x = Utils.compareInteger(rid, k.rid);
            if(x != 0) return x;
            return o.compareTo(k.o);
        }


        public String toString() {
            return (n != null ? n.getNeuronLabel() : "") + r + " " + rid + " " + o;
        }
    }


}
