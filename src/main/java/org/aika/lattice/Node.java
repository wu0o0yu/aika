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
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.lattice.InputNode.SynapseKey;
import org.aika.lattice.OrNode.OrEntry;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.RangeVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class Node implements Comparable<Node>, Writable {
    public static int minFrequency = 5;
    public static int MAX_RID = 20;

    public static boolean LINK_NEURON_RELATIONS_OPTIMIZATION = true;

    public static final DummyNode MIN_NODE = new DummyNode(Integer.MIN_VALUE);
    public static final DummyNode MAX_NODE = new DummyNode(Integer.MAX_VALUE);

    private static final Logger log = LoggerFactory.getLogger(Node.class);

    public static AtomicInteger currentNodeId = new AtomicInteger(0);
    public int id;

    public TreeMap<ReverseAndRefinement, Refinement> reverseAndChildren;
    public TreeMap<Refinement, AndNode> andChildren;
    public TreeSet<OrEntry> orChildren;

    public int level;

    // Temporary fix for a performance problem
    public boolean passive;

    public volatile int frequency;
    public volatile double nullHypFreq;
    public volatile double oldNullHypFreq;

    public boolean isBlocked;

    public boolean endRequired;
    public boolean ridRequired;


    public int numberOfNeuronRefs = 0;
    public volatile boolean isRemoved;
    public volatile int isRemovedId;
    public static int isRemovedIdCounter = 0;

    public volatile boolean frequencyHasChanged = true;
    public volatile int nOffset;

    public volatile int sizeSum = 0;
    public volatile int instanceSum = 0;


    // Only childrens are locked.
    public ReadWriteLock lock = new ReadWriteLock();


    public boolean isQueued = false;
    public long queueId;

    public Neuron neuron = null;

    public RangeVisibility[] rangeVisibility;
    public boolean[] matchRange;

    public static long visitedCounter = 0;

    public ThreadState[] threads;

    public static class ThreadState {
        public long lastUsed;

        public TreeMap<Key, Activation> activations;
        public TreeMap<Key, Activation> activationsEnd;
        public TreeMap<Key, Activation> activationsRid;

        public NavigableMap<Key, Collection<Activation>> added;
        public NavigableMap<Key, RemovedEntry> removed;
        protected long visitedNeuronRefsChange = -1;
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

    public static class RidVisited {
        public long computeParents = -1;
        public long outputNode = -1;
        public long adjust = -1;
    }


    public ThreadState getThreadState(Iteration t) {
        ThreadState th = threads[t.threadId];
        if(th == null) {
            th = new ThreadState(endRequired, ridRequired);
            threads[t.threadId] = th;
        }
        th.lastUsed = t.iterationId;
        return th;
    }


    public abstract void propagateAddedActivation(Iteration t, Activation act, Option conflict);

    public abstract void propagateRemovedActivation(Iteration t, Activation act);

    public abstract boolean isAllowedOption(Iteration t, Option n, Activation act, long v);

    public abstract void cleanup(Iteration t);

    public abstract void initActivation(Iteration t, Activation act);

    public abstract void deleteActivation(Iteration t, Activation act);

    public abstract double computeSynapseWeightSum(Integer offset, Neuron n);

    public abstract String logicToString();

    public abstract void apply(Iteration t, Activation act, Option conflict);

    public abstract void discover(Iteration t, Activation act);

    protected abstract Collection<Refinement> collectNodeAndRefinements(Refinement newRef);

    protected abstract void changeNumberOfNeuronRefs(Iteration t, long v, int d);

    protected abstract boolean hasSupport(Activation act);

    public abstract void computeNullHyp(Model m);

    public abstract boolean isExpandable(boolean checkFrequency);


    protected Node() {}


    public Node(Iteration t, int level) {
        Model m = t.m;
        threads = new ThreadState[m.numberOfThreads];
        id = currentNodeId.addAndGet(1);
        this.level = level;
        if(m != null) {
            m.allNodes[t.threadId].add(this);
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
            return Option.compare(k1.o, k2.o);
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
            return Option.compare(k1.o, k2.o);
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
            return Option.compare(k1.o, k2.o);
        }
    };


    public void addOrChild(Iteration t, OrEntry oe) {
        lock.acquireWriteLock(t.threadId);
        if(orChildren == null) {
            orChildren = new TreeSet<>();
        }
        orChildren.add(oe);
        lock.releaseWriteLock();
    }


    public void removeOrChild(Iteration t, OrEntry oe) {
        lock.acquireWriteLock(t.threadId);
        if(orChildren != null) {
            orChildren.remove(oe);
            if(orChildren.isEmpty()) {
                orChildren = null;
            }
        }
        lock.releaseWriteLock();
    }


    public void addAndChild(Refinement ref, AndNode child) {
        if(andChildren == null) {
            andChildren = new TreeMap<>();
            reverseAndChildren = new TreeMap<>();
        }

        AndNode n = andChildren.put(ref, child);
        assert n == null;
        reverseAndChildren.put(new ReverseAndRefinement(child, ref.rid, 0), ref);
    }


    public void removeAndChild(Refinement ref) {
        if(andChildren != null) {
            andChildren.remove(ref);
            reverseAndChildren.remove(new ReverseAndRefinement(this, ref.rid, 0));

            if(andChildren.isEmpty()) {
                andChildren = null;
                reverseAndChildren = null;
            }
        }
    }


    public void count(Iteration t) {
        for(Activation act: getThreadState(t).activations.values()) {
            frequency++;
            frequencyHasChanged = true;

            sizeSum += act.key.r.end == Integer.MAX_VALUE ? 1 : Math.max(1, act.key.r.end - act.key.r.begin);
            instanceSum++;
        }
    }


    public Activation addActivationInternal(Iteration t, Key ak, Collection<Activation> inputActs, boolean isTrainingAct) {
        Activation act = Activation.get(t, this, ak);
        if(act == null) {
            act = new Activation(ak);
            act.isTrainingAct = isTrainingAct;

            if(neuron != null) {
                act.neuronInputs = new TreeSet<>(SynapseActivation.INPUT_COMP);
                act.neuronOutputs = new TreeSet<>(SynapseActivation.OUTPUT_COMP);
            }

            initActivation(t, act);
            act.register(t);

            act.link(inputActs);

            if(neuron != null) {
                linkNeuronRelations(t, act);
            }

            if(!isTrainingAct) {
                propagateAddedActivation(t, act, null);
            }
        } else {
            if(neuron != null) {
                linkNeuronRelations(t, act);
            }
            act.link(inputActs);
        }

        return act;
    }


    public boolean removeActivationInternal(Iteration t, Activation act, Collection<Activation> inputActs) {
        boolean flag = false;
        if(act.isRemoved) {
            act.unregister(t);
            deleteActivation(t, act);

            propagateRemovedActivation(t, act);

            act.key.releaseRef();

            if(neuron != null) {
                unlinkNeuronRelations(t, act);
            }
            flag = true;
        }

        // TODO: check unlinkNeuronRelations symmetry
        act.unlink(inputActs);

        return flag;
    }


    private void linkNeuronRelations(Iteration t, Activation act) {
        long v = visitedCounter++;
        for(int dir = 0; dir < (passive ? 1 : 2); dir++) {
            ArrayList<Activation> recNegTmp = new ArrayList<>();
            neuron.lock.acquireReadLock();
            TreeSet<Synapse> syns = (dir == 0 ? neuron.inputSynapses : neuron.outputSynapses);

            // Optimization in case the set of synapses is very large
            if (LINK_NEURON_RELATIONS_OPTIMIZATION && syns.size() > 10 && t.activatedNeurons.size() * 20 < syns.size()) {
                TreeSet<Synapse> newSyns = new TreeSet<>(dir == 0 ? Synapse.INPUT_SYNAPSE_COMP : Synapse.OUTPUT_SYNAPSE_COMP);
                Synapse lk = new Synapse(null, Synapse.Key.MIN_KEY);
                Synapse uk = new Synapse(null, Synapse.Key.MAX_KEY);

                for (Neuron n : t.activatedNeurons) {
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
                ThreadState th = n.getThreadState(t);
                if(th.activations.isEmpty()) continue;

                Integer rid;
                if(dir == 0) {
                    rid = s.key.absoluteRid != null ? s.key.absoluteRid : Utils.nullSafeAdd(act.key.rid, false, s.key.relativeRid, false);
                } else {
                    rid = Utils.nullSafeSub(act.key.rid, false, s.key.relativeRid, false);
                }
                Stream<Activation> tmp = Activation.select(
                        t,
                        n,
                        rid,
                        act.key.r,
                        s.key.matchRange ? new Range.SynapseRangeMatcher(s, dir != 0) : null,
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

                addConflict(t, oAct.key.o, iAct.key.o, iAct, Collections.singleton(act), v);
            }
        }
    }


    private void unlinkNeuronRelations(Iteration t, Activation act) {
        long v = visitedCounter++;
        for(int dir = 0; dir < 2; dir++) {
            for (SynapseActivation sa: (dir == 0 ? act.neuronInputs : act.neuronOutputs)) {
                Synapse s = sa.s;
                Activation rAct = dir == 0 ? sa.input : sa.output;

                if(s.key.isNeg && s.key.isRecurrent) {
                    Activation oAct = (dir == 0 ? act : rAct);
                    Activation iAct = (dir == 0 ? rAct : act);

                    markConflicts(iAct, oAct, v);

                    removeConflict(t, oAct.key.o, iAct.key.o, iAct, act, v);
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


    public static void addConflict(Iteration t, Option io, Option o, Activation act, Collection<Activation> inputActs, long v) {
        if(o.markedConflict == v || o.orOptions == null) {
            if (!isAllowed(t, io, o, inputActs)) {
                Conflicts.add(t, act, io, o);
            }
        } else {
            for(Option no: o.orOptions.values()) {
                addConflict(t, io, no, act, inputActs, v);
            }
        }
    }


    public static void removeConflict(Iteration t, Option io, Option o, Activation act, Activation nAct, long v) {
        if(o.markedConflict == v || o.orOptions == null) {
            if (!nAct.key.n.isAllowedOption(t, o, nAct, visitedCounter++)) {
                assert io != null;

                Conflicts.remove(t, act, io, o);
            }
        } else {
            for(Option no: o.orOptions.values()) {
                removeConflict(t, io, no, act, nAct, v);
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


    private static boolean isAllowed(Iteration t, Option io, Option o, Collection<Activation> inputActs) {
        if(io != null && o.contains(io, false)) return true;
        for (Activation act : inputActs) {
            if (act.key.n.isAllowedOption(t, o, act, visitedCounter++)) return true;
        }
        return false;
    }


    public void processChanges(Iteration t) {
        ThreadState th = getThreadState(t);
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
            processRemovedActivation(t, re.act, re.iActs);
        }

        for(Map.Entry<Key, Collection<Activation>> me: tmpAdded.entrySet()) {
            processAddedActivation(t, me.getKey(), me.getValue());
        }
    }


    public static void addActivationAndPropagate(Iteration t, Key ak, Collection<Activation> inputActs) {
        ThreadState th = ak.n.getThreadState(t);
        Collection<Activation> iActs = th.added.get(ak);
        if(iActs == null) {
            iActs = new ArrayList<>();
            th.added.put(ak, iActs);
        }
        iActs.addAll(inputActs);
        t.queue.add(ak.n);
    }


    protected Range preProcessAddedActivation(Iteration t, Key ak, Collection<Activation> inputActs) {
        return ak.r;
    }


    public void processAddedActivation(Iteration t, Key ak, Collection<Activation> inputActs) {
        Range r = preProcessAddedActivation(t, ak, inputActs);
        if(r == null) return;

        Key nak = new Key(this, r, ak.rid, ak.o);

        if (Iteration.APPLY_DEBUG_OUTPUT) {
            log.info("add: " + nak + " - " + nak.n);
        }

        addActivationInternal(t, nak, inputActs, false);
    }


    /*
    First remove the inputs from the given activation. Only if, depending on the node type, insufficient support exists for this activation, then actually remove it.
     */
    public static void removeActivationAndPropagate(Iteration t, Activation act, Collection<Activation> inputActs) {
        if(act == null || act.isRemoved) return;

        ThreadState th = act.key.n.getThreadState(t);
        RemovedEntry re = th.removed.get(act.key);
        if(re == null) {
            re = new RemovedEntry();
            re.act = act;
            th.removed.put(act.key, re);
        }
        re.iActs.addAll(inputActs);
        t.queue.add(act.key.n);
    }


    protected void postProcessRemovedActivation(Iteration t, Activation act, Collection<Activation> inputActs) {}


    public void processRemovedActivation(Iteration t, Activation act, Collection<Activation> inputActs) {

        if(Iteration.APPLY_DEBUG_OUTPUT) {
            log.info("remove: " + act.key + " - " + act.key.n);
        }

        if(removeActivationInternal(t, act, inputActs)) {
            postProcessRemovedActivation(t, act, inputActs);
        }
    }



    public Collection<Activation> getActivations(Iteration t) {
        return getThreadState(t).activations.values();
    }


    public synchronized Activation getFirstActivation(Iteration t) {
        ThreadState th = getThreadState(t);
        if(th.activations.isEmpty()) return null;
        return th.activations.firstEntry().getValue();
    }


    public void clearActivations(Iteration t) {
        ThreadState th = getThreadState(t);
        th.activations.clear();

        if(th.activationsEnd != null) th.activationsEnd.clear();
        if(th.activationsRid != null) th.activationsRid.clear();

        th.added.clear();
        th.removed.clear();
    }


    public void clearActivations(Model m) {
        for(int i = 0; i < m.numberOfThreads; i++) {
            clearActivations(m.startIteration(null, i));
        }
    }


    public boolean isFrequent() {
        return frequency >= minFrequency;
    }


    public boolean isPublic() {
        return this instanceof AndNode && orChildren != null && !orChildren.isEmpty();
    }


    public boolean computeAndParents(Iteration t, Integer offset, SortedSet<Refinement> inputs, Map<Refinement, Node> parents, boolean discoverPatterns, long v) {
        RidVisited nv = getThreadState(t).lookupVisited(offset);
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
                cp = AndNode.createNextLevelNode(t, this, nRef, discoverPatterns);
                if(cp == null) return false;
            }

            Integer nOffset = Utils.nullSafeMin(ref.getRelativePosition(), offset);
            if(!cp.computeAndParents(t, nOffset, childInputs, parents, discoverPatterns, v)) {
                return false;
            }
        }
        return true;
    }


    public void removeFromNextLevel(Iteration t, Activation iAct) {
        AndNode.removeActivation(t, iAct);

        if(orChildren != null) {
            for (OrEntry oe : orChildren) {
                ((OrNode) oe.node).removeActivation(t, oe.ridOffset, iAct);
            }
        }
    }


    public void remove(Iteration t) {
        assert !isRemoved;

        if(neuron != null) {
            neuron.remove(t);
        }

        lock.acquireWriteLock(t.threadId);
        while(andChildren != null && !andChildren.isEmpty()) {
            andChildren.pollFirstEntry().getValue().remove(t);
        }

        while(orChildren != null && !orChildren.isEmpty())  {
            orChildren.pollFirst().node.remove(t);
        }
        lock.releaseWriteLock();

//        m.allNodes.remove(this);

        clearActivations(t.m);

        isRemoved = true;
        isRemovedId = isRemovedIdCounter++;
    }


    public AndNode getAndChild(Refinement ref) {
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


    public static boolean adjust(Iteration t, Neuron neuron, final int dir) {
        long v = visitedCounter++;
        OrNode outputNode = (OrNode) neuron.node;

        if(neuron.inputSynapsesByWeight.isEmpty()) return false;

        neuron.maxRecurrentSum = 0.0;
        for(Synapse s: neuron.inputSynapsesByWeight) {
            s.input.lock.acquireWriteLock(t.threadId);

            if (s.inputNode == null) {
                s.inputNode = InputNode.add(t, s.key.createInputNodeKey(), s.input);
                s.inputNode.isBlocked = s.input.isBlocked;
                s.inputNode.setSynapse(t, new SynapseKey(s.key.relativeRid, neuron), s);
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
                computeRefinements(t, queue, neuron, rsk, v, outputs, cleanup);
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
                                RidVisited nv = pn.getThreadState(t).lookupVisited(rsk.offset);
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

        outputNode.lock.acquireWriteLock(t.threadId);
        outputNode.removeAllInputs(t);

        for(RSKey rsk: outputs) {
            rsk.pa.lock.acquireWriteLock(t.threadId);
            outputNode.addInput(t, rsk.offset, rsk.pa);
            rsk.pa.lock.releaseWriteLock();
        }
        outputNode.lock.releaseWriteLock();

        for(RSKey on: cleanup) {
            on.pa.cleanup(t);
        }

        return true;
    }


    public static void computeRefinements(Iteration t, TreeSet<RSKey> queue, Neuron n, RSKey rsk, long v, List<RSKey> outputs, List<RSKey> cleanup) {
        n.lock.acquireWriteLock(t.threadId);
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
                        AndNode.createNextLevelNode(t, rsk.pa, new Refinement(s.key.relativeRid, rsk.offset, s.inputNode), false);

                if(nln != null) {
                    nln.prepareResultsForPredefinedNodes(t, queue, v, outputs, cleanup, n, s, Utils.nullSafeMin(s.key.relativeRid, rsk.offset));
                }
            }
        }
        n.lock.releaseWriteLock();
    }


    protected void prepareResultsForPredefinedNodes(Iteration t, TreeSet<RSKey> queue, long v, List<RSKey> outputs, List<RSKey> cleanup, Neuron n, Synapse s, Integer offset) {
        RSKey rs = new RSKey(this, offset);
        RidVisited nv = getThreadState(t).lookupVisited(offset);
        // TODO: mindestens einen positiven Knoten mit rein nehmen.
        if(computeSynapseWeightSum(offset, n) + n.posRecSum - (n.negDirSum + n.negRecSum) > 0 || !isExpandable(false) || (Math.abs(s.w) / -n.bias) < 0.1) {
            if(nv.outputNode != v) {
                nv.outputNode = v;
                if (isCovered(t, offset, v)) {
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


    public static class RSKey implements Comparable<RSKey> {
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


    public boolean isCovered(Iteration t, Integer offset, long v) {
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


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(level);
        out.writeBoolean(passive);

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

        out.writeBoolean(rangeVisibility != null);
        if(rangeVisibility != null) {
            out.writeUTF(rangeVisibility[0].name());
            out.writeUTF(rangeVisibility[1].name());
        }

        out.writeBoolean(matchRange != null);
        if(matchRange != null) {
            out.writeBoolean(matchRange[0]);
            out.writeBoolean(matchRange[1]);
        }
    }


    @Override
    public void readFields(DataInput in, Iteration t) throws IOException {
        id = in.readInt();
        level = in.readInt();
        passive = in.readBoolean();

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

        if(in.readBoolean()) {
            rangeVisibility = new RangeVisibility[]{RangeVisibility.valueOf(in.readUTF()), RangeVisibility.valueOf(in.readUTF())};
        }
        if(in.readBoolean()) {
            matchRange = new boolean[]{in.readBoolean(), in.readBoolean()};
        }

        threads = new ThreadState[t.m.numberOfThreads];
    }


    public static Node read(DataInput in, Iteration t) throws IOException {
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
        n.readFields(in, t);
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
        public boolean isAllowedOption(Iteration t, Option n, Activation act, long v) {
            return false;
        }

        @Override
        public void cleanup(Iteration t) {}

        @Override
        public void initActivation(Iteration t, Activation act) {}

        @Override
        public void deleteActivation(Iteration t, Activation act) {}

        @Override
        public double computeSynapseWeightSum(Integer offset, Neuron n) {
            return n.bias;
        }

        @Override
        public void propagateAddedActivation(Iteration t, Activation act, Option removedConflict) {}

        @Override
        public void propagateRemovedActivation(Iteration t, Activation act) {}

        @Override
        public String logicToString() {
            return null;
        }

        @Override
        public void apply(Iteration t, Activation act, Option conflict) {}

        @Override
        public void discover(Iteration t, Activation act) {}

        @Override
        protected Set<Refinement> collectNodeAndRefinements(Refinement newRef) { return null; }

        @Override
        protected void changeNumberOfNeuronRefs(Iteration t, long v, int d) {
        }
    }


    public static class Similarity {
        volatile public int frequency;
        public int neuronFreqOffset;
        public int nodeFreqOffset;
    }


    public static class ReverseAndRefinement implements Comparable<ReverseAndRefinement> {
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
