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


import org.aika.*;
import org.aika.lattice.NodeActivation.Key;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.EQUALS;


/**
 * While several neurons might share a the same input-node or and-node, there is always a always a one-to-one relation
 * between or-nodes and neurons. The only exceptions are the input neurons which have a one-to-one relation with the
 * input-node. The or-nodes form a disjunction of one or more input-nodes or and-nodes.
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node<OrNode, Activation> {

    // Hack: Integer.MIN_VALUE represents the null key
    public TreeMap<Integer, TreeSet<Provider<Node>>> parents = new TreeMap<>();
    public TreeMap<Integer, TreeSet<Provider<Node>>> allParents = new TreeMap<>();

    public Neuron neuron = null;
    public Node requiredNode;

    public OrNode() {}


    public OrNode(Model m) {
        super(m, -1); // Or-node activations always need to be processed first!

        m.stat.nodes++;
        m.stat.orNodes++;

        endRequired = true;
        ridRequired = true;
    }


    @Override
    public void computeNullHyp(Model m) {

    }


    @Override
    public boolean isExpandable(boolean checkFrequency) {
        return false;
    }


    @Override
    public boolean isAllowedOption(int threadId, InterprNode n, NodeActivation act, long v) {
        return false;
    }


    @Override
    protected Activation createActivation(Document doc, NodeActivation.Key ak, boolean isTrainingAct) {
        Activation act = new Activation(doc.activationIdCounter++, doc, ak);
        ak.o.act = act;
        act.isTrainingAct = isTrainingAct;
        ThreadState<OrNode, Activation> th = getThreadState(doc.threadId, false);
        if(th == null || th.activations.isEmpty()) {
            doc.activatedNeurons.add(neuron.get());
        }
        return act;
    }


    @Override
    public void deleteActivation(Document doc, Activation act) {
        ThreadState th = getThreadState(doc.threadId, false);
        if(th == null || th.activations.isEmpty()) {
            doc.activatedNeurons.remove(neuron.get());
        }
    }


    private void retrieveInputs(Document doc, Range inputR, Integer rid, List<NodeActivation<?>> inputs, Integer pRidOffset, TreeSet<Provider<Node>> parents) {
        // Optimization the number of parents can get very large, thus we need to avoid iterating over all of them.
        if(parents.size() > 10) {
            retrieveInputs(doc, null, inputR, rid, inputs, pRidOffset, parents);
        } else {
            for(Provider<Node> pn: parents) {
                retrieveInputs(doc, pn.get(), inputR, rid, inputs, pRidOffset, parents);
            }
        }
    }


    private void retrieveInputs(Document doc, Node<?, NodeActivation<?>> n, Range inputR, Integer rid, List<NodeActivation<?>> inputs, Integer pRidOffset, TreeSet<Provider<Node>> parents) {
        Stream<NodeActivation> s = n != null ?
                NodeActivation.select(doc, n, Utils.nullSafeAdd(rid, true, pRidOffset, false), inputR, EQUALS, EQUALS, null, null) :
                NodeActivation.select(doc, Utils.nullSafeAdd(rid, true, pRidOffset, false), inputR, EQUALS, EQUALS, null, null);
        for(NodeActivation iAct: s.collect(Collectors.toList())) {
            if(!iAct.isRemoved && parents.contains(iAct.key.n.provider) && !checkSelfReferencing(doc, iAct)) {
                inputs.add(iAct);
            }
        }
    }


    Activation processAddedActivation(Document doc, Key<OrNode> ak, Collection<NodeActivation> inputActs, boolean isTrainingAct) {
        Activation act = super.processAddedActivation(doc, ak, inputActs, isTrainingAct);
        if(act != null) {
            neuron.get().linkNeuronRelations(doc, act);
        }
        return act;
    }


    void processRemovedActivation(Document doc, Activation act, Collection<NodeActivation> inputActs) {
        super.processRemovedActivation(doc, act, inputActs);

        if(act.isRemoved) {
            neuron.get().unlinkNeuronRelations(doc, act);
        }
    }


    public void addActivation(Document doc, Integer ridOffset, NodeActivation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        Key ak = inputAct.key;
        Range r = ak.r;
        Integer rid = Utils.nullSafeSub(ak.rid, true, ridOffset, false);

        List<NodeActivation<?>> inputs = new ArrayList<>();

        for (Map.Entry<Integer, TreeSet<Provider<Node>>> me : parents.entrySet()) {
            retrieveInputs(doc, r, rid, inputs, me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, me.getValue());
        }

        if(inputs.isEmpty()) return;

        InterprNode no = lookupOrOption(doc, r, true);

        for(NodeActivation iAct: inputs) {
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


    public void removeActivation(Document doc, Integer ridOffset, NodeActivation<?> inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        for(NodeActivation oAct: inputAct.outputs.values()) {
            if(oAct.key.n == this && !oAct.isRemoved && oAct.inputs.size() <= 1) {
                removeActivationAndPropagate(doc, oAct, oAct.inputs.values());
            }
        }
    }


    private boolean checkSelfReferencing(Document doc, NodeActivation inputAct) {
        InterprNode o = lookupOrOption(doc, inputAct.key.r, false);
        if(o == null) return false;
        return inputAct.key.o.contains(o, true);
    }


    public void propagateAddedActivation(Document doc, Activation act, InterprNode removedConflict) {
        if(removedConflict == null) {
            neuron.get().propagateAddedActivation(doc, act);
        }
    }


    public void propagateRemovedActivation(Document doc, NodeActivation act) {
        neuron.get().propagateRemovedActivation(doc, act);
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, INeuron n) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void cleanup(Model m) {

    }


    @Override
    boolean hasSupport(Activation act) {
        for(NodeActivation iAct: act.inputs.values()) {
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
    public void discover(Document doc, NodeActivation act) {
    }


    public static void processCandidate(Document doc, Node<?, ? extends NodeActivation<?>> parentNode, NodeActivation inputAct, boolean train) {
        Key ak = inputAct.key;
        parentNode.lock.acquireReadLock();
        if(parentNode.orChildren != null) {
            for (OrEntry oe : parentNode.orChildren) {
                if (!ak.o.isConflicting(doc.visitedCounter++)) {
                    oe.node.get().addActivation(doc, oe.ridOffset, inputAct);
                }
            }
        }
        parentNode.lock.releaseReadLock();
    }


    // TODO: RID
    public InterprNode lookupOrOption(Document doc, Range r, boolean create) {
        NodeActivation act = NodeActivation.select(doc, this, null, r, EQUALS, EQUALS, null, null)
                .findFirst()
                .orElse(null);

        if(act != null) {
            return act.key.o;
        }

        ThreadState<OrNode, Activation> th = getThreadState(doc.threadId, false);
        if(th != null) {
            for (Key<OrNode> ak : th.added.keySet()) {
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


    public void addInput(Integer ridOffset, int threadId, Node in, boolean all) {
        in.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, 1);
        in.lock.acquireWriteLock();
        in.addOrChild(new OrEntry(ridOffset, provider), all);
        in.provider.setModified();
        in.lock.releaseWriteLock();

        lock.acquireWriteLock();
        provider.setModified();
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeMap<Integer, TreeSet<Provider<Node>>> p = all ? allParents : parents;

        TreeSet<Provider<Node>> pn = p.get(key);
        if(pn == null) {
            pn = new TreeSet();
            p.put(key, pn);
        }
        pn.add(in.provider);
        lock.releaseWriteLock();
    }


    public boolean hasParent(Integer ridOffset, Node in, boolean all) {
        lock.acquireReadLock();
        TreeMap<Integer, TreeSet<Provider<Node>>> p = all ? allParents : parents;

        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeSet<Provider<Node>> pn = p.get(key);
        boolean result = pn != null && pn.contains(in.provider);

        lock.releaseReadLock();

        return result;
    }


    public void removeInput(Integer ridOffset, int threadId, Node in, boolean all) {
        in.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, -1);
        in.removeOrChild(new OrEntry(ridOffset, provider), all);
        in.provider.setModified();
        lock.acquireWriteLock();
        provider.setModified();
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeMap<Integer, TreeSet<Provider<Node>>> p = all ? allParents : parents;
        TreeSet<Provider<Node>> pn = p.get(key);
        if(pn != null) {
            pn.remove(in.provider);
            if(pn.isEmpty() && ridOffset != null) {
                p.remove(key);
            }
        }
        lock.releaseWriteLock();
    }



    void remove(Model m, int threadId) {
        neuron.get().remove();

        super.remove(m);

        lock.acquireReadLock();
        removeParents(threadId, true);
        removeParents(threadId, false);
        lock.releaseReadLock();
    }


    public void removeParents(int threadId, boolean all) {
        for(Map.Entry<Integer, TreeSet<Provider<Node>>> me: (all ? allParents : parents).entrySet()) {
            for(Provider<Node> p: me.getValue()) {
                Node pn = p.get();
                pn.changeNumberOfNeuronRefs(threadId, Node.visitedCounter++, -1);
                pn.removeOrChild(new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, provider), all);
                pn.provider.setModified();
            }
        }
        (all ? allParents : parents).clear();
    }


    public void register(Activation act, Document doc) {
        super.register(act, doc);
        Key ak = act.key;

        if(ak.o.neuronActivations == null) {
            ak.o.neuronActivations = new TreeSet<>();
        }
        ak.o.neuronActivations.add(act);

        neuron.get().lastUsedDocumentId = doc.id;
    }


    public void unregister(Activation act, Document doc) {
        Key ak = act.key;

        super.unregister(act, doc);

        ak.o.neuronActivations.remove(act);
    }


    @Override
    boolean contains(Refinement ref) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        throw new UnsupportedOperationException();
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OR[");
        boolean first = true;
        int i = 0;
        for(Map.Entry<Integer, TreeSet<Provider<Node>>> me: parents.entrySet()) {
            for (Provider<Node> pn : me.getValue()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(me.getKey() != Integer.MIN_VALUE ? me.getKey() : "X");
                sb.append(":");
                sb.append(pn.get().logicToString());
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


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        out.writeUTF("O");
        super.write(out);

        out.writeInt(neuron.id);

        out.writeInt(parents.size());
        for(Map.Entry<Integer, TreeSet<Provider<Node>>> me: parents.entrySet()) {
            out.writeInt(me.getKey());
            out.writeInt(me.getValue().size());
            for(Provider<Node> pn: me.getValue()) {
                out.writeInt(pn.id);
            }
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        neuron = m.lookupNeuron(in.readInt());

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            TreeSet<Provider<Node>> ridParents = new TreeSet<>();
            Integer ridOffset = in.readInt();
            parents.put(ridOffset, ridParents);

            int sa = in.readInt();
            for(int j = 0; j < sa; j++) {
                ridParents.add(m.lookupNodeProvider(in.readInt()));
            }
        }
    }


    public String getNeuronLabel() {
        String l = neuron.get().label;
        return l != null ? l : "";
    }


    static class OrEntry implements Comparable<OrEntry>, Writable {
        public Integer ridOffset;
        public Provider<OrNode> node;


        private OrEntry() {}


        public OrEntry(Integer ridOffset, Provider<OrNode> node) {
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
        public void readFields(DataInput in, Model m) throws IOException {
            if(in.readBoolean()) {
                ridOffset = in.readInt();
            }
            node = m.lookupNodeProvider(in.readInt());
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
