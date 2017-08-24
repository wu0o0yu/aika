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


import org.aika.Activation;
import org.aika.Activation.Key;
import org.aika.Model;
import org.aika.Utils;
import org.aika.Writable;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Neuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.aika.corpus.Range.Operator.EQUALS;


/**
 * While several neurons might share a the same input-node or and-node, there is always a always a one-to-one relation
 * between or-nodes and neurons. The only exceptions are the input neurons which have a one-to-one relation with the
 * input-node. The or-nodes form a disjunction of one or more input-nodes or and-nodes.
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node {

    // Hack: Integer.MIN_VALUE represents the null key
    public TreeMap<Integer, TreeSet<Node>> parents = new TreeMap<>();


    public OrNode() {}


    public OrNode(Model m, int threadId) {
        super(m, threadId, -1); // Or-node activations always need to be processed first!

        m.stat.nodes++;
        m.stat.orNodes++;

        endRequired = true;
        ridRequired = true;
    }


    @Override
    public void computeNullHyp(Model m) {

    }


    @Override
    boolean isExpandable(boolean checkFrequency) {
        return false;
    }


    @Override
    public boolean isAllowedOption(int threadId, InterprNode n, Activation act, long v) {
        return false;
    }


    @Override
    public void initActivation(Document doc, Activation act) {
        ThreadState th = getThreadState(doc.threadId, false);
        if(th == null || th.activations.isEmpty()) {
            doc.activatedNeurons.add(neuron);
        }
    }


    @Override
    public void deleteActivation(Document doc, Activation act) {
        ThreadState th = getThreadState(doc.threadId, false);
        if(th == null || th.activations.isEmpty()) {
            doc.activatedNeurons.remove(neuron);
        }
    }


    private void retrieveInputs(Document doc, Range inputR, Integer rid, List<Activation> inputs, Integer pRidOffset, TreeSet<Node> parents) {
        // Optimization the number of parents can get very large, thus we need to avoid iterating over all of them.
        if(parents.size() > 10) {
            retrieveInputs(doc, null, inputR, rid, inputs, pRidOffset, parents);
        } else {
            for(Node pn: parents) {
                retrieveInputs(doc, pn, inputR, rid, inputs, pRidOffset, parents);
            }
        }
    }


    private void retrieveInputs(Document doc, Node n, Range inputR, Integer rid, List<Activation> inputs, Integer pRidOffset, TreeSet<Node> parents) {
        for(Activation iAct: Activation.select(doc, n, Utils.nullSafeAdd(rid, true, pRidOffset, false), inputR, EQUALS, EQUALS, null, null)
                .collect(Collectors.toList())) {
            if(!iAct.isRemoved && parents.contains(iAct.key.n) && !checkSelfReferencing(doc, iAct)) {
                inputs.add(iAct);
            }
        }
    }


    public void addActivation(Document doc, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        Key ak = inputAct.key;
        Range r = ak.r;
        Integer rid = Utils.nullSafeSub(ak.rid, true, ridOffset, false);

        List<Activation> inputs = new ArrayList<>();

        for (Map.Entry<Integer, TreeSet<Node>> me : parents.entrySet()) {
            retrieveInputs(doc, r, rid, inputs, me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, me.getValue());
        }

        if(inputs.isEmpty()) return;

        InterprNode no = lookupOrOption(doc, r, true);

        for(Activation iAct: inputs) {
            no.addOrOption(iAct, iAct.key.o);
        }

        Key nak = new Key(
                this,
                r,
                rid,
                no
        );

        addActivationAndPropagate(
                doc,
                nak,
                inputs
        );
    }


    public void removeActivation(Document doc, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        for(Activation oAct: inputAct.outputs.values()) {
            if(oAct.key.n == this && !oAct.isRemoved && oAct.inputs.size() <= 1) {
                removeActivationAndPropagate(doc, oAct, oAct.inputs.values());
            }
        }
    }


    private boolean checkSelfReferencing(Document doc, Activation inputAct) {
        InterprNode o = lookupOrOption(doc, inputAct.key.r, false);
        if(o == null) return false;
        return inputAct.key.o.contains(o, true);
    }


    public void propagateAddedActivation(Document doc, Activation act, InterprNode removedConflict) {
        if(removedConflict == null) {
            neuron.propagateAddedActivation(doc, act);
        }
    }


    public void propagateRemovedActivation(Document doc, Activation act) {
        neuron.propagateRemovedActivation(doc, act);
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, Neuron n) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void cleanup(Model m, int threadId) {

    }


    @Override
    boolean hasSupport(Activation act) {
        for(Activation iAct: act.inputs.values()) {
            if(!iAct.isRemoved) return true;
        }

        return false;
    }


    @Override
    public void apply(Document doc, Activation act, InterprNode conflict) {
        if(conflict == null) {
            OrNode.processCandidate(doc, this, act, false);
        }
    }


    @Override
    public void discover(Document doc, Activation act) {
    }


    public static void processCandidate(Document doc, Node parentNode, Activation inputAct, boolean train) {
        Key ak = inputAct.key;
        parentNode.lock.acquireReadLock();
        if(parentNode.orChildren != null) {
            for (OrEntry oe : parentNode.orChildren) {
                if (!ak.o.isConflicting(doc.visitedCounter++)) {
                    ((OrNode) oe.node).addActivation(doc, oe.ridOffset, inputAct);
                }
            }
        }
        parentNode.lock.releaseReadLock();
    }


    // TODO: RID
    public InterprNode lookupOrOption(Document doc, Range r, boolean create) {
        Activation act = Activation.select(doc, this, null, r, EQUALS, EQUALS, null, null)
                .findFirst()
                .orElse(null);

        if(act != null) {
            return act.key.o;
        }

        ThreadState th = getThreadState(doc.threadId, false);
        if(th != null) {
            for (Key ak : th.added.keySet()) {
                if (Range.compare(ak.r, r) == 0) {
                    return ak.o;
                }
            }
        }
        return create ? InterprNode.addPrimitive(doc) : null;
    }


    @Override
    Set<Refinement> collectNodeAndRefinements(Refinement newRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        throw new UnsupportedOperationException();
    }


    void addInput(int threadId, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, 1);
        in.addOrChild(threadId, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(threadId);
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeSet<Node> pn = parents.get(key);
        if(pn == null) {
            pn = new TreeSet();
            parents.put(key, pn);
        }
        pn.add(in);
        lock.releaseWriteLock();
    }


    void removeInput(int threadId, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, -1);
        in.removeOrChild(threadId, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(threadId);
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeSet<Node> pn = parents.get(key);
        if(pn != null) {
            pn.remove(in);
            if(pn.isEmpty() && ridOffset != null) {
                parents.remove(key);
            }
        }
        lock.releaseWriteLock();
    }


    void removeAllInputs(int threadId) {
        lock.acquireWriteLock(threadId);
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, -1);
                pn.removeOrChild(threadId, new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, this));
            }
        }
        parents.clear();
        lock.releaseWriteLock();
    }


    void remove(Model m, int threadId) {
        super.remove(m, threadId);

        lock.acquireReadLock();
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.removeOrChild(threadId, new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, this));
            }
        }
        lock.releaseReadLock();
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OR[");
        boolean first = true;
        int i = 0;
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for (Node pn : me.getValue()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(me.getKey() != Integer.MIN_VALUE ? me.getKey() : "X");
                sb.append(":");
                sb.append(pn.logicToString());
                if (i > 2) {
                    sb.append(",...");
                    break;
                }

                i++;
            }
        }

        sb.append("]");
        return sb.toString();
    }


    public void suspend(Model m) {
        super.suspend(m);

        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            Integer ridOffset = me.getKey() != Integer.MIN_VALUE ? me.getKey() : null;
            TreeSet<Node> pNodes = me.getValue();

            for(Node pn: pNodes) {
                pn.removeOrChild(m.defaultThreadId, new OrEntry(ridOffset, this));
                pn.suspendedOrChildren.add(id);
            }
        }
    }


    protected void reactivateIntern(Model m) {

    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF("O");
        super.write(out);

        out.writeInt(parents.size());
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            out.writeInt(me.getKey());
            out.writeInt(me.getValue().size());
            for(Node pn: me.getValue()) {
                out.writeInt(pn.id);
            }
        }
    }


    @Override
    public boolean readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            TreeSet<Node> ridParents = new TreeSet<>();
            Integer ridOffset = in.readInt();
            parents.put(ridOffset, ridParents);

            int sa = in.readInt();
            for(int j = 0; j < sa; j++) {
                Node pn = m.initialNodes.get(in.readInt());
                pn.addOrChild(m.defaultThreadId, new OrEntry(ridOffset, this));
                ridParents.add(pn);
            }
        }
        return true;
    }


    static class OrEntry implements Comparable<OrEntry>, Writable {
        public Integer ridOffset;
        public Node node;


        private OrEntry() {}


        public OrEntry(Integer ridOffset, Node node) {
            this.ridOffset = ridOffset;
            this.node = node;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(ridOffset != null);
            if(ridOffset != null) {
                out.writeInt(ridOffset);
            }
            out.writeInt(node.id);
        }


        @Override
        public boolean readFields(DataInput in, Model m) throws IOException {
            if(in.readBoolean()) {
                ridOffset = in.readInt();
            }
            node = m.initialNodes.get(in.readInt());
            return true;
        }


        public static OrEntry read(DataInput in, Model m) throws IOException {
            OrEntry n = new OrEntry();
            n.readFields(in, m);
            return n;
        }


        @Override
        public int compareTo(OrEntry on) {
            int r = Utils.compareInteger(ridOffset, on.ridOffset);
            if(r != 0) return r;
            return node.compareTo(on.node);
        }
    }

}
