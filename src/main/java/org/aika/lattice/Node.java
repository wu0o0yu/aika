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
import org.aika.Activation.Key;
import org.aika.Activation.SynapseActivation;
import org.aika.corpus.Conflicts;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.AndNode.Refinement;
import org.aika.lattice.InputNode.SynapseKey;
import org.aika.lattice.OrNode.OrEntry;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.*;
import static org.aika.corpus.Range.Mapping.END;
import static org.aika.corpus.Range.Mapping.START;


/**
 * The {@code Node} class is the abstract class for all the boolean logic nodes underneath the neural network layer.
 * These nodes form a boolean representation for all the neurons of the neural network. Whenever changes occur to the
 * synapse weights in the neural layer, then the structure of the boolean representation needs to be adjusted. Several
 * neurons, however, might share common substructures in this boolean representation. The {@code InputNode} and
 * the {@code AndNode} classes together form a pattern lattice, containing all possible substructures of any
 * given conjunction. For example if we have the conjunction ABCD where A, B, C, D are the inputs then the
 * pattern lattice will contain the nodes ABCD, ABC, ABD, ACD, BCD, AB, AC, AD, BC, BD, CD, A, B, C, D. The class
 * {@code OrNode} is a disjunction of either input-nodes or and-nodes. The or-node is connected with one of
 * the neurons.
 *
 * <p>Each logic node has a set of activations. The activations are stored in the thread local data structure
 * {@code ThreadState}.
 *
 * @author Lukas Molzberger
 */
public abstract class Node implements Comparable<Node>, Writable {
    public static int minFrequency = 5;
    public static int MAX_RID = 20;

    public static boolean LINK_NEURON_RELATIONS_OPTIMIZATION = true;

    public static final Node MIN_NODE = new DummyNode(Integer.MIN_VALUE);
    public static final Node MAX_NODE = new DummyNode(Integer.MAX_VALUE);

    private static final Logger log = LoggerFactory.getLogger(Node.class);

    private static AtomicInteger currentNodeId = new AtomicInteger(0);
    public int id;

    TreeMap<ReverseAndRefinement, Refinement> reverseAndChildren;
    TreeMap<Refinement, AndNode> andChildren;
    TreeSet<OrEntry> orChildren;

    TreeMap<Refinement, Integer> suspendedAndChildren;
    TreeSet<Integer> suspendedOrChildren;

    public int level;

    public volatile int frequency;
    public volatile double nullHypFreq;
    public volatile double oldNullHypFreq;

    public boolean isBlocked;

    public boolean endRequired;
    public boolean ridRequired;


    public int numberOfNeuronRefs = 0;
    volatile boolean isRemoved;
    volatile int isRemovedId;
    static int isRemovedIdCounter = 0;

    public volatile boolean frequencyHasChanged = true;
    public volatile int nOffset;

    public volatile int sizeSum = 0;
    public volatile int instanceSum = 0;


    // Only childrens are locked.
    public ReadWriteLock lock = new ReadWriteLock();


    public boolean isQueued = false;
    public long queueId;

    public Neuron neuron = null;
    public Integer neuronId = null;

    public static long visitedCounter = 0;

    public ThreadState[] threads;


    /**
     * The {@code ThreadState} is a thread local data structure containing the activations of a single document for
     * a specific logic node.
     */
    public static class ThreadState {
        public long lastUsed;

        public TreeMap<Key, Activation> activations;
        public TreeMap<Key, Activation> activationsEnd;
        public TreeMap<Key, Activation> activationsRid;

        public NavigableMap<Key, Collection<Activation>> added;
        public NavigableMap<Key, RemovedEntry> removed;
        long visitedNeuronRefsChange = -1;
        public long visitedAllowedOption = -1;
        public long visitedComputeWeight = -1;

        private RidVisited nullRidVisited;
        private RidVisited[] ridVisited = new RidVisited[2 * MAX_RID];

        public ThreadState(boolean endRequired, boolean ridRequired) {
            activations = new TreeMap<>(BEGIN_COMP);
            activationsEnd = endRequired ? new TreeMap<>(END_COMP) : null;
            activationsRid = ridRequired ? new TreeMap<>(RID_COMP) : null;

            added = new TreeMap<>();
            removed = new TreeMap<>();
        }

        public RidVisited lookupVisited(Integer offset) {
            if(offset == null) {
                if(nullRidVisited == null) {
                    nullRidVisited = new RidVisited();
                }
                return nullRidVisited;
            } else {
                RidVisited v = ridVisited[offset + MAX_RID];
                if (v == null) {
                    v = new RidVisited();
                    ridVisited[offset + MAX_RID] = v;
                }
                return v;
            }
        }

    }


    /**
     * Aika extensively uses graph coloring techniques. When traversing the logic node lattice, nodes will be
     * marked in order to avoid having to visit the same node twice. To avoid having to reset each mark Aika uses the
     * counter {@code Node.visitedCounter} to set a new mark each time.
     */
    static class RidVisited {
        public long computeParents = -1;
        public long outputNode = -1;
        public long adjust = -1;
    }


    public ThreadState getThreadState(Document doc, boolean create) {
        ThreadState th = threads[doc.threadId];
        if(th == null) {
            if(!create) return null;

            th = new ThreadState(endRequired, ridRequired);
            threads[doc.threadId] = th;
        }
        th.lastUsed = doc.id;
        return th;
    }


    /**
     * Propagate an activation to the next node or the next neuron that is depending on the current node.
     *
     * @param doc
     * @param act
     * @param conflict
     */
    public abstract void propagateAddedActivation(Document doc, Activation act, InterprNode conflict);

    public abstract void propagateRemovedActivation(Document doc, Activation act);

    abstract boolean isAllowedOption(Document doc, InterprNode n, Activation act, long v);

    abstract void cleanup(Document doc);

    abstract void initActivation(Document doc, Activation act);

    abstract void deleteActivation(Document doc, Activation act);

    public abstract double computeSynapseWeightSum(Integer offset, Neuron n);

    public abstract String logicToString();

    abstract void apply(Document doc, Activation act, InterprNode conflict);

    public abstract void discover(Document doc, Activation act);

    abstract Collection<Refinement> collectNodeAndRefinements(Refinement newRef);

    abstract void changeNumberOfNeuronRefs(Document doc, long v, int d);

    abstract boolean hasSupport(Activation act);

    public abstract void computeNullHyp(Model m);

    abstract boolean isExpandable(boolean checkFrequency);


    protected Node() {}


    public Node(Model m, int threadId, int level) {
        threads = new ThreadState[m.numberOfThreads];
        id = currentNodeId.addAndGet(1);
        this.level = level;
        if(m != null) {
            m.allNodes[threadId].add(this);
            nOffset = m.numberOfPositions;
        }
    }

    public static final Comparator<Activation.Key> BEGIN_COMP = new Comparator<Key>() {
        @Override
        public int compare(Key k1, Key k2) {
            int r;
            r = Range.compare(k1.r, k2.r, false);
            if(r != 0) return r;
            r = Utils.compareInteger(k1.rid, k2.rid);
            if(r != 0) return r;
            return InterprNode.compare(k1.o, k2.o);
        }
    };


    public static final Comparator<Activation.Key> END_COMP = new Comparator<Key>() {
        @Override
        public int compare(Key k1, Key k2) {
            int r;
            r = Range.compare(k1.r, k2.r, true);
            if(r != 0) return r;
            r = Utils.compareInteger(k1.rid, k2.rid);
            if(r != 0) return r;
            return InterprNode.compare(k1.o, k2.o);
        }
    };


    public static final Comparator<Activation.Key> RID_COMP = new Comparator<Key>() {
        @Override
        public int compare(Key k1, Key k2) {
            int r;
            r = Utils.compareInteger(k1.rid, k2.rid);
            if(r != 0) return r;
            r = Range.compare(k1.r, k2.r, false);
            if(r != 0) return r;
            return InterprNode.compare(k1.o, k2.o);
        }
    };


    public void setNeuron(Neuron n) {
        neuron = n;
        neuronId = n.id;
    }


    void addOrChild(Document doc, OrEntry oe) {
        lock.acquireWriteLock(doc.threadId);
        if(orChildren == null) {
            orChildren = new TreeSet<>();
        }
        orChildren.add(oe);
        lock.releaseWriteLock();
    }


    void removeOrChild(Document doc, OrEntry oe) {
        lock.acquireWriteLock(doc.threadId);
        if(orChildren != null) {
            orChildren.remove(oe);
            if(orChildren.isEmpty()) {
                orChildren = null;
            }
        }
        lock.releaseWriteLock();
    }


    void addAndChild(Refinement ref, AndNode child) {
        if(andChildren == null) {
            andChildren = new TreeMap<>();
            reverseAndChildren = new TreeMap<>();
        }

        AndNode n = andChildren.put(ref, child);
        assert n == null;
        reverseAndChildren.put(new ReverseAndRefinement(child, ref.rid, 0), ref);
    }



    void addSuspendedAndChild(Refinement ref, Integer id) {
        if(suspendedAndChildren == null) {
            suspendedAndChildren = new TreeMap<>();
//            reverseAndChildren = new TreeMap<>();
        }

        suspendedAndChildren.put(ref, id);
//        reverseAndChildren.put(new ReverseAndRefinement(child, ref.rid, 0), ref);
    }


    void removeAndChild(Refinement ref) {
        if(andChildren != null) {
            andChildren.remove(ref);
            reverseAndChildren.remove(new ReverseAndRefinement(this, ref.rid, 0));

            if(andChildren.isEmpty()) {
                andChildren = null;
                reverseAndChildren = null;
            }
        }
    }


    public void count(Document doc) {
        ThreadState ts = getThreadState(doc, false);
        if(ts == null) return;

        for(Activation act: ts.activations.values()) {
            frequency++;
            frequencyHasChanged = true;

            sizeSum += act.key.r.end == null || act.key.r.begin == null || act.key.r.end == Integer.MAX_VALUE ? 1 : Math.max(1, act.key.r.end - act.key.r.begin);
            instanceSum++;
        }
    }


    Activation addActivationInternal(Document doc, Key ak, Collection<Activation> inputActs, boolean isTrainingAct) {
        Activation act = Activation.get(doc, this, ak);
        if(act == null) {
            act = new Activation(doc.activationIdCounter++, ak);
            act.isTrainingAct = isTrainingAct;

            if(neuron != null) {
                act.neuronInputs = new TreeSet<>(SynapseActivation.INPUT_COMP);
                act.neuronOutputs = new TreeSet<>(SynapseActivation.OUTPUT_COMP);
            }

            initActivation(doc, act);
            act.register(doc);

            act.link(inputActs);

            if(neuron != null) {
                linkNeuronRelations(doc, act);
            }

            if(!isTrainingAct) {
                propagateAddedActivation(doc, act, null);
            }
        } else {
            if(neuron != null) {
                linkNeuronRelations(doc, act);
            }
            act.link(inputActs);
        }

        return act;
    }


    boolean removeActivationInternal(Document doc, Activation act, Collection<Activation> inputActs) {
        boolean flag = false;
        if(act.isRemoved) {
            act.unregister(doc);
            deleteActivation(doc, act);

            propagateRemovedActivation(doc, act);

            act.key.releaseRef();

            if(neuron != null) {
                unlinkNeuronRelations(doc, act);
            }
            flag = true;
        }

        // TODO: check unlinkNeuronRelations symmetry
        act.unlink(inputActs);

        return flag;
    }


    /**
     * Sets the incoming and outgoing links between neuron activations.
     *
     * @param doc
     * @param act
     */
    private void linkNeuronRelations(Document doc, Activation act) {
        long v = visitedCounter++;
        for(int dir = 0; dir < 2; dir++) {
            ArrayList<Activation> recNegTmp = new ArrayList<>();
            neuron.lock.acquireReadLock();
            TreeSet<Synapse> syns = (dir == 0 ? neuron.inputSynapses : neuron.outputSynapses);

            // Optimization in case the set of synapses is very large
            if (LINK_NEURON_RELATIONS_OPTIMIZATION && syns.size() > 10 && doc.activatedNeurons.size() * 20 < syns.size()) {
                TreeSet<Synapse> newSyns = new TreeSet<>(dir == 0 ? Synapse.INPUT_SYNAPSE_COMP : Synapse.OUTPUT_SYNAPSE_COMP);
                Synapse lk = new Synapse(null, Synapse.Key.MIN_KEY);
                Synapse uk = new Synapse(null, Synapse.Key.MAX_KEY);

                for (Neuron n : doc.activatedNeurons) {
                    if (dir == 0) {
                        lk.input = n;
                        uk.input = n;
                    } else {
                        lk.output = n;
                        uk.output = n;
                    }
                    newSyns.addAll(syns.subSet(lk, true, uk, true));
                }

                syns = newSyns;
            }


            for (Synapse s : syns) {
                Node n = (dir == 0 ? s.input : s.output).node;
                ThreadState th = n.getThreadState(doc, false);
                if(th == null || th.activations.isEmpty()) continue;

                Integer rid;
                if(dir == 0) {
                    rid = s.key.absoluteRid != null ? s.key.absoluteRid : Utils.nullSafeAdd(act.key.rid, false, s.key.relativeRid, false);
                } else {
                    rid = Utils.nullSafeSub(act.key.rid, false, s.key.relativeRid, false);
                }


                Operator begin = replaceFirstAndLast(s.key.startRangeMatch);
                Operator end = replaceFirstAndLast(s.key.endRangeMatch);
                Range r = act.key.r;
                if(dir == 0) {
                    begin = Operator.invert(s.key.startRangeMapping == START ? begin : (s.key.endRangeMapping == START ? end : NONE));
                    end = Operator.invert(s.key.endRangeMapping == END ? end : (s.key.startRangeMapping == END ? begin : NONE));

                    if(s.key.startRangeMapping != START || s.key.endRangeMapping != END) {
                        r = new Range(s.key.endRangeMapping == START ? r.end : (s.key.startRangeMapping == START ? r.begin : null), s.key.startRangeMapping == END ? r.begin : (s.key.endRangeMapping == END ? r.end : null));
                    }
                } else {
                    if(s.key.startRangeMapping != START || s.key.endRangeMapping != END) {
                        r = new Range(s.key.startRangeMapping == END ? r.end : (s.key.startRangeMapping == START ? r.begin : null), s.key.endRangeMapping == START ? r.begin : (s.key.endRangeMapping == END ? r.end : null));
                    }
                }

                Stream<Activation> tmp = Activation.select(
                        doc ,
                        n,
                        rid,
                        r,
                        begin,
                        end,
                        null,
                        null
                );

                final int d = dir;
                tmp.forEach(rAct -> {
                    Activation oAct = (d == 0 ? act : rAct);
                    Activation iAct = (d == 0 ? rAct : act);

                    SynapseActivation sa = new SynapseActivation(s, iAct, oAct);
                    oAct.neuronInputs.add(sa);
                    iAct.neuronOutputs.add(sa);

                    if(s.key.isNeg && s.key.isRecurrent) {
                        recNegTmp.add(rAct);
                    }
                });
            }
            neuron.lock.releaseReadLock();

            for(Activation rAct: recNegTmp) {
                Activation oAct = (dir == 0 ? act : rAct);
                Activation iAct = (dir == 0 ? rAct : act);

                markConflicts(iAct, oAct, v);

                addConflict(doc, oAct.key.o, iAct.key.o, iAct, Collections.singleton(act), v);
            }
        }
    }


    private Operator replaceFirstAndLast(Operator rm) {
        return rm == FIRST || rm == LAST ? EQUALS : rm;
    }


    private void unlinkNeuronRelations(Document doc, Activation act) {
        long v = visitedCounter++;
        for(int dir = 0; dir < 2; dir++) {
            for (SynapseActivation sa: (dir == 0 ? act.neuronInputs : act.neuronOutputs)) {
                Synapse s = sa.s;
                Activation rAct = dir == 0 ? sa.input : sa.output;

                if(s.key.isNeg && s.key.isRecurrent) {
                    Activation oAct = (dir == 0 ? act : rAct);
                    Activation iAct = (dir == 0 ? rAct : act);

                    markConflicts(iAct, oAct, v);

                    removeConflict(doc, oAct.key.o, iAct.key.o, iAct, act, v);
                }
            }
        }

        for(int dir = 0; dir < 2; dir++) {
            for (SynapseActivation sa: (dir == 0 ? act.neuronInputs : act.neuronOutputs)) {
                Activation rAct = dir == 0 ? sa.input : sa.output;
                (dir == 0 ? rAct.neuronOutputs : rAct.neuronInputs).remove(sa);
            }
        }
    }


    private static void addConflict(Document doc, InterprNode io, InterprNode o, Activation act, Collection<Activation> inputActs, long v) {
        if(o.markedConflict == v || o.orInterprNodes == null) {
            if (!isAllowed(doc, io, o, inputActs)) {
                Conflicts.add(doc, act, io, o);
            }
        } else {
            for(InterprNode no: o.orInterprNodes.values()) {
                addConflict(doc, io, no, act, inputActs, v);
            }
        }
    }


    private static void removeConflict(Document doc, InterprNode io, InterprNode o, Activation act, Activation nAct, long v) {
        if(o.markedConflict == v || o.orInterprNodes == null) {
            if (!nAct.key.n.isAllowedOption(doc, o, nAct, visitedCounter++)) {
                assert io != null;

                Conflicts.remove(doc, act, io, o);
            }
        } else {
            for(InterprNode no: o.orInterprNodes.values()) {
                removeConflict(doc, io, no, act, nAct, v);
            }
        }
    }


    private void markConflicts(Activation iAct, Activation oAct, long v) {
        oAct.key.o.markedConflict = v;
        for(SynapseActivation sa: iAct.neuronOutputs) {
            if(sa.s.key.isRecurrent && sa.s.key.isNeg) {
                sa.output.key.o.markedConflict = v;
            }
        }
    }


    private static boolean isAllowed(Document doc, InterprNode io, InterprNode o, Collection<Activation> inputActs) {
        if(io != null && o.contains(io, false)) return true;
        for (Activation act : inputActs) {
            if (act.key.n.isAllowedOption(doc, o, act, visitedCounter++)) return true;
        }
        return false;
    }


    /**
     * Process all added or removed activation for this logic node.
     *
     * @param doc
     */
    public void processChanges(Document doc) {
        ThreadState th = getThreadState(doc, true);
        NavigableMap<Key, Collection<Activation>> tmpAdded = th.added;
        NavigableMap<Key, RemovedEntry> tmpRemoved = th.removed;

        th.added = new TreeMap<>();
        th.removed = new TreeMap<>();

        for (Iterator<Map.Entry<Key, RemovedEntry>> it = tmpRemoved.entrySet().iterator(); it.hasNext(); ) {
            Key akr = it.next().getKey();
            boolean remove = false;
            for (Key aka : tmpAdded.keySet()) {
                if (aka.o == akr.o && aka.rid == akr.rid && aka.r == akr.r)
                    remove = true;
            }
            if (remove) it.remove();
        }

        for(RemovedEntry re: tmpRemoved.values()) {
            if(!hasSupport(re.act)) {
                re.act.removedId = Activation.removedIdCounter++;
                re.act.isRemoved = true;
            }
        }

        for(RemovedEntry re: tmpRemoved.values()) {
            processRemovedActivation(doc, re.act, re.iActs);
        }

        for(Map.Entry<Key, Collection<Activation>> me: tmpAdded.entrySet()) {
            processAddedActivation(doc, me.getKey(), me.getValue());
        }
    }


    /**
     * Add a new activation to this logic node and further propagate this activation through the network.
     * This activation, however, will not be added immediately. This method only adds a request to the activations
     * queue in the document. The activation will be added when the method {@code Node.processChanges(Document doc)}
     * is called.
     *
     * @param doc
     * @param ak
     * @param inputActs
     */
    public static void addActivationAndPropagate(Document doc, Key ak, Collection<Activation> inputActs) {
        ThreadState th = ak.n.getThreadState(doc, true);
        Collection<Activation> iActs = th.added.get(ak);
        if(iActs == null) {
            iActs = new ArrayList<>();
            th.added.put(ak, iActs);
        }
        iActs.addAll(inputActs);
        doc.queue.add(ak.n);
    }


    Range preProcessAddedActivation(Document doc, Key ak, Collection<Activation> inputActs) {
        return ak.r;
    }


    void processAddedActivation(Document doc, Key ak, Collection<Activation> inputActs) {
        Range r = preProcessAddedActivation(doc, ak, inputActs);
        if(r == null) return;

        Key nak = new Key(this, r, ak.rid, ak.o);

        if (Document.APPLY_DEBUG_OUTPUT) {
            log.info("add: " + nak + " - " + nak.n);
        }

        addActivationInternal(doc, nak, inputActs, false);
    }


    /*
    First remove the inputs from the given activation. Only if, depending on the node type, insufficient support exists for this activation, then actually remove it.
     */
    public static void removeActivationAndPropagate(Document doc, Activation act, Collection<Activation> inputActs) {
        if(act == null || act.isRemoved) return;

        ThreadState th = act.key.n.getThreadState(doc, true);
        RemovedEntry re = th.removed.get(act.key);
        if(re == null) {
            re = new RemovedEntry();
            re.act = act;
            th.removed.put(act.key, re);
        }
        re.iActs.addAll(inputActs);
        doc.queue.add(act.key.n);
    }


    void postProcessRemovedActivation(Document doc, Activation act, Collection<Activation> inputActs) {}


    private void processRemovedActivation(Document doc, Activation act, Collection<Activation> inputActs) {
        if(Document.APPLY_DEBUG_OUTPUT) {
            log.info("remove: " + act.key + " - " + act.key.n);
        }

        if(removeActivationInternal(doc, act, inputActs)) {
            postProcessRemovedActivation(doc, act, inputActs);
        }
    }


    public Collection<Activation> getActivations(Document doc) {
        ThreadState th = getThreadState(doc, false);
        if(th == null) return Collections.EMPTY_LIST;
        return th.activations.values();
    }


    public synchronized Activation getFirstActivation(Document doc) {
        ThreadState th = getThreadState(doc, false);
        if(th == null || th.activations.isEmpty()) return null;
        return th.activations.firstEntry().getValue();
    }


    public void clearActivations(Document doc) {
        ThreadState th = getThreadState(doc, false);
        if(th == null) return;
        th.activations.clear();

        if(th.activationsEnd != null) th.activationsEnd.clear();
        if(th.activationsRid != null) th.activationsRid.clear();

        th.added.clear();
        th.removed.clear();
    }


    public void clearActivations(Model m) {
        for(int i = 0; i < m.numberOfThreads; i++) {
            clearActivations(m.createDocument(null, i));
        }
    }


    public boolean isFrequent() {
        return frequency >= minFrequency;
    }


    public boolean isPublic() {
        return this instanceof AndNode && orChildren != null && !orChildren.isEmpty();
    }


    boolean computeAndParents(Document doc, Integer offset, SortedSet<Refinement> inputs, Map<Refinement, Node> parents, boolean discoverPatterns, long v) {
        RidVisited nv = getThreadState(doc, true).lookupVisited(offset);
        if(nv.computeParents == v) return true;
        nv.computeParents = v;

        if(inputs.size() == 1) {
            parents.put(inputs.first(), this);
            return true;
        }

        for(Refinement ref: inputs) {
            SortedSet<Refinement> childInputs = new TreeSet<>(inputs);
            childInputs.remove(ref);

            Refinement nRef = new Refinement(ref.getRelativePosition(), offset, ref.input);
            lock.acquireReadLock();
            AndNode cp = andChildren != null ? andChildren.get(nRef) : null;
            lock.releaseReadLock();

            if(cp == null) {
                if(discoverPatterns) return false;
                cp = AndNode.createNextLevelNode(doc, this, nRef, discoverPatterns);
                if(cp == null) return false;
            }

            Integer nOffset = Utils.nullSafeMin(ref.getRelativePosition(), offset);
            if(!cp.computeAndParents(doc, nOffset, childInputs, parents, discoverPatterns, v)) {
                return false;
            }
        }
        return true;
    }


    void removeFromNextLevel(Document doc, Activation iAct) {
        AndNode.removeActivation(doc, iAct);

        if(orChildren != null) {
            for (OrEntry oe : orChildren) {
                ((OrNode) oe.node).removeActivation(doc, oe.ridOffset, iAct);
            }
        }
    }


    void remove(Document doc) {
        assert !isRemoved;

        if(neuron != null) {
            neuron.remove(doc);
        }

        lock.acquireWriteLock(doc.threadId);
        while(andChildren != null && !andChildren.isEmpty()) {
            andChildren.pollFirstEntry().getValue().remove(doc);
        }

        while(orChildren != null && !orChildren.isEmpty())  {
            orChildren.pollFirst().node.remove(doc);
        }
        lock.releaseWriteLock();

//        m.allNodes.remove(this);

        clearActivations(doc.m);

        isRemoved = true;
        isRemovedId = isRemovedIdCounter++;
    }


    AndNode getAndChild(Refinement ref) {
        lock.acquireReadLock();
        AndNode result = andChildren != null ? andChildren.get(ref) : null;
        lock.releaseReadLock();
        return result;
    }


    private static int evaluate(Neuron n, RSKey rsk) {
        double sum = rsk.pa.computeSynapseWeightSum(rsk.offset, n);
        if(sum < 0.0) return -1;
        if(rsk.pa == null) return 0;

        if(rsk.pa instanceof AndNode) {
            AndNode an = (AndNode) rsk.pa;
            for(Refinement ref: an.parents.keySet()) {
                Synapse s = ref.input.getSynapse(new SynapseKey(rsk.offset, n));
                if(sum - Math.abs(s.w) >= 0.0) return 1;
            }
        } else {
            InputNode in = (InputNode) rsk.pa;
            Synapse s = in.getSynapse(new SynapseKey(rsk.offset, n));
            if(sum - Math.abs(s.w) >= 0.0) return 1;
        }
        return 0;
    }


    /**
     * Translates the synapse weights of a neuron into logic nodes.
     *
     * @param doc
     * @param neuron
     * @param dir
     * @return
     */
    public static boolean adjust(Document doc, Neuron neuron, final int dir) {
        long v = visitedCounter++;
        OrNode outputNode = (OrNode) neuron.node;

        if(neuron.inputSynapsesByWeight.isEmpty()) return false;

        neuron.maxRecurrentSum = 0.0;
        for(Synapse s: neuron.inputSynapsesByWeight) {
            s.input.lock.acquireWriteLock(doc.threadId);

            if (s.inputNode == null) {
                s.inputNode = InputNode.add(doc, s.key.createInputNodeKey(), s.input);
                s.inputNode.isBlocked = s.input.isBlocked;
                s.inputNode.setSynapse(doc.threadId, new SynapseKey(s.key.relativeRid, neuron), s);
            }

            if (s.key.isRecurrent) {
                neuron.maxRecurrentSum += Math.abs(s.w);
            }
            s.input.lock.releaseWriteLock();
        }


        TreeSet<RSKey> queue = new TreeSet<>(new Comparator<RSKey>() {
            @Override
            public int compare(RSKey rsk1, RSKey rsk2) {
                if(rsk1.pa == null && rsk2.pa != null) return -1;
                else if(rsk1.pa != null && rsk2.pa == null) return 1;
                else if(rsk1.pa == null && rsk2.pa == null) return 0;

                int r = Integer.compare(rsk2.pa.level, rsk1.pa.level) * dir;
                if(r != 0) return r;
                r = rsk1.pa.compareTo(rsk2.pa);
                if(r != 0) return r;
                return Utils.compareInteger(rsk1.offset, rsk2.offset);
            }
        });

/*        for(OrEntry oe: outputNode.parents) {
            queue.add(new Node.RSKey(oe.node, oe.ridOffset));
        }
*/
        if(queue.isEmpty()) {
            queue.add(new Node.RSKey(null, null));
        }

        List<RSKey> outputs = new ArrayList<>();
        List<RSKey> cleanup = new ArrayList<>();
        while(!queue.isEmpty()) {
            RSKey rsk = queue.pollFirst();
            Node n = rsk.pa;

            if(dir == -1) {
                computeRefinements(doc, queue, neuron, rsk, v, outputs, cleanup);
            } else {
                if(n instanceof AndNode) {
                    AndNode an = (AndNode) n;

                    for(Map.Entry<Refinement, Node> me: an.parents.entrySet()) {
                        Node pn = me.getValue();
                        RSKey prsk = new RSKey(pn, me.getKey().getOffset()); // TODO: Prüfen
                        switch(evaluate(neuron, prsk)) {
                            case -1:
                                break;
                            case 0:
                                outputs.add(prsk);
                                break;
                            case 1:
                                RidVisited nv = pn.getThreadState(doc, true).lookupVisited(rsk.offset);
                                if(nv.adjust != v) {
                                    nv.adjust = v;
                                    queue.add(prsk);
                                }
                        }
                    }

                    cleanup.add(rsk);
                }
            }
        }

        if(outputs.isEmpty()) return false;

        outputNode.lock.acquireWriteLock(doc.threadId);
        outputNode.removeAllInputs(doc);

        for(RSKey rsk: outputs) {
            rsk.pa.lock.acquireWriteLock(doc.threadId);
            outputNode.addInput(doc, rsk.offset, rsk.pa);
            rsk.pa.lock.releaseWriteLock();
        }
        outputNode.lock.releaseWriteLock();

        for(RSKey on: cleanup) {
            on.pa.cleanup(doc);
        }

        return true;
    }


    private static void computeRefinements(Document doc, TreeSet<RSKey> queue, Neuron n, RSKey rsk, long v, List<RSKey> outputs, List<RSKey> cleanup) {
        n.lock.acquireWriteLock(doc.threadId);
        Synapse minSyn = null;
        double sum = 0.0;
        if(rsk.pa == null) {
        } else if(rsk.pa instanceof InputNode) {
            InputNode node = (InputNode) rsk.pa;
            minSyn = node.getSynapse(new SynapseKey(rsk.offset, n));
            sum = Math.abs(minSyn.w);
        } else {
            AndNode node = (AndNode) rsk.pa;

            for(Refinement ref: node.parents.keySet()) {
                Synapse s = ref.getSynapse(rsk.offset, n);
                if(minSyn == null || Synapse.INPUT_SYNAPSE_BY_WEIGHTS_COMP.compare(minSyn, s) > 0) {
                    minSyn = s;
                }
                sum += Math.abs(s.w);
            }
        }

        for(Synapse s: (minSyn != null ? n.inputSynapsesByWeight.headSet(minSyn, false) : n.inputSynapsesByWeight)) {
            if(n.bias - (n.negDirSum + n.negRecSum) + n.posRecSum + sum + Math.abs(s.w) + s.maxLowerWeightsSum > 0.0 && !s.key.isNeg && !s.key.isRecurrent) {
                Node nln = rsk.pa == null ?
                        s.inputNode :
                        AndNode.createNextLevelNode(doc, rsk.pa, new Refinement(s.key.relativeRid, rsk.offset, s.inputNode), false);

                if(nln != null) {
                    nln.prepareResultsForPredefinedNodes(doc, queue, v, outputs, cleanup, n, s, Utils.nullSafeMin(s.key.relativeRid, rsk.offset));
                }
            }
        }
        n.lock.releaseWriteLock();
    }


    void prepareResultsForPredefinedNodes(Document doc, TreeSet<RSKey> queue, long v, List<RSKey> outputs, List<RSKey> cleanup, Neuron n, Synapse s, Integer offset) {
        RSKey rs = new RSKey(this, offset);
        RidVisited nv = getThreadState(doc, true).lookupVisited(offset);
        // TODO: mindestens einen positiven Knoten mit rein nehmen.
        if(computeSynapseWeightSum(offset, n) + n.posRecSum - (n.negDirSum + n.negRecSum) > 0 || !isExpandable(false) || (Math.abs(s.w) / -n.bias) < 0.1) {
            if(nv.outputNode != v) {
                nv.outputNode = v;
                if (isCovered(doc, offset, v)) {
                    cleanup.add(rs);
                } else {
                    outputs.add(rs);
                }
            }
        } else {
            if(nv.adjust != v) {
                nv.adjust = v;
                queue.add(rs);
            }
        }
    }


    private static class RSKey implements Comparable<RSKey> {
        Node pa;
        Integer offset;

        public RSKey(Node pa, Integer offset) {
            this.pa = pa;
            this.offset = offset;
        }


        public String toString() {
            return "Offset:" + offset + " PA:" + pa.logicToString();
        }

        @Override
        public int compareTo(RSKey rs) {
            int r = pa.compareTo(rs.pa);
            if(r != 0) return r;
            return Utils.compareInteger(offset, rs.offset);
        }
    }


    public boolean isCovered(Document doc, Integer offset, long v) {
        return false;
    }


    public boolean isRequired() {
        return numberOfNeuronRefs > 0;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toSimpleString());
        sb.append(" - ");
        sb.append(logicToString());
        sb.append(" - ");
        sb.append(weightsToString());
        return sb.toString();
    }


    public String toSimpleString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        if(neuron != null && neuron.label != null) {
            sb.append(" ");
            sb.append(neuron.label);
        }
        return sb.toString();
    }


    public String weightsToString() {
        return "";
    }



    public static Node reactivate(Model m, Integer id) {
        assert m.suspensionHook != null;

        byte[] nData = m.suspensionHook.retrieve(id, SuspensionHook.Type.NODE);
        ByteArrayInputStream bais = new ByteArrayInputStream(nData);
        DataInputStream dis = new DataInputStream(bais);
        try {
            return read(dis, m);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void suspend(Model m) {
        assert m.suspensionHook != null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            write(dos);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        m.suspensionHook.store(id, SuspensionHook.Type.NODE, baos.toByteArray());

        for(AndNode n: andChildren.values()) {
            n.suspend(m);
        }

        if(neuron != null) {
            neuron.node = null;
        }

        if(this instanceof AndNode) {
            AndNode n = (AndNode) this;
            for(Map.Entry<Refinement, Node> me: n.parents.entrySet()) {
                Refinement ref = me.getKey();
                Node pn = me.getValue();

                pn.removeAndChild(ref);
                pn.addSuspendedAndChild(ref, n.id);
            }
        }
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(level);

        out.writeInt(frequency);
        out.writeDouble(nullHypFreq);
        out.writeDouble(oldNullHypFreq);

        out.writeBoolean(isBlocked);

        out.writeBoolean(endRequired);
        out.writeBoolean(ridRequired);

        out.writeInt(numberOfNeuronRefs);
        out.writeBoolean(frequencyHasChanged);
        out.writeInt(nOffset);

        out.writeInt(sizeSum);
        out.writeInt(instanceSum);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        id = in.readInt();
        level = in.readInt();

        frequency = in.readInt();
        nullHypFreq = in.readDouble();
        oldNullHypFreq = in.readDouble();

        isBlocked = in.readBoolean();

        endRequired = in.readBoolean();
        ridRequired = in.readBoolean();

        numberOfNeuronRefs = in.readInt();
        frequencyHasChanged = in.readBoolean();
        nOffset = in.readInt();

        sizeSum = in.readInt();
        instanceSum = in.readInt();

        threads = new ThreadState[m.numberOfThreads];
    }


    public static Node read(DataInput in, Model m) throws IOException {
        String type = in.readUTF();
        Node n = null;
        switch(type) {
            case "I":
                n = new InputNode();
                break;
            case "A":
                n = new AndNode();
                break;
            case "O":
                n = new OrNode();
                break;
        }
        n.readFields(in, m);
        return n;
    }



    @Override
    public int compareTo(Node n) {
        if(id < n.id) return -1;
        else if(id > n.id) return 1;
        else return 0;
    }


    public static int compare(Node a, Node b) {
        if(a == b) return 0;
        if(a == null && b != null) return -1;
        if(a != null && b == null) return 1;
        return a.compareTo(b);
    }


    private static class RemovedEntry {
        Activation act;
        Set<Activation> iActs = new TreeSet<>();
    }


    private static class DummyNode extends InputNode {

        public DummyNode(int id) {
            super();
            this.id = id;
        }

        @Override
        public boolean isAllowedOption(Document doc, InterprNode n, Activation act, long v) {
            return false;
        }

        @Override
        public void cleanup(Document doc) {}

        @Override
        public void initActivation(Document doc, Activation act) {}

        @Override
        public void deleteActivation(Document doc, Activation act) {}

        @Override
        public double computeSynapseWeightSum(Integer offset, Neuron n) {
            return n.bias;
        }

        @Override
        public void propagateAddedActivation(Document doc, Activation act, InterprNode removedConflict) {}

        @Override
        public void propagateRemovedActivation(Document doc, Activation act) {}

        @Override
        public String logicToString() {
            return null;
        }

        @Override
        protected void apply(Document doc, Activation act, InterprNode conflict) {}

        @Override
        public void discover(Document doc, Activation act) {}

        @Override
        protected Set<Refinement> collectNodeAndRefinements(Refinement newRef) { return null; }

        @Override
        protected void changeNumberOfNeuronRefs(Document doc, long v, int d) {
        }
    }


    static class ReverseAndRefinement implements Comparable<ReverseAndRefinement> {
        boolean dir;
        Node node;

        public ReverseAndRefinement(Node n, Integer a, Integer b) {
            this.node = n;
            this.dir = Utils.compareNullSafe(a, b);
        }

        @Override
        public int compareTo(ReverseAndRefinement rar) {
            int r = node.compareTo(rar.node);
            if(r != 0) return r;
            return Boolean.compare(dir, rar.dir);
        }
    }

}
