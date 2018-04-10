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
import org.aika.neuron.Relation;
import org.aika.neuron.activation.Range;
import org.aika.training.PatternDiscovery.Config;

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
public class AndNode extends Node<AndNode, NodeActivation<AndNode>> {


    public SortedMap<Refinement, RefValue> parents = new TreeMap<>();

    public AndNode() {}


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


    NodeActivation<AndNode> processActivation(Document doc, Collection<NodeActivation> inputActs) {
        if(inputActs.size() != level) { // TODO
            return null;
        }

        return super.processActivation(doc, inputActs);
    }


    public void propagate(NodeActivation act) {
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
    void apply(NodeActivation<AndNode> act) {
        if (andChildren != null) {
            for (Map.Entry<Refinement, NodeActivation<?>> fme : act.inputs.entrySet()) {
                Refinement ref = fme.getKey();
                NodeActivation<?> pAct = fme.getValue();

                for (Map.Entry<Refinement, NodeActivation<?>> sme : pAct.outputs.entrySet()) {
                    Refinement secondRef = sme.getKey();
                    NodeActivation secondAct = sme.getValue();
                    if (act != secondAct) {
                        Refinement nRef = new Refinement(secondRef.rel, ref.getOffset(), secondRef.input);

                        RefValue nlp = getAndChild(nRef);
                        if (nlp != null) {
                            addNextLevelActivation(act, secondAct, nlp.child);
                        }
                    }
                }
            }
        }

        OrNode.processCandidate(this, act, false);
    }


    @Override
    public void discover(NodeActivation<AndNode> act, Config config) {
        Document doc = act.doc;
        for(NodeActivation<?> pAct : act.inputs.values()) {
            for (NodeActivation<?> secondAct : pAct.outputs.values()) {
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


    public RefValue extend(int threadId, Document doc, Refinement ref) {
        RefValue rv = getAndChild(ref);
        if(rv != null) {
            return rv;
        }

        Integer[] offsets = new Integer[level];
        for(int i = 0; i < offsets.length; i++) {
            offsets[i] = i;
        }

        SortedMap<Refinement, RefValue> parents = new TreeMap<>();

        for(Map.Entry<Refinement, RefValue> me: parents.entrySet()) {
            Refinement pRef = me.getKey();
            RefValue pRV = me.getValue();
            Node pn = pRV.parent.get(doc);

            Relation[] npRelations = new Relation[ref.relations.length - 1];
            for(int i = 0; i < ref.relations.length; i++) {
                Integer j = pRV.reverseOffsets[i];
                if(j != null) {
                    npRelations[j] = ref.relations[i];
                }
            }

            Refinement npRef = new Refinement(npRelations, ref.input);

            RefValue npRV = pn.extend(threadId, doc, npRef);

            Relation[] nRelations = new Relation[pRef.relations.length + 1];
            for(int i = 0; i < pRef.relations.length; i++) {
                int j = npRV.offsets[i];
                nRelations[j] = pRef.relations[i];
            }

            Relation rel = ref.relations[pRV.refOffset];
            if(rel != null) {
                nRelations[npRV.refOffset] = rel.invert();
            }

            Refinement nRef = new Refinement(nRelations, pRef.input);

            Integer[] nOffsets = new Integer[npRV.offsets.length + 1];
            for(int i = 0; i < pRV.reverseOffsets.length; i++) {
                Integer j = pRV.reverseOffsets[i];
                if(j != null) {
                    nOffsets[npRV.offsets[j]] = i;
                }
            }

            parents.put(nRef, new RefValue(nOffsets, offsets[pRV.refOffset], pn.provider));
        }

        rv = new RefValue(offsets, level, provider);
        parents.put(ref, rv);

        return createAndNode(provider.model, doc, parents, level + 1) ? rv : null;
    }



    static boolean createAndNode(Model m, Document doc, SortedMap<Refinement, RefValue> parents, int level) {
        if (parents != null) {
            // Locking needs to take place in a predefined order.
            TreeSet<? extends Provider<? extends Node>> parentsForLocking = new TreeSet(parents.values());
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


    public static void addNextLevelActivation(NodeActivation<AndNode> act, NodeActivation<AndNode> secondAct, Provider<AndNode> pnlp) {
        // TODO: check if the activation already exists
        Document doc = act.doc;
        AndNode nlp = pnlp.get(doc);
        if(act.repropagateV != null && act.repropagateV != nlp.markedCreated) return;

        Key ak = act.key;
        Node.addActivation(
                doc,
                new Key(
                        nlp,
                        Range.mergeRange(ak.range, secondAct.key.range),
                        Utils.nullSafeMin(ak.rid, secondAct.key.rid)
                ),
                prepareInputActs(act, secondAct)
        );
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
    protected NodeActivation<AndNode> createActivation(Document doc) {
        return new NodeActivation<>(doc.activationIdCounter++, doc, this);
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
        public static Refinement MIN = new Refinement(null, null);
        public static Refinement MAX = new Refinement(null, null);

        public Relation[] relations;
        public Provider<InputNode> input;

        private Refinement() {}


        public Refinement(Relation[] relations, Provider<InputNode> input) {
            this.relations = relations;
            this.input = input;
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for(int i = 0; i < relations.length; i++) {
                Relation rel = relations[i];
                if(rel != null) {
                    sb.append(i + ":" + rel + ", ");
                }
            }
            sb.append(input.get().logicToString());
            sb.append(")");
            return sb.toString();
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
            out.writeInt(input.id);
        }


        public void readFields(DataInput in, Model m) throws IOException {
            int l = in.readInt();
            relations = new Relation[l];
            for(int i = 0; i < l; i++) {
                if(in.readBoolean()) {
                    relations[i] = Relation.read(in, m);
                }
            }
            input = m.lookupNodeProvider(in.readInt());
        }


        public static Refinement read(DataInput in, Model m) throws IOException {
            Refinement k = new Refinement();
            k.readFields(in, m);
            return k;
        }


        @Override
        public int compareTo(Refinement ref) {
            if(this == MIN || ref == MAX) return -1;
            if(this == MAX || ref == MIN) return 1;

            int r = input.compareTo(ref.input);
            if(r != 0) return r;

            r = Integer.compare(relations.length, ref.relations.length);
            if(r != 0) return r;

            for(int i = 0; i < relations.length; i++) {
                Relation ra = relations[i];
                Relation rb = ref.relations[i];

                if(ra == null && rb != null) return -1;
                if(ra != null && rb == null) return 1;

                r = ra.compareTo(rb);
                if(r != 0) return r;
            }
            return 0;
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

}
