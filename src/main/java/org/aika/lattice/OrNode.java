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
import org.aika.neuron.Synapse;
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

    public TreeSet<OrEntry> parents = new TreeSet<>();

    public Neuron neuron = null;

    public OrNode() {}


    public OrNode(Model m) {
        super(m, -1); // Or-node activations always need to be processed first!
    }


    @Override
    public AndNode.RefValue extend(int threadId, Document doc, AndNode.Refinement ref) {
        throw new UnsupportedOperationException();
    }


    public void addInputActivation(OrEntry oe, NodeActivation inputAct) {
        Document doc = inputAct.doc;

        int begin = Integer.MIN_VALUE;
        int end = Integer.MAX_VALUE;

        for(int i = 0; i < oe.synapseIds.length; i++) {
            int synapseId = oe.synapseIds[i];

            Synapse s = neuron.getSynapseById(synapseId);
            s.rangeOutput.begin
        }

        Range r = new Range(begin, end);

        if(neuron.get(doc).outputText != null) {
            begin = r.begin != Integer.MIN_VALUE ? r.begin : 0;
            end = r.end != Integer.MAX_VALUE ? r.end : begin + neuron.get(doc).outputText.length();
            r = new Range(begin, end);
        }

        if(r.begin == Integer.MIN_VALUE || r.end == Integer.MAX_VALUE) return;

        Activation act = neuron.get(doc).getThreadState(doc.threadId, true).activations.get(r);

        if(act == null) {
            act = new Activation(doc.activationIdCounter++, doc, this);
            addActivation(act);
        }

        act.link(, inputAct);
    }


    public void propagate(Activation act) {
        act.doc.ubQueue.add(act);
    }


    void processActivation(Activation act) {
        super.processActivation(act);
        neuron.get(act.doc).register(act);
    }


    @Override
    public void cleanup() {

    }


    @Override
    public void apply(Activation act) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void discover(Activation act, Config config) {
        throw new UnsupportedOperationException();
    }


    public static void processCandidate(Node<?, ? extends NodeActivation<?>> parentNode, NodeActivation inputAct, boolean train) {
        Document doc = inputAct.doc;
        try {
            parentNode.lock.acquireReadLock();
            if (parentNode.orChildren != null) {
                for (OrEntry oe : parentNode.orChildren) {
                    oe.child.get(doc).addInputActivation(oe, inputAct);
                }
            }
        } finally {
            parentNode.lock.releaseReadLock();
        }
    }


    @Override
    public void reprocessInputs(Document doc) {
        for (OrEntry oe : parents) {
            Node<?, NodeActivation<?>> pn = oe.parent.get();
            for (NodeActivation act : pn.getActivations(doc)) {
                act.repropagateV = markedCreated;
                act.node.propagate(act);
            }
        }
    }


    public void addInput(int[] synapseIds, int threadId, Node in) {
        in.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), 1);

        OrEntry oe = new OrEntry(synapseIds, in.provider, provider);
        in.addOrChild(oe);
        in.setModified();

        lock.acquireWriteLock();
        setModified();
        parents.add(oe);
        lock.releaseWriteLock();
    }


    void remove(int threadId) {
        neuron.get().remove();

        super.remove();

        try {
            lock.acquireReadLock();
            removeParents(threadId);
        } finally {
            lock.releaseReadLock();
        }
    }


    public void removeParents(int threadId) {
        for (OrEntry oe : parents) {
            Node pn = oe.parent.get();
            pn.changeNumberOfNeuronRefs(threadId, provider.model.visitedCounter.addAndGet(1), -1);
            pn.removeOrChild(oe);
            pn.setModified();
        }
        parents.clear();
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
        for(OrEntry oe : parents) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(oe.parent.get().logicToString());
            if (i > 2) {
                sb.append(",...");
                break;
            }

            i++;
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


    public static class OrEntry implements Comparable<OrEntry>, Writable {
        public int[] synapseIds;
        public Provider<? extends Node> parent;
        public Provider<OrNode> child;

        private OrEntry() {}

        public OrEntry(int[] synapseIds, Provider<? extends Node> parent, Provider<OrNode> child) {
            this.synapseIds = synapseIds;
            this.parent = parent;
            this.child = child;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(synapseIds.length);
            for(int i = 0; i < synapseIds.length; i++) {
                Integer ofs = synapseIds[i];
                out.writeBoolean(ofs != null);
                out.writeInt(ofs);
            }
            out.writeInt(parent.id);
            out.writeInt(child.id);
        }

        public static OrEntry read(DataInput in, Model m)  throws IOException {
            OrEntry rv = new OrEntry();
            rv.readFields(in, m);
            return rv;
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            int l = in.readInt();
            synapseIds = new int[l];
            for(int i = 0; i < l; i++) {
                if(in.readBoolean()) {
                    Integer ofs = in.readInt();
                    synapseIds[i] = ofs;
                }
            }
            parent = m.lookupNodeProvider(in.readInt());
            child = m.lookupNodeProvider(in.readInt());
        }


        @Override
        public int compareTo(OrEntry oe) {
            int r = child.compareTo(oe.child);
            if(r != 0) return r;

            r = parent.compareTo(oe.parent);
            if(r != 0) return r;

            r = Integer.compare(synapseIds.length, oe.synapseIds.length);
            if(r != 0) return r;

            for(int i = 0; i < synapseIds.length; i++) {
                r = Integer.compare(synapseIds[i], oe.synapseIds[i]);
                if(r != 0) return r;
            }
            return 0;
        }
    }


    public static class OrActivation extends NodeActivation<OrNode> {
        public Map<Integer, Link> inputs = new TreeMap<>();

        public OrActivation(int id, Document doc, OrNode node) {
            super(id, doc, node);
        }

        public void link(OrEntry oe, NodeActivation<?> input) {
            inputs.put(input.id, new Link(oe, input, this));
        }
    }


    public static class Link {
        OrEntry oe;

        NodeActivation<?> input;
        OrActivation output;

        public Link(OrEntry oe, NodeActivation<?> input, OrActivation output) {
            this.oe = oe;
            this.input = input;
            this.output = output;
        }
    }
}
