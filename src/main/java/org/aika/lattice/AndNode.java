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
import org.aika.corpus.Option;
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
 *
 * @author Lukas Molzberger
 */
public class AndNode extends Node {

    public static double SIGNIFICANCE_THRESHOLD = 0.98;
    public static int MAX_POS_NODES = 4;
    public static int MAX_RID_RANGE = 5;

    public SortedMap<Refinement, Node> parents = new TreeMap<>();


    public volatile int numberOfPositionsNotify;
    public volatile int frequencyNotify;

    public double weight = -1;


    public AndNode() {}


    public AndNode(Document doc, int level, SortedMap<Refinement, Node> parents) {
        super(doc, level);
        this.parents = parents;

        Model m = doc.m;
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
    public boolean isAllowedOption(Document doc, Option n, Activation act, long v) {
        ThreadState th = getThreadState(doc);
        if(th.visitedAllowedOption == v) return false;
        th.visitedAllowedOption = v;

        for(Activation pAct: act.inputs.values()) {
            if(pAct.key.n.isAllowedOption(doc, n, pAct, v)) return true;
        }
        return false;
    }


    protected Range preProcessAddedActivation(Document doc, Key ak, Collection<Activation> inputActs) {
        for(Activation iAct: inputActs) {
            if(iAct.isRemoved) return null;
        }
        return ak.r;
    }


    public void addActivation(Document doc, Key ak, Collection<Activation> directInputActs) {
        Node.addActivationAndPropagate(doc, ak, directInputActs);
    }


    protected static void removeActivation(Document doc, Activation iAct) {
        for(Activation act: iAct.outputs.values()) {
            if(act.key.n instanceof AndNode) {
                Node.removeActivationAndPropagate(doc, act, Collections.singleton(iAct));
            }
        }
    }


    public void propagateAddedActivation(Document doc, Activation act, Option removedConflict) {
        apply(doc, act, removedConflict);
    }


    public void propagateRemovedActivation(Document doc, Activation act) {
        removeFromNextLevel(doc, act);
    }


    @Override
    protected boolean hasSupport(Activation act) {
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
        ThreadState th = getThreadState(doc);
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



    public double computeFMeasure(Similarity sim, Neuron n) {
        double fPattern = frequency - sim.nodeFreqOffset;
        double fNeuron = n.node.frequency - sim.neuronFreqOffset;

        double recall = sim.frequency / fPattern;
        double precision = sim.frequency / fNeuron;

        if(recall > 1.0 || precision > 1.0) {
            assert false;
        }

        return (2 * precision * recall) / (precision + recall);
    }


    @Override
    public void cleanup(Document doc) {
        if(!isRemoved && !isFrequent() && !isRequired()) {
            remove(doc);

            for(Node p: parents.values()) {
                p.cleanup(doc);
            }
        }
    }


    @Override
    public void apply(Document doc, Activation act, Option removedConflict) {

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

                        createNextLevelNode(doc, this, nRef, true);
                    }
                }
            }
            pn.lock.releaseReadLock();
        }
    }


    public boolean isExpandable(boolean checkFrequency) {
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


    public static AndNode createNextLevelNode(Document doc, Node n, Refinement ref, boolean discoverPatterns) {
        AndNode nln = n.getAndChild(ref);
        if(nln != null) {
            return nln;
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
                        if(pRef.rid < 0 && pRef.input == ref.input) {
                            flag = true;
                            break;
                        }
                    }
                }
                an.lock.releaseReadLock();
                if (flag) return null;
            }
        }

        SortedMap<Refinement, Node> parents = computeNextLevelParents(doc, n, ref, discoverPatterns);

        if (parents != null && (!discoverPatterns || checkRidRange(parents))) {
            // Locking needs to take place in a predefined order.
            TreeSet<Node> parentsForLocking = new TreeSet<>(parents.values());
            for(Node pn: parentsForLocking) {
                pn.lock.acquireWriteLock(doc.threadId);
            }

            if(n.andChildren == null || !n.andChildren.containsKey(ref)) {
                nln = new AndNode(doc, n.level + 1, parents);
                nln.isBlocked = n.isBlocked || ref.input.isBlocked;
            }

            for(Node pn: parentsForLocking) {
                pn.lock.releaseWriteLock();
            }

            if(discoverPatterns) {
                doc.addedNodes.add(nln);
            }
        }
        return nln;
    }


    public static void addNextLevelActivation(Document doc, Activation act, Activation secondAct, AndNode nlp, Option conflict) {
        // TODO: check if the activation already exists
        Key ak = act.key;
        Option o = Option.add(doc, true, ak.o, secondAct.key.o);
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


    public static SortedMap<Refinement, Node> computeNextLevelParents(Document doc, Node pa, Refinement ref, boolean discoverPatterns) {
        Collection<Refinement> refinements = pa.collectNodeAndRefinements(ref);

        long v = visitedCounter++;
        SortedMap<Refinement, Node> parents = new TreeMap<>();

        for(Refinement pRef: refinements) {
            SortedSet<Refinement> childInputs = new TreeSet<>(refinements);
            childInputs.remove(pRef);
            if(!pRef.input.computeAndParents(doc, pRef.getRelativePosition(), childInputs, parents, discoverPatterns, v)) {
                return null;
            }
        }

        return parents;
    }


    @Override
    protected Collection<Refinement> collectNodeAndRefinements(Refinement newRef) {
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
    protected void changeNumberOfNeuronRefs(Document doc, long v, int d) {
        ThreadState th = getThreadState(doc);
        if(th.visitedNeuronRefsChange == v) return;
        th.visitedNeuronRefsChange = v;
        numberOfNeuronRefs += d;

        for(Node n: parents.values()) {
            n.changeNumberOfNeuronRefs(doc, v, d);
        }
    }


    @Override
    public boolean isCovered(Document doc, Integer offset, long v) {
        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            RidVisited nv = me.getValue().getThreadState(doc).lookupVisited(Utils.nullSafeSub(offset, true, me.getKey().getOffset(), false));
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
    public void remove(Document doc) {
        super.remove(doc);

        for(Map.Entry<Refinement, Node> me: parents.entrySet()) {
            Node pn = me.getValue();
            pn.lock.acquireWriteLock(doc.threadId);
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
    public void readFields(DataInput in, Document doc) throws IOException {
        super.readFields(in, doc);

        numberOfPositionsNotify = in.readInt();
        frequencyNotify = in.readInt();

        weight = in.readDouble();

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            Refinement ref = Refinement.read(in, doc);
            Node pn = doc.m.initialNodes.get(in.readInt());
            parents.put(ref, pn);
            pn.addAndChild(ref, this);
        }
    }


    public static class Refinement implements Comparable<Refinement> {
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


        public void readFields(DataInput in, Document doc) throws IOException {
            if(in.readBoolean()) {
                rid = in.readInt();
            }
            input = (InputNode) doc.m.initialNodes.get(in.readInt());
        }


        public static Refinement read(DataInput in, Document doc) throws IOException {
            Refinement k = new Refinement();
            k.readFields(in, doc);
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
