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
import org.aika.Document;
import org.aika.neuron.Neuron;
import org.aika.neuron.Relation;
import org.aika.neuron.activation.Selector;
import org.aika.training.PatternDiscovery.Config;
import org.aika.neuron.activation.Range;
import org.aika.neuron.activation.Activation;
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

    public OrNode() {}


    public OrNode(Model m) {
        super(m, -1); // Or-node activations always need to be processed first!
    }


    @Override
    public AndNode.RefValue extend(int threadId, Document doc, AndNode.Refinement ref) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Activation createActivation(Document doc) {
        return new Activation(doc.activationIdCounter++, doc, this);
    }


    public void addActivation(Document doc, Integer ridOffset, NodeActivation inputAct) {
        Key ak = inputAct.key;
        Range r = ak.range;
        Integer rid = Utils.nullSafeSub(ak.rid, true, ridOffset, false);

        if(neuron.get(doc).outputText != null) {
            int begin = r.begin != Integer.MIN_VALUE ? r.begin : 0;
            int end = r.end != Integer.MAX_VALUE ? r.end : begin + neuron.get(doc).outputText.length();
            r = new Range(begin, end);
        }

        if(r.begin == Integer.MIN_VALUE || r.end == Integer.MAX_VALUE) return;

        Activation no = lookupOrOption(doc, r, true);

        if(no == null) {
            addActivation(
                    doc,
                    new Key(
                            this,
                            r,
                            rid
                    ),
                    Collections.singleton(inputAct)
            );
        }
    }


    public void propagate(Activation act) {
        act.doc.ubQueue.add(act);
    }


    Activation processActivation(Document doc, Collection<NodeActivation> inputActs) {
        Activation act = Selector.get(doc,  neuron.get(), ak);
        if (act == null) {
            act = createActivation(doc, ak);

            register(act);

            propagate(act);
        }

        act.link(inputActs);

        neuron.get(doc).register(act);

        return act;
    }


    @Override
    public void cleanup() {

    }


    @Override
    public void apply(Activation act) {
    }


    @Override
    public void discover(Activation act, Config config) {
    }


    public static void processCandidate(Node<?, ? extends NodeActivation<?>> parentNode, NodeActivation inputAct, boolean train) {
        Document doc = inputAct.doc;
        try {
            parentNode.lock.acquireReadLock();
            if (parentNode.orChildren != null) {
                for (Refinement oe : parentNode.orChildren) {
                    oe.node.get(doc).addActivation(doc, oe.ridOffset, inputAct);
                }
            }
        } finally {
            parentNode.lock.releaseReadLock();
        }
    }


    // TODO: RID
    public Activation lookupOrOption(Document doc, Range r, boolean create) {
        Activation act = Selector.select(doc, neuron.get(), null, r, Range.Relation.EQUALS)
                .findFirst()
                .orElse(null);

        return act;
    }


    @Override
    public void reprocessInputs(Document doc) {
        for(TreeSet<Provider<Node>> ppSet: parents.values()) {
            for(Provider<Node> pp: ppSet) {
                Node<?, NodeActivation<?>> pn = pp.get();
                for (NodeActivation act : pn.getActivations(doc)) {
                    act.repropagateV = markedCreated;
                    act.node.propagate(act);
                }
            }
        }
    }


    public void addInput(Integer ridOffset, int threadId, Node in, boolean all) {
        in.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), 1);
        in.lock.acquireWriteLock();
        in.addOrChild(new Refinement(ridOffset, provider), all);
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
        in.removeOrChild(new Refinement(ridOffset, provider), all);
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

        try {
            lock.acquireReadLock();
            removeParents(threadId, true);
            removeParents(threadId, false);
        } finally {
            lock.releaseReadLock();
        }
    }


    public void removeParents(int threadId, boolean all) {
        for(Map.Entry<Integer, TreeSet<Provider<Node>>> me: (all ? allParents : parents).entrySet()) {
            for(Provider<Node> p: me.getValue()) {
                Node pn = p.get();
                pn.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), -1);
                pn.removeOrChild(new Refinement(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, provider), all);
                pn.setModified();
            }
        }
        (all ? allParents : parents).clear();
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


    static class Refinement implements Comparable<Refinement>, Writable {
        public Integer[] offsets;
        public Provider<OrNode> node;


        private Refinement() {}


        public Refinement(Integer[] offsets, Provider<OrNode> node) {
            this.offsets = offsets;
            this.node = node;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(offsets.length);
            for(int i = 0; i < offsets.length; i++) {
                Integer ofs = offsets[i];
                out.writeBoolean(ofs != null);
                if(ofs != null) {
                    out.writeInt(ofs);
                }
            }
            out.writeInt(node.id);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            int l = in.readInt();
            offsets = new Integer[l];
            for(int i = 0; i < l; i++) {
                if(in.readBoolean()) {
                    offsets[i] = in.readInt();
                }
            }

            node = m.lookupNodeProvider(in.readInt());
        }


        public static Refinement read(DataInput in, Model m) throws IOException {
            Refinement n = new Refinement();
            n.readFields(in, m);
            return n;
        }


        @Override
        public int compareTo(Refinement on) {
            int r = Integer.compare(offsets.length, on.offsets.length);
            if(r != 0) return r;

            for(int i = 0; i < offsets.length; i++) {
                r = Utils.compareInteger(offsets[i], on.offsets[i]);
                if(r != 0) return r;
            }
            return node.compareTo(on.node);
        }
    }


    public static class RefValue {

    }


    public static class OrActivation extends NodeActivation<OrNode> {

        public Map<Integer, Link> inputs = new TreeMap<>();

        public OrActivation(int id, Document doc, OrNode node) {
            super(id, doc, node);
        }
    }


    public static class Link {
        Refinement ref;
        RefValue rv;

        NodeActivation<?> input;
        Activation output;
    }
}
