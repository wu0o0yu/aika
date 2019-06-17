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
package network.aika.lattice;


import network.aika.*;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.activation.Position;
import network.aika.Document;
import network.aika.neuron.relation.Relation;
import network.aika.lattice.OrNode.OrActivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static network.aika.neuron.Synapse.OUTPUT;


/**
 * While several neurons might share a the same input-node or and-node, there is always a always a one-to-one relation
 * between or-nodes and neurons. The only exceptions are the input neurons which have a one-to-one relation with the
 * input-node. The or-nodes form a disjunction of one or more input-nodes or and-nodes.
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node<OrNode, OrActivation> {

    private static final Logger log = LoggerFactory.getLogger(OrNode.class);

    TreeSet<OrEntry> andParents = new TreeSet<>();

    private Neuron outputNeuron = null;


    public OrNode() {}


    public OrNode(Model m) {
        super(m, -1); // Or-node activations always need to be processed first!
    }


    @Override
    public AndNode.RefValue expand(int threadId, Document doc, AndNode.Refinement ref) {
        throw new UnsupportedOperationException();
    }


    protected void addActivation(OrEntry oe, NodeActivation inputAct) {
        Link ol = new Link(oe, inputAct);

        Document doc = inputAct.getDocument();
        INeuron n = outputNeuron.get(doc);

        Activation act = lookupActivation(ol, l -> {
            Synapse s = l.getSynapse();
            if(!s.isIdentity()) return true;

            Integer i = oe.revSynapseIds.get(s.getId());
            Activation iAct = doc.getLinker().computeInputActivation(s, inputAct.getInputActivation(i));
            return i != null && l.getInput() == iAct;
        });

        if(act == null) {
            OrActivation orAct = new OrActivation(doc, this);
            register(orAct);

            act = new Activation(doc, n, getSlots(oe, inputAct));

            act.setInputNodeActivation(orAct);
            orAct.setOutputAct(act);
        }


        OrActivation orAct = act.getInputNodeActivation();
        propagate(orAct);
        orAct.link(ol);

        ol.linkOutputActivation(act);
    }


    private Activation lookupActivation(Link ol, Predicate<Activation.Link> filter) {
        for(Activation.Link l: ol.getInputLinks(outputNeuron)) {
            Synapse syn = l.getSynapse();
            Map<Integer, Relation> rels = syn.getRelations();
            for(Map.Entry<Integer, Relation> me: rels.entrySet()) {
                Integer relSynId = me.getKey();
                Relation rel = me.getValue();

                Activation existingAct = null;
                if(relSynId != OUTPUT) {
                    Synapse s = outputNeuron.getSynapseById(relSynId);
                    if (s != null) {
                        existingAct = rel
                                .invert()
                                .getActivations(s.getInput().get(), l.getInput())
                                .flatMap(act -> act.getOutputLinksBySynapse(s))
                                .map(rl -> rl.getOutput())
                                .findFirst()
                                .orElse(null);
                    }
                } else {
                    existingAct = rel
                            .invert()
                            .getActivations(outputNeuron.get(), l.getInput())
                            .findFirst()
                            .orElse(null);
                }

                if(existingAct != null && existingAct.match(filter)) {
                    return existingAct;
                }
            }
        }

        return null;
    }


    private SortedMap<Integer, Position> getSlots(OrEntry oe, NodeActivation inputAct) {
        SortedMap<Integer, Position> slots = new TreeMap<>();
        for(int i = 0; i < oe.synapseIds.length; i++) {
            int synapseId = oe.synapseIds[i];

            Synapse s = outputNeuron.getSynapseById(synapseId);
            for(Map.Entry<Integer, Relation> me: s.getRelations().entrySet()) {
                Relation rel = me.getValue();
                if(me.getKey() == Synapse.OUTPUT) {
                    Activation iAct = inputAct.getInputActivation(i);
                    rel.mapSlots(slots, iAct);
                }
            }
        }
        return slots;
    }


    @Override
    protected void propagate(OrActivation act) {
        act.getDocument().getUpperBoundQueue().add(act.getOutputAct());
    }


    @Override
    public void cleanup() {

    }


    @Override
    public void reprocessInputs(Document doc) {
        for (OrEntry oe : andParents) {
            Node<?, NodeActivation<?>> pn = oe.parent.get();
            for (NodeActivation act : pn.getActivations(doc)) {
                act.repropagateV = markedCreated;
                act.getNode().propagate(act);
            }
        }
    }


    void addInput(int[] synapseIds, int threadId, Node in, boolean andMode) {
        in.changeNumberOfNeuronRefs(threadId, provider.getModel().visitedCounter.addAndGet(1), 1);

        OrEntry oe = new OrEntry(synapseIds, in.getProvider(), provider);
        in.addOrChild(oe);
        in.setModified();

        if(andMode) {
            lock.acquireWriteLock();
            setModified();
            andParents.add(oe);
            lock.releaseWriteLock();
        }
    }


    void remove(int threadId) {
        outputNeuron.get().remove();

        super.remove();

        try {
            lock.acquireReadLock();
            removeParents(threadId);
        } finally {
            lock.releaseReadLock();
        }
    }


    void removeParents(int threadId) {
        for (OrEntry oe : andParents) {
            Node pn = oe.parent.get();
            pn.changeNumberOfNeuronRefs(threadId, provider.getModel().visitedCounter.addAndGet(1), -1);
            pn.removeOrChild(oe);
            pn.setModified();
        }
        andParents.clear();
    }


    @Override
    protected void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        throw new UnsupportedOperationException();
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OR[");
        boolean first = true;
        int i = 0;
        for(OrEntry oe : andParents) {
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

        out.writeInt(outputNeuron.getId());

        out.writeInt(andParents.size());
        for(OrEntry oe: andParents) {
            oe.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        outputNeuron = m.lookupNeuron(in.readInt());

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            andParents.add(OrEntry.read(in, m));
        }
    }


    public String getNeuronLabel() {
        String l = outputNeuron.getLabel();
        return l != null ? l : "";
    }


    public void setOutputNeuron(Neuron n) {
        outputNeuron = n;
    }

    public Neuron getOutputNeuron() {
        return outputNeuron;
    }


    static class OrEntry implements Comparable<OrEntry>, Writable {
        int[] synapseIds;
        TreeMap<Integer, Integer> revSynapseIds = new TreeMap<>();
        Provider<? extends Node> parent;
        Provider<OrNode> child;

        private OrEntry() {}

        public OrEntry(int[] synapseIds, Provider<? extends Node> parent, Provider<OrNode> child) {
            this.synapseIds = synapseIds;
            for(int ofs = 0; ofs < synapseIds.length; ofs++) {
                revSynapseIds.put(synapseIds[ofs], ofs);
            }

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
            out.writeInt(parent.getId());
            out.writeInt(child.getId());
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
                    revSynapseIds.put(ofs, i);
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
        private Map<Integer, Link> orInputs = new TreeMap<>();
        private Activation outputAct;

        public OrActivation(Document doc, OrNode node) {
            super(doc, node);
        }

        public Activation getOutputAct() {
            return outputAct;
        }

        public void setOutputAct(Activation outputAct) {
            this.outputAct = outputAct;
        }

        @Override
        public Activation getInputActivation(int i) {
            throw new UnsupportedOperationException();
        }

        public void link(Link l) {
            l.setOutput(this);
            orInputs.put(l.input.id, l);
            l.input.outputsToOrNode.put(id, l);
        }
    }


    protected static class Link {
        public OrEntry oe;

        private NodeActivation<?> input;
        private OrActivation output;

        public Link(OrEntry oe, NodeActivation<?> input) {
            this.oe = oe;
            this.input = input;
        }


        public int size() {
            return oe.synapseIds.length;
        }

        public int get(int i) {
            return oe.synapseIds[i];
        }


        void linkOutputActivation(Activation act) {
            Linker l = act.getDocument().getLinker();
            for (int i = 0; i < size(); i++) {
                int synId = get(i);
                Synapse s = act.getSynapseById(synId);
                if(s != null) {
                    Activation iAct = input.getInputActivation(i);
                    l.link(s, iAct, act);
                }
            }
            l.process();
        }


        Collection<Activation.Link> getInputLinks(Neuron n) {
            List<Activation.Link> inputActs = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                int synId = get(i);
                Synapse s = n.getSynapseById(synId);
                Activation iAct = input.getInputActivation(i);
                inputActs.add(new Activation.Link(s, iAct, null));
            }
            return inputActs;
        }

        public void setOutput(OrActivation output) {
            this.output = output;
        }
    }
}
