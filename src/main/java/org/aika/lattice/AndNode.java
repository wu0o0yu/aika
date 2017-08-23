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
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.InputNode.SynapseKey;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.apache.commons.math3.distribution.BinomialDistribution;

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
public class AndNode extends Node {

    private static double SIGNIFICANCE_THRESHOLD = 0.98;
    public static int MAX_POS_NODES = 4;
    public static int MAX_RID_RANGE = 5;

    SortedMap<Refinement, Node> parents = new TreeMap<>();


    public volatile int numberOfPositionsNotify;
    private volatile int frequencyNotify;

    private double weight = -1;


    public AndNode() {}


    public AndNode(Model m, int threadId, int level, SortedMap<Refinement, Node> parents) {
        super(m, threadId, level);
        this.parents = parents;

        m.stat.nodes++;
        m.stat.nodesPerLevel[level]++;

        ridRequired = false;

        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            Refinement ref = me.getKey();
            Node pn = me.getValue();

            pn.addAndChild(ref, this);

            if(ref.rid != null) ridRequired = true;
        }

        endRequired = false;
    }


    @Override
    boolean isAllowedOption(int threadId, InterprNode n, Activation act, long v) {
        ThreadState th = getThreadState(threadId, true);
        if(th.visitedAllowedOption == v) return false;
        th.visitedAllowedOption = v;

        for(Activation pAct: act.inputs.values()) {
            if(pAct.key.n.isAllowedOption(threadId, n, pAct, v)) return true;
        }
        return false;
    }


    Range preProcessAddedActivation(Document doc, Key ak, Collection<Activation> inputActs) {
        for(Activation iAct: inputActs) {
            if(iAct.isRemoved) return null;
        }
        return ak.r;
    }


    void addActivation(Document doc, Key ak, Collection<Activation> directInputActs) {
        Node.addActivationAndPropagate(doc, ak, directInputActs);
    }


    static void removeActivation(Document doc, Activation iAct) {
        for(Activation act: iAct.outputs.values()) {
            if(act.key.n instanceof AndNode) {
                Node.removeActivationAndPropagate(doc, act, Collections.singleton(iAct));
            }
        }
    }


    public void propagateAddedActivation(Document doc, Activation act, InterprNode removedConflict) {
        apply(doc, act, removedConflict);
    }


    public void propagateRemovedActivation(Document doc, Activation act) {
        removeFromNextLevel(doc, act);
    }


    @Override
    boolean hasSupport(Activation act) {
        int expected = parents.size();

        int support = 0;
        Activation lastAct = null;
        for(Activation iAct: act.inputs.values()) {
            if(!iAct.isRemoved && (lastAct == null || lastAct.key.n != iAct.key.n)) {
                support++;
            }
            lastAct = iAct;
        }
        assert support <= expected;
        return support == expected;
    }


    @Override
    public void computeNullHyp(Model m) {
        double avgSize = sizeSum / instanceSum;
        double n = (double) (m.numberOfPositions - nOffset) / avgSize;

        double nullHyp = 0.0;
        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            Node pn = me.getValue();
            InputNode in = me.getKey().input;
            double inputNA = (double) (m.numberOfPositions - in.nOffset) / avgSize;
            double inputNB = (double) (m.numberOfPositions - pn.nOffset) / avgSize;

            double nh = Math.min(1.0, in.frequency / inputNA) * Math.min(1.0, Math.max(pn.frequency, pn.nullHypFreq) / inputNB);
            nullHyp = Math.max(nullHyp, nh);
        }

        nullHypFreq = nullHyp * n;
    }


    public void updateWeight(Document doc, long v) {
        ThreadState th = getThreadState(doc.threadId, true);
        Model m = doc.m;
        if(isBlocked ||
                (m.numberOfPositions - nOffset) == 0 ||
                frequency < Node.minFrequency ||
                th.visitedComputeWeight == v ||
                (numberOfPositionsNotify > m.numberOfPositions && frequencyNotify > frequency && Math.abs(nullHypFreq - oldNullHypFreq) < 0.01)
                ) {
            return;
        }

        th.visitedComputeWeight = v;

        double avgSize = sizeSum / instanceSum;
        double n = (double) (m.numberOfPositions - nOffset) / avgSize;

        doc.m.numberOfPositionsQueue.remove(this);
        numberOfPositionsNotify = computeNotify(n) + m.numberOfPositions;
        doc.m.numberOfPositionsQueue.add(this);

        BinomialDistribution binDist = new BinomialDistribution(null, (int)Math.round(n), nullHypFreq / n);

        weight = binDist.cumulativeProbability(frequency - 1);

        frequencyNotify = computeNotify(frequency) + frequency;
        oldNullHypFreq = nullHypFreq;

        if(weight >= SIGNIFICANCE_THRESHOLD) {
//            checkSignificantPattern(t);
        }
    }


    public int computeNotify(double x) {
        return 1 + (int) Math.floor(Math.pow(x, 1.15) - x);
    }


    @Override
    public void cleanup(Model m, int threadId) {
        if(!isRemoved && !isFrequent() && !isRequired()) {
            remove(m, threadId);

            for(Node p: parents.values()) {
                p.cleanup(m, threadId);
            }
        }
    }


    @Override
    void apply(Document doc, Activation act, InterprNode removedConflict) {

        // Check if the activation has been deleted in the meantime.
        if(act.isRemoved) {
            return;
        }

        for(Activation pAct: act.inputs.values()) {
            Node pn = pAct.key.n;
            pn.lock.acquireReadLock();
            Refinement ref = pn.reverseAndChildren.get(new ReverseAndRefinement(act.key.n, act.key.rid, pAct.key.rid));
            if(ref != null) {
                for (Activation secondAct : pAct.outputs.values()) {
                    if (act != secondAct && !secondAct.isRemoved) {
                        Refinement secondRef = pn.reverseAndChildren.get(new ReverseAndRefinement(secondAct.key.n, secondAct.key.rid, pAct.key.rid));
                        if (secondRef != null) {
                            Refinement nRef = new Refinement(secondRef.rid, ref.getOffset(), secondRef.input);

                            AndNode nlp = getAndChild(nRef);
                            if (nlp != null) {
                                addNextLevelActivation(doc, act, secondAct, nlp, removedConflict);
                            }
                        }
                    }
                }
            }
            pn.lock.releaseReadLock();
        }

        if(removedConflict == null) {
            OrNode.processCandidate(doc, this, act, false);
        }
    }


    @Override
    public void discover(Document doc, Activation act) {
        if(!isExpandable(true)) return;

        for(Activation pAct: act.inputs.values()) {
            Node pn = pAct.key.n;
            pn.lock.acquireReadLock();
            Refinement ref = pn.reverseAndChildren.get(new ReverseAndRefinement(act.key.n, act.key.rid, pAct.key.rid));
            for(Activation secondAct: pAct.outputs.values()) {
                if(secondAct.key.n instanceof AndNode) {
                    Node secondNode = secondAct.key.n;
                    Integer ridDelta = Utils.nullSafeSub(act.key.rid, false, secondAct.key.rid, false);
                    if (act != secondAct &&
                            !secondNode.isBlocked &&
                            secondNode.isFrequent() &&
                            (ridDelta == null || ridDelta < MAX_RID_RANGE)
                            ) {
                        Refinement secondRef = pn.reverseAndChildren.get(new ReverseAndRefinement(secondAct.key.n, secondAct.key.rid, pAct.key.rid));
                        Refinement nRef = new Refinement(secondRef.rid, ref.getOffset(), secondRef.input);

                        AndNode nln = createNextLevelNode(doc.m, doc.threadId, this, nRef, true);
                        if(nln != null) {
                            doc.addedNodes.add(nln);
                        }
                    }
                }
            }
            pn.lock.releaseReadLock();
        }
    }


    boolean isExpandable(boolean checkFrequency) {
        if(checkFrequency && !isFrequent()) return false;

        int numPosNodes = 0;
        for(Refinement ref: parents.keySet()) {
            if(!ref.input.key.isNeg) numPosNodes++;
        }

        return numPosNodes < MAX_POS_NODES;
    }


    private static boolean checkRidRange(SortedMap<Refinement, Node> parents) {
        int maxRid = 0;
        for(Refinement ref: parents.keySet()) {
            if(ref.rid != null) {
                maxRid = Math.max(maxRid, ref.rid);
            }
        }
        return maxRid < MAX_RID_RANGE;
    }


    static AndNode createNextLevelNode(Model m, int threadId, Node n, Refinement ref, boolean discoverPatterns) {
        AndNode nln = n.getAndChild(ref);
        if(nln != null) {
            return discoverPatterns ? null : nln;
        }

        if(n instanceof InputNode) {
            if(n == ref.input && ref.rid == 0) return null;
        } else {
            AndNode an = (AndNode) n;

            // Check if this refinement is already present in this and-node.
            if(ref.rid == null || ref.rid >= 0) {
                boolean flag = false;
                an.lock.acquireReadLock();
                if(ref.rid == null || ref.rid > 0) {
                    flag = an.parents.containsKey(ref);
                } else if(ref.rid == 0) {
                    for(Refinement pRef: an.parents.keySet()) {
                        if((pRef.rid == null || pRef.rid < 0) && pRef.input == ref.input) {
                            flag = true;
                            break;
                        }
                    }
                }
                an.lock.releaseReadLock();
                if (flag) return null;
            }
        }

        SortedMap<Refinement, Node> parents = computeNextLevelParents(m, threadId, n, ref, discoverPatterns);

        if (parents != null && (!discoverPatterns || checkRidRange(parents))) {
            // Locking needs to take place in a predefined order.
            TreeSet<Node> parentsForLocking = new TreeSet<>(parents.values());
            for(Node pn: parentsForLocking) {
                pn.lock.acquireWriteLock(threadId);
            }

            if(n.andChildren == null || !n.andChildren.containsKey(ref)) {
                nln = new AndNode(m, threadId, n.level + 1, parents);
                nln.isBlocked = n.isBlocked || ref.input.isBlocked;
            }

            for(Node pn: parentsForLocking) {
                pn.lock.releaseWriteLock();
            }
        }
        return nln;
    }


    public static void addNextLevelActivation(Document doc, Activation act, Activation secondAct, AndNode nlp, InterprNode conflict) {
        // TODO: check if the activation already exists
        Key ak = act.key;
        InterprNode o = InterprNode.add(doc, true, ak.o, secondAct.key.o);
        if (o != null && (conflict == null || o.contains(conflict, false))) {
            nlp.addActivation(
                    doc,
                    new Key(
                            nlp,
                            Range.mergeRange(ak.r, secondAct.key.r),
                            Utils.nullSafeMin(ak.rid, secondAct.key.rid),
                            o
                    ),
                    prepareInputActs(act, secondAct)
            );
        }
    }


    public static Collection<Activation> prepareInputActs(Activation firstAct, Activation secondAct) {
        List<Activation> inputActs = new ArrayList<>(2);
        inputActs.add(firstAct);
        inputActs.add(secondAct);
        return inputActs;
    }


    public static SortedMap<Refinement, Node> computeNextLevelParents(Model m, int threadId, Node pa, Refinement ref, boolean discoverPatterns) {
        Collection<Refinement> refinements = pa.collectNodeAndRefinements(ref);

        long v = visitedCounter++;
        SortedMap<Refinement, Node> parents = new TreeMap<>();

        for(Refinement pRef: refinements) {
            SortedSet<Refinement> childInputs = new TreeSet<>(refinements);
            childInputs.remove(pRef);
            if(!pRef.input.computeAndParents(m, threadId, pRef.getRelativePosition(), childInputs, parents, discoverPatterns, v)) {
                return null;
            }
        }

        return parents;
    }


    @Override
    Collection<Refinement> collectNodeAndRefinements(Refinement newRef) {
        List<Refinement> inputs = new ArrayList<>(parents.size() + 1);
        inputs.add(newRef);

        int numRidRefs = 0;
        for(Refinement ref: parents.keySet()) {
            if(ref.rid != null) numRidRefs++;
        }

        for(Refinement ref: parents.keySet()) {
            if(newRef.rid != null && newRef.rid != null && (newRef.rid < 0 || numRidRefs == 1)) {
                inputs.add(new Refinement(ref.getRelativePosition(), newRef.rid, ref.input));
            } else if(ref.rid != null && newRef.rid != null && ref.getOffset() < 0) {
                inputs.add(new Refinement(0, Math.min(-ref.getOffset(), newRef.getRelativePosition()) , ref.input));
            } else {
                inputs.add(ref);
            }
        }

        return inputs;
    }


    @Override
    void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        ThreadState th = getThreadState(threadId, true);
        if(th.visitedNeuronRefsChange == v) return;
        th.visitedNeuronRefsChange = v;
        numberOfNeuronRefs += d;

        for(Node n: parents.values()) {
            n.changeNumberOfNeuronRefs(threadId, v, d);
        }
    }


    @Override
    public boolean isCovered(int threadId, Integer offset, long v) {
        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            RidVisited nv = me.getValue().getThreadState(threadId, true).lookupVisited(Utils.nullSafeSub(offset, true, me.getKey().getOffset(), false));
            if(nv.outputNode == v) return true;
        }
        return false;
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, Neuron n) {
        double sum = n.bias;
        for(Refinement ref: parents.keySet()) {
            Synapse s = ref.getSynapse(offset, n);
            sum += Math.abs(s.w);
        }
        return sum;
    }


    @Override
    public void initActivation(Document doc, Activation act) {
    }


    @Override
    public void deleteActivation(Document doc, Activation act) {
    }


    @Override
    void remove(Model m, int threadId) {
        super.remove(m, threadId);

        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            Node pn = me.getValue();
            pn.lock.acquireWriteLock(threadId);
            pn.removeAndChild(me.getKey());
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

    public String weightsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" - ");
        sb.append(" F:");
        sb.append(frequency);
        sb.append("  BW:");
        sb.append(Utils.round(weight));

        return sb.toString();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF("A");
        super.write(out);

        out.writeInt(numberOfPositionsNotify);
        out.writeInt(frequencyNotify);

        out.writeDouble(weight);

        out.writeInt(parents.size());
        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            me.getKey().write(out);
            out.writeInt(me.getValue().id);
        }
    }


    @Override
    public boolean readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        numberOfPositionsNotify = in.readInt();
        frequencyNotify = in.readInt();

        weight = in.readDouble();

        int s = in.readInt();
        Map<Refinement, Node> tmp = new TreeMap<>();
        for(int i = 0; i < s; i++) {
            Refinement ref = Refinement.read(in, m);
            Node pn = m.initialNodes.get(in.readInt());
            if(pn == null) {
                return false;
            }
            tmp.put(ref, pn);
        }
        for(Map.Entry<Refinement, Node> me: tmp.entrySet()) {
            Refinement ref = me.getKey();
            Node pn = me.getValue();
            parents.put(ref, pn);
            pn.addAndChild(ref, this);
        }
        return true;
    }


    /**
     *
     */
    static class Refinement implements Comparable<Refinement> {
        public static Refinement MIN = new Refinement(null, null);
        public static Refinement MAX = new Refinement(null, null);

        public Integer rid;
        public InputNode input;

        private Refinement() {}

        public Refinement(Integer rid, InputNode input) {
            this.rid = rid;
            this.input = input;
        }

        public Refinement(Integer rid, Integer offset, InputNode input) {
            if(offset == null && rid != null) this.rid = 0;
            else if(offset == null || rid == null) this.rid = null;
            else this.rid = rid - offset;
            this.input = input;
        }


        public Integer getOffset() {
            return rid != null ? Math.min(0, rid) : null;
        }


        public Integer getRelativePosition() {
            return rid != null ? Math.max(0, rid) : null;
        }


        public Synapse getSynapse(Integer offset, Neuron n) {
            input.lock.acquireReadLock();
            Synapse s = input.synapses != null ? input.synapses.get(new SynapseKey(Utils.nullSafeAdd(getRelativePosition(), false, offset, false), n)) : null;
            input.lock.releaseReadLock();
            return s;
        }


        public String toString() {
            return "(" + (rid != null ? rid + ":" : "") + input.logicToString() + ")";
        }


        public void write(DataOutput out) throws IOException {
            out.writeBoolean(rid != null);
            if(rid != null) out.writeInt(rid);
            out.writeInt(input.id);
        }


        public boolean readFields(DataInput in, Model m) throws IOException {
            if(in.readBoolean()) {
                rid = in.readInt();
            }
            input = (InputNode) m.initialNodes.get(in.readInt());
            return true;
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
            return Utils.compareInteger(rid, ref.rid);
        }
    }

}
