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


import network.aika.Document;
import network.aika.Model;
import network.aika.Provider;
import network.aika.Writable;
import network.aika.neuron.Relation;
import network.aika.neuron.activation.Activation;
import network.aika.training.PatternDiscovery;
import network.aika.*;
import network.aika.lattice.InputNode.InputActivation;
import network.aika.lattice.AndNode.AndActivation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * The {@code InputNode} and the {@code AndNode} classes together form a pattern lattice, containing all
 * possible substructures of any given conjunction. For example if we have the conjunction ABCD where A, B, C, D are
 * the inputs, then the pattern lattice will contain the nodes ABCD, ABC, ABD, ACD, BCD, AB, AC, AD, BC, BD, CD,
 * A, B, C, D. The pattern lattice is organized in layers, where each layer only contains conjunctions/patterns of the
 * same size. These layers are connected through refinements. For example the and-node
 * ABD on layer 3 is connected to the and-node ABCD on layer 4 via the refinement C.
 *
 * @author Lukas Molzberger
 */
public class AndNode extends Node<AndNode, AndActivation> {


    public SortedMap<Refinement, RefValue> parents;

    public AndNode() {
        parents = new TreeMap<>();
    }


    public AndNode(Model m, int level, SortedMap<Refinement, RefValue> parents) {
        super(m, level);
        this.parents = parents;
    }


    private void init() {
        for(Map.Entry<Refinement, RefValue> me: parents.entrySet()) {
            Refinement ref = me.getKey();
            RefValue rv = me.getValue();
            rv.child = provider;
            Node pn = rv.parent.get();

            pn.addAndChild(ref, rv);
            pn.setModified();
        }
    }


    public void propagate(AndActivation act) {
        apply(act);
    }


    @Override
    public void cleanup() {
        if(!isRemoved && !isRequired()) {
            remove();

            for(RefValue p: parents.values()) {
                p.parent.get().cleanup();
            }
        }
    }


    @Override
    void apply(AndActivation act) {
        if (andChildren != null) {
            TreeMap<Refinement, AndActivation> results = null;
            for (Link fl : act.inputs) {
                InputActivation refAct = fl.refAct;
                Refinement ref = fl.ref;
                RefValue rv = fl.rv;
                NodeActivation<?> pAct = fl.input;

                for (Link sl : pAct.outputsToAndNode.values()) {
                    InputActivation secondRefAct = sl.refAct;
                    Refinement secondRef = sl.ref;
                    RefValue secondRv = sl.rv;
                    NodeActivation secondAct = sl.output;
                    if (act != secondAct) {
                        Relation[] relations = new Relation[secondRef.relations.length() + 1];
                        for(int i = 0; i < secondRef.relations.length(); i++) {
                            relations[rv.offsets[i]] = secondRef.relations.get(i);
                        }

                        lock.acquireReadLock();
                        for(Map.Entry<Refinement, RefValue> me: andChildren.subMap(
                                new Refinement(RelationsMap.MIN, secondRef.input),
                                new Refinement(RelationsMap.MAX, secondRef.input)).entrySet()) {
                            Refinement nRef = me.getKey();
                            RefValue nRv = me.getValue();
                            if(nRef.contains(secondRef, rv)) {
                                if(results == null) {
                                    results = new TreeMap<>();
                                }

                                AndActivation nln = results.get(nRef);
                                if(nln == null) {
                                    nln = new AndActivation(act.doc.activationIdCounter++, act.doc, nRv.child.get(act.doc));
                                    nln.link(nRef, nRv, secondRefAct, act);
                                    results.put(nRef, nln);
                                }

                                for(Map.Entry<Refinement, RefValue> mea: nln.node.parents.entrySet()) {
                                    Refinement secondNRef = mea.getKey();
                                    RefValue secondNRv = mea.getValue();
                                    if(secondNRv.parent.get(act.doc) == secondAct.node && secondNRef.contains(ref, secondRv)) {
                                        nln.link(secondNRef, secondNRv, refAct, secondAct);
                                        break;
                                    }
                                }
                            }
                        }
                        lock.releaseReadLock();
                    }
                }
            }
            if(results != null) {
                for(AndActivation nlAct: results.values()) {
                    nlAct.node.addActivation(nlAct);
                }
            }
        }

        OrNode.processCandidate(this, act, false);
    }



    @Override
    public void discover(AndActivation act, PatternDiscovery.Config config) {
        Document doc = act.doc;
        for(Link fl : act.inputs) {
            for (Link sl : fl.input.outputsToAndNode.values()) {
                AndActivation secondAct = sl.output;
                if (secondAct.node instanceof AndNode) {
                    if (act != secondAct) {
                        Refinement nRef = config.refinementFactory.create(act, secondAct);

                        AndNode nln = extend(doc.threadId, doc, nRef).child.get();
                        if (nln != null) {
                            nln.isDiscovered = true;
                        }
                    }
                }
            }
        }
    }


    public RefValue extend(int threadId, Document doc, Refinement firstRef) {
        RefValue firstRV = getAndChild(firstRef);
        if(firstRV != null) {
            return firstRV;
        }

        int firstRefOffset = level;
        Integer[] firstOffsets = new Integer[level];
        for(int i = 0; i < firstOffsets.length; i++) {
            firstOffsets[i] = i;
        }

        SortedMap<Refinement, RefValue> nextLevelParents = new TreeMap<>();

        for(Map.Entry<Refinement, RefValue> me: parents.entrySet()) {
            Refinement firstParentRef = me.getKey();
            RefValue firstParentRV = me.getValue();
            Node parentNode = firstParentRV.parent.get(doc);

            Relation[] secondParentRelations = new Relation[firstRef.relations.length() - 1];
            for(int i = 0; i < firstRef.relations.length(); i++) {
                Integer j = firstParentRV.reverseOffsets[i];
                if(j != null) {
                    secondParentRelations[j] = firstRef.relations.get(i);
                }
            }

            Refinement secondParentRef = new Refinement(new RelationsMap(secondParentRelations), firstRef.input);

            RefValue secondParentRV = parentNode.extend(threadId, doc, secondParentRef);

            if(secondParentRV == null) {
                continue;
            }

            Relation[] secondRelations = new Relation[firstParentRef.relations.length() + 1];
            for(int i = 0; i < firstParentRef.relations.length(); i++) {
                int j = secondParentRV.offsets[i];
                secondRelations[j] = firstParentRef.relations.get(i);
            }

            Relation rel = firstRef.relations.get(firstParentRV.refOffset);
            if(rel != null) {
                secondRelations[secondParentRV.refOffset] = rel.invert();
            }

            Refinement secondRef = new Refinement(new RelationsMap(secondRelations), firstParentRef.input);

            Integer[] secondOffsets = new Integer[secondParentRV.offsets.length + 1];
            for(int i = 0; i < firstParentRV.reverseOffsets.length; i++) {
                Integer j = firstParentRV.reverseOffsets[i];
                if(j != null) {
                    secondOffsets[secondParentRV.offsets[j]] = i;
                }
            }
            secondOffsets[secondParentRV.refOffset] = firstRefOffset;

            nextLevelParents.put(secondRef, new RefValue(secondOffsets, firstOffsets[firstParentRV.refOffset], secondParentRV.child));
        }

        firstRV = new RefValue(firstOffsets, firstRefOffset, provider);
        nextLevelParents.put(firstRef, firstRV);

        return createAndNode(provider.model, doc, nextLevelParents, level + 1) ? firstRV : null;
    }



    static boolean createAndNode(Model m, Document doc, SortedMap<Refinement, RefValue> parents, int level) {
        if (parents != null) {
            // Locking needs to take place in a predefined order.
            TreeSet<Provider<? extends Node>> parentsForLocking = new TreeSet();
            for(RefValue rv: parents.values()) {
                parentsForLocking.add(rv.parent);
            }

            for (Provider<? extends Node> pn : parentsForLocking) {
                pn.get().lock.acquireWriteLock();
            }
            try {
                AndNode nln = new AndNode(m, level, parents);

                nln.init();
                nln.postCreate(doc);
            } finally {
                for (Provider<? extends Node> pn : parentsForLocking) {
                    pn.get().lock.releaseWriteLock();
                }
            }
        }

        return true;
    }


    @Override
    public void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        super.changeNumberOfNeuronRefs(threadId, v, d);

        parents.values().forEach(rv -> rv.parent.get().changeNumberOfNeuronRefs(threadId, v, d));
    }


    public static Collection<NodeActivation<?>> prepareInputActs(NodeActivation<?> firstAct, NodeActivation<?> secondAct) {
        List<NodeActivation<?>> inputActs = new ArrayList<>(2);
        inputActs.add(firstAct);
        inputActs.add(secondAct);
        return inputActs;
    }


    @Override
    public void reprocessInputs(Document doc) {
        for(RefValue pp: parents.values()) {
            Node<?, NodeActivation<?>> pn = pp.parent.get();
            for(NodeActivation act : pn.getActivations(doc)) {
                act.repropagateV = markedCreated;
                act.node.propagate(act);
            }
        }
    }


    @Override
    public void remove() {
        super.remove();

        for(Map.Entry<Refinement, RefValue> me: parents.entrySet()) {
            Node pn = me.getValue().parent.get();
            pn.lock.acquireWriteLock();
            pn.removeAndChild(me.getKey());
            pn.setModified();
            pn.lock.releaseWriteLock();
        }
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AND[");
        boolean first = true;
        for(Refinement ref: parents.keySet()) {
            if(!first) {
                sb.append(",");
            }
            first = false;
            sb.append(ref);
        }
        sb.append("]");
        return sb.toString();
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        out.writeChar('A');
        super.write(out);

        out.writeInt(parents.size());
        for(Map.Entry<Refinement, RefValue> me: parents.entrySet()) {
            me.getKey().write(out);
            me.getValue().write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            Refinement ref = Refinement.read(in, m);
            RefValue rv = RefValue.read(in, m);
            parents.put(ref, rv);
        }
    }


    /**
     *
     */
    public static class Refinement implements Comparable<Refinement>, Writable {

        public RelationsMap relations;
        public Provider<InputNode> input;

        private Refinement() {}


        public Refinement(RelationsMap relations, Provider<InputNode> input) {
            this.relations = relations;
            this.input = input;
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(relations);
            sb.append(input.get().logicToString());
            sb.append(")");
            return sb.toString();
        }


        public void write(DataOutput out) throws IOException {
            relations.write(out);
            out.writeInt(input.id);
        }


        public void readFields(DataInput in, Model m) throws IOException {
            relations = RelationsMap.read(in, m);
            input = m.lookupNodeProvider(in.readInt());
        }


        public static Refinement read(DataInput in, Model m) throws IOException {
            Refinement k = new Refinement();
            k.readFields(in, m);
            return k;
        }


        @Override
        public int compareTo(Refinement ref) {
            int r = input.compareTo(ref.input);
            if(r != 0) return r;

            return relations.compareTo(ref.relations);
        }

        public boolean contains(Refinement ref, RefValue rv) {
            for(int i = 0; i < ref.relations.length(); i++) {
                Relation ra = ref.relations.get(i);
                Relation rb = relations.get(rv.offsets[i]);

                if((ra == null && rb != null) || (ra != null && rb == null)) return false;

                if(ra != null && rb != null && ra.compareTo(rb) != 0) {
                    return false;
                }
            }

            return true;
        }
    }


    public static class RelationsMap implements Comparable<RelationsMap>, Writable {

        public static final RelationsMap MIN = new RelationsMap();
        public static final RelationsMap MAX = new RelationsMap();

        public Relation[] relations;


        public RelationsMap() {}


        public RelationsMap(Relation[] relations) {
            this.relations = relations;
        }


        public void write(DataOutput out) throws IOException {
            out.writeInt(relations.length);
            for(int i = 0; i < relations.length; i++) {
                Relation rel = relations[i];
                out.writeBoolean(rel != null);
                if(rel != null) {
                    rel.write(out);
                }
            }
        }


        public void readFields(DataInput in, Model m) throws IOException {
            int l = in.readInt();
            relations = new Relation[l];
            for(int i = 0; i < l; i++) {
                if(in.readBoolean()) {
                    relations[i] = Relation.read(in, m);
                }
            }
        }


        public static RelationsMap read(DataInput in, Model m) throws IOException {
            RelationsMap k = new RelationsMap();
            k.readFields(in, m);
            return k;
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < relations.length; i++) {
                Relation rel = relations[i];
                if(rel != null) {
                    sb.append(i + ":" + rel + ", ");
                }
            }
            return sb.toString();
        }


        @Override
        public int compareTo(RelationsMap rm) {
            if (this == MIN) return -1;
            if (rm == MIN) return 1;
            if (this == MAX) return 1;
            if (rm == MAX) return -1;

            int r = Integer.compare(relations.length, rm.relations.length);
            if(r != 0) return r;

            for(int i = 0; i < relations.length; i++) {
                Relation ra = relations[i];
                Relation rb = rm.relations[i];

                if(ra == null && rb == null) continue;
                if(ra == null && rb != null) return -1;
                if(ra != null && rb == null) return 1;

                r = ra.compareTo(rb);
                if(r != 0) return r;
            }
            return 0;
        }

        public int length() {
            return relations.length;
        }

        public Relation get(int i) {
            return relations[i];
        }
    }


    public static class RefValue implements Writable {
        public Integer[] offsets;  // input offsets -> output offsets
        public Integer[] reverseOffsets;  // output offsets -> input offsets
        public int refOffset;
        public Provider<? extends Node> parent;
        public Provider<AndNode> child;

        private RefValue() {}

        public RefValue(Integer[] offsets, int refOffset, Provider<? extends Node> parent) {
            this.offsets = offsets;
            reverseOffsets = new Integer[offsets.length + 1];
            for(int i = 0; i < offsets.length; i++) {
                reverseOffsets[offsets[i]] = i;
            }

            this.refOffset = refOffset;
            this.parent = parent;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(offsets.length);
            for(int i = 0; i < offsets.length; i++) {
                Integer ofs = offsets[i];
                out.writeBoolean(ofs != null);
                out.writeInt(ofs);
            }
            out.writeInt(refOffset);
            out.writeInt(parent.id);
            out.writeInt(child.id);
        }

        public static RefValue read(DataInput in, Model m)  throws IOException {
            RefValue rv = new RefValue();
            rv.readFields(in, m);
            return rv;
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            int l = in.readInt();
            offsets = new Integer[l];
            reverseOffsets = new Integer[l + 1];
            for(int i = 0; i < l; i++) {
                if(in.readBoolean()) {
                    Integer ofs = in.readInt();
                    offsets[i] = ofs;
                    reverseOffsets[ofs] = i;
                }
            }
            refOffset = in.readInt();
            parent = m.lookupNodeProvider(in.readInt());
            child = m.lookupNodeProvider(in.readInt());
        }
    }


    public static class AndActivation extends NodeActivation<AndNode> {

        public Link[] inputs;

        public AndActivation(int id, Document doc, AndNode node) {
            super(id, doc, node);
            inputs = new Link[node.level];
        }

        public void link(Refinement ref, RefValue rv, InputActivation refAct, NodeActivation<?> input) {
            Link l = new Link(ref, rv, refAct, input, this);
            inputs[rv.refOffset] = l;
            input.outputsToAndNode.put(id, l);
        }

        public Activation getInputActivation(int i) {
            Link l = inputs[i];
            return l.refAct.input.input;
        }
    }


    public static class Link {
        Refinement ref;
        RefValue rv;

        NodeActivation<?> input;
        InputActivation refAct;
        AndActivation output;

        public Link(Refinement ref, RefValue rv, InputActivation refAct, NodeActivation<?> input, AndActivation output) {
            this.ref = ref;
            this.rv = rv;
            this.refAct = refAct;
            this.input = input;
            this.output = output;
        }
    }
}
