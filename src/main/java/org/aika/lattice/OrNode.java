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
import org.aika.training.PatternDiscovery.Config;
import org.aika.corpus.InterpretationNode;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


/**
 * While several neurons might share a the same input-node or and-node, there is always a always a one-to-one relation
 * between or-nodes and neurons. The only exceptions are the input neurons which have a one-to-one relation with the
 * input-node. The or-nodes form a disjunction of one or more input-nodes or and-nodes.
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node<OrNode, Activation> {

    private static final Logger log = LoggerFactory.getLogger(OrNode.class);

    // Hack: Integer.MIN_VALUE represents the null key
    public TreeMap<Integer, TreeSet<Provider<Node>>> parents = new TreeMap<>();
    public TreeMap<Integer, TreeSet<Provider<Node>>> allParents = new TreeMap<>();

    public Neuron neuron = null;
    public Node requiredNode;

    public OrNode() {}


    public OrNode(Model m) {
        super(m, -1); // Or-node activations always need to be processed first!
    }


    @Override
    protected Activation createActivation(Document doc, NodeActivation.Key ak) {
        Activation act = new Activation(doc.activationIdCounter++, doc, ak);
        ak.interpretation.activation = act;

        return act;
    }


    public void addActivation(Document doc, Integer ridOffset, NodeActivation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        Key ak = inputAct.key;
        Range r = ak.range;
        Integer rid = Utils.nullSafeSub(ak.rid, true, ridOffset, false);

        InterpretationNode no = lookupOrOption(doc, r, true);

        if(neuron.get(doc).outputText != null) {
            int begin = r.begin != Integer.MIN_VALUE ? r.begin : 0;
            int end = r.end != Integer.MAX_VALUE ? r.end : begin + neuron.get(doc).outputText.length();
            r = new Range(begin, end);
        }

        if(r.begin == Integer.MIN_VALUE || r.end == Integer.MAX_VALUE) return;

        addActivationAndPropagate(
                doc,
                new Key(
                        this,
                        r,
                        rid,
                        no
                ),
                Collections.singleton(inputAct)
        );
    }


    private boolean checkSelfReferencing(Document doc, NodeActivation inputAct) {
        InterpretationNode o = lookupOrOption(doc, inputAct.key.range, false);
        if(o == null) return false;
        return inputAct.key.interpretation.contains(o, true);
    }


    public void propagateAddedActivation(Document doc, Activation act) {
        neuron.get(doc).propagateAddedActivation(doc, act);
    }


    Activation processAddedActivation(Document doc, Key<OrNode> ak, Collection<NodeActivation> inputActs) {
        if (Document.APPLY_DEBUG_OUTPUT) {
            log.info("add: " + ak + " - " + ak.node);
        }

        Activation act = Activation.get(doc,  neuron.get(), ak);
        if (act == null) {
            act = createActivation(doc, ak);

            register(act, doc);

            propagateAddedActivation(doc, act);
        }

        act.link(inputActs);

        for(NodeActivation iAct: inputActs) {
            act.key.interpretation.addOrInterpretationNode(iAct.key.interpretation);
        }

        neuron.get(doc).linkNeuronRelations(doc, act);

        return act;
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, INeuron n) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void cleanup() {

    }


    @Override
    public void apply(Document doc, Activation act) {
        OrNode.processCandidate(doc, this, act, false);
    }


    @Override
    public void discover(Document doc, NodeActivation act, Config config) {
    }


    public static void processCandidate(Document doc, Node<?, ? extends NodeActivation<?>> parentNode, NodeActivation inputAct, boolean train) {
        Key ak = inputAct.key;
        parentNode.lock.acquireReadLock();
        if(parentNode.orChildren != null) {
            for (OrEntry oe : parentNode.orChildren) {
                if (!ak.interpretation.isConflicting(doc.visitedCounter++)) {
                    oe.node.get(doc).addActivation(doc, oe.ridOffset, inputAct);
                }
            }
        }
        parentNode.lock.releaseReadLock();
    }


    // TODO: RID
    public InterpretationNode lookupOrOption(Document doc, Range r, boolean create) {
        Activation act = Activation.select(doc, neuron.get(), null, r, Range.Relation.EQUALS, null, null)
                .findFirst()
                .orElse(null);

        if(act != null) {
            return act.key.interpretation;
        }

        ThreadState<OrNode, Activation> th = getThreadState(doc.threadId, false);
        if(th != null) {
            for (Key<OrNode> ak : th.added.keySet()) {
                if (Range.compare(ak.range, r) == 0) {
                    return ak.interpretation;
                }
            }
        }
        return create ? InterpretationNode.addPrimitive(doc) : null;
    }


    @Override
    Set<Refinement> collectNodeAndRefinements(Refinement newRef) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void reprocessInputs(Document doc) {
        for(TreeSet<Provider<Node>> ppSet: parents.values()) {
            for(Provider<Node> pp: ppSet) {
                Node<?, NodeActivation<?>> pn = pp.get();
                for (NodeActivation act : pn.getActivations(doc)) {
                    act.repropagateV = markedCreated;
                    act.key.node.propagateAddedActivation(doc, act);
                }
            }
        }
    }


    public void addInput(Integer ridOffset, int threadId, Node in, boolean all) {
        in.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), 1);
        in.lock.acquireWriteLock();
        in.addOrChild(new OrEntry(ridOffset, provider), all);
        in.setModified();
        in.lock.releaseWriteLock();

        lock.acquireWriteLock();
        setModified();
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


    public void removeInput(Integer ridOffset, int threadId, Node in, boolean all) {
        in.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), -1);
        in.removeOrChild(new OrEntry(ridOffset, provider), all);
        in.setModified();
        lock.acquireWriteLock();
        setModified();
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



    void remove(int threadId) {
        neuron.get().remove();

        super.remove();

        lock.acquireReadLock();
        removeParents(threadId, true);
        removeParents(threadId, false);
        lock.releaseReadLock();
    }


    public void removeParents(int threadId, boolean all) {
        for(Map.Entry<Integer, TreeSet<Provider<Node>>> me: (all ? allParents : parents).entrySet()) {
            for(Provider<Node> p: me.getValue()) {
                Node pn = p.get();
                pn.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), -1);
                pn.removeOrChild(new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, provider), all);
                pn.setModified();
            }
        }
        (all ? allParents : parents).clear();
    }


    public void register(Activation act, Document doc) {
        super.register(act, doc);
        Key<OrNode> ak = act.key;

        INeuron.ThreadState th = ak.node.neuron.get().getThreadState(doc.threadId, true);
        if (th.activations.isEmpty()) {
            doc.activatedNeurons.add(ak.node.neuron.get());
        }
        th.activations.put(ak, act);

        TreeMap<Key, Activation> actEnd = th.activationsEnd;
        if (actEnd != null) actEnd.put(ak, act);

        TreeMap<Key, Activation> actRid = th.activationsRid;
        if (actRid != null) actRid.put(ak, act);

        if(ak.interpretation.activations == null) {
            ak.interpretation.activations = new TreeSet<>();
        }
        ak.interpretation.activations.add(act);

        if (ak.rid != null) {
            doc.activationsByRid.put(ak, act);
        }
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
        out.writeChar('O');
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
        String l = neuron.getLabel();
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
