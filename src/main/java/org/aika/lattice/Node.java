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
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.lattice.OrNode.OrEntry;
import org.aika.neuron.INeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


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
 * <p>
 * <p>Each logic node has a set of activations. The activations are stored in the thread local data structure
 * {@code ThreadState}.
 *
 * @author Lukas Molzberger
 */
public abstract class Node<T extends Node, A extends NodeActivation<T>> extends AbstractNode<Provider<T>> implements Comparable<Node> {
    public static int MAX_RELATIVE_RID = 25;

    public static final Node MIN_NODE = new InputNode();
    public static final Node MAX_NODE = new InputNode();

    private static final Logger log = LoggerFactory.getLogger(Node.class);

    public TreeMap<ReverseAndRefinement, Refinement> reverseAndChildren;
    public TreeMap<Refinement, Provider<AndNode>> andChildren;
    public TreeSet<OrEntry> orChildren;
    public TreeSet<OrEntry> allOrChildren;

    public int level;

    public Writable statistic;

    // Prevents this node from being removed during cleanup.
    public boolean isDiscovered;

    public AtomicInteger numberOfNeuronRefs = new AtomicInteger(0);
    volatile boolean isRemoved;

    // Only the children maps are locked.
    public ReadWriteLock lock = new ReadWriteLock();

    public ThreadState<T, A>[] threads;

    /**
     * The {@code ThreadState} is a thread local data structure containing the activations of a single document for
     * a specific logic node.
     */
    public static class ThreadState<T extends Node, A extends NodeActivation<T>> {
        public long lastUsed;

        public NavigableMap<Key, Set<NodeActivation<?>>> added;

        public long visited;

        public boolean isQueued = false;
        public long queueId;

        private RidVisited nullRidVisited;
        private RidVisited[] ridVisited = new RidVisited[2 * MAX_RELATIVE_RID];

        public ThreadState() {
            added = new TreeMap<>();
        }


        public RidVisited lookupVisited(Integer offset) throws RidOutOfRange {
            if (offset != null && (offset >= MAX_RELATIVE_RID || offset <= -MAX_RELATIVE_RID)) {
                log.warn("RID too large:" + offset);
                throw new RidOutOfRange("RID too large:" + offset);
            }

            if (offset == null) {
                if (nullRidVisited == null) {
                    nullRidVisited = new RidVisited();
                }
                return nullRidVisited;
            } else {
                RidVisited v = ridVisited[offset + MAX_RELATIVE_RID];
                if (v == null) {
                    v = new RidVisited();
                    ridVisited[offset + MAX_RELATIVE_RID] = v;
                }
                return v;
            }
        }

        public static class RidOutOfRange extends Exception {
            public RidOutOfRange(String s) {
                super(s);
            }
        }
    }


    /**
     * Aika extensively uses graph coloring techniques. When traversing the logic node lattice, nodes will be
     * marked in order to avoid having to visit the same node twice. To avoid having to reset each mark Aika uses the
     * counter {@code Node.visitedCounter} to set a new mark each time.
     */
    public static class RidVisited {
        public long computeParents = -1;
    }


    public ThreadState<T, A> getThreadState(int threadId, boolean create) {
        ThreadState<T, A> th = threads[threadId];
        if (th == null) {
            if (!create) return null;

            th = new ThreadState();
            threads[threadId] = th;
        }
        th.lastUsed = provider.model.docIdCounter.get();
        return th;
    }


    abstract A createActivation(Document doc, Key ak);

    /**
     * Propagate an activation to the next node or the next neuron that is depending on the current node.
     *
     * @param doc
     * @param act
     */
    public abstract void propagateAddedActivation(Document doc, A act);

    public abstract boolean isAllowedOption(int threadId, InterprNode n, NodeActivation<?> act, long v);

    public abstract double computeSynapseWeightSum(Integer offset, INeuron n);

    abstract void apply(Document doc, A act);

    public abstract void discover(Document doc, NodeActivation<T> act, Config config);

    abstract Collection<Refinement> collectNodeAndRefinements(Refinement newRef);

    abstract boolean contains(Refinement ref);

    public abstract void cleanup();

    public abstract String logicToString();


    protected Node() {
    }


    public Node(Model m, int level) {
        threads = new ThreadState[m.numberOfThreads];
        provider = new Provider(m, this);
        this.level = level;
        setModified();

        if(m.nodeStatisticFactory != null) {
            statistic = m.nodeStatisticFactory.createStatisticObject();
        }
    }


    public static final Comparator<NodeActivation.Key> BEGIN_COMP = (k1, k2) -> {
        int r;
        r = Range.compare(k1.range, k2.range, false);
        if (r != 0) return r;
        r = Utils.compareInteger(k1.rid, k2.rid);
        if (r != 0) return r;
        return InterprNode.compare(k1.interpretation, k2.interpretation);
    };


    public static final Comparator<NodeActivation.Key> END_COMP = (k1, k2) -> {
        int r;
        r = Range.compare(k1.range, k2.range, true);
        if (r != 0) return r;
        r = Utils.compareInteger(k1.rid, k2.rid);
        if (r != 0) return r;
        return InterprNode.compare(k1.interpretation, k2.interpretation);
    };


    public static final Comparator<NodeActivation.Key> RID_COMP = (k1, k2) -> {
        int r;
        r = Utils.compareInteger(k1.rid, k2.rid);
        if (r != 0) return r;
        r = Range.compare(k1.range, k2.range, false);
        if (r != 0) return r;
        return InterprNode.compare(k1.interpretation, k2.interpretation);
    };


    void addOrChild(OrEntry oe, boolean all) {
        lock.acquireWriteLock();
        if(all) {
            if (allOrChildren == null) {
                allOrChildren = new TreeSet<>();
            }
            allOrChildren.add(oe);
        } else {
            if (orChildren == null) {
                orChildren = new TreeSet<>();
            }
            orChildren.add(oe);
        }
        lock.releaseWriteLock();
    }


    void removeOrChild(OrEntry oe, boolean all) {
        lock.acquireWriteLock();
        if(all) {
            if (allOrChildren != null) {
                allOrChildren.remove(oe);
                if (allOrChildren.isEmpty()) {
                    allOrChildren = null;
                }
            }
        } else {
            if (orChildren != null) {
                orChildren.remove(oe);
                if (orChildren.isEmpty()) {
                    orChildren = null;
                }
            }
        }
        lock.releaseWriteLock();
    }


    void addAndChild(Refinement ref, Provider<AndNode> child) {
        if (andChildren == null) {
            andChildren = new TreeMap<>();
            reverseAndChildren = new TreeMap<>();
        }

        Provider<AndNode> n = andChildren.put(ref, child);
        assert n == null;
        reverseAndChildren.put(new ReverseAndRefinement(child, ref.rid, 0), ref);
    }


    void removeAndChild(Refinement ref) {
        if (andChildren != null) {
            Provider<AndNode> child = andChildren.remove(ref);
            reverseAndChildren.remove(new ReverseAndRefinement(child, ref.rid, 0));

            if (andChildren.isEmpty()) {
                andChildren = null;
                reverseAndChildren = null;
            }
        }
    }


    A processAddedActivation(Document doc, Key<T> ak, Collection<NodeActivation> inputActs) {
        if (Document.APPLY_DEBUG_OUTPUT) {
            log.info("add: " + ak + " - " + ak.node);
        }

        A act = createActivation(doc, ak);

        register(act, doc);

        act.link(inputActs);

        propagateAddedActivation(doc, act);

        return act;
    }


    public void register(A act, Document doc) {
        Key ak = act.key;

        if (ak.interpretation.activations == null) {
            ak.interpretation.activations = new TreeMap<>();
        }
        ak.interpretation.activations.put(ak, act);
    }


    /**
     * Process all added or removed activation for this logic node.
     *
     * @param doc
     */
    public void processChanges(Document doc) {
        ThreadState th = getThreadState(doc.threadId, true);
        NavigableMap<Key<T>, Collection<NodeActivation>> tmpAdded = th.added;

        th.added = new TreeMap<>();

        tmpAdded.forEach((ak, iActs) -> processAddedActivation(doc, ak, iActs));
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
    public static <T extends Node, A extends NodeActivation<T>> void addActivationAndPropagate(Document doc, Key<T> ak, Collection<NodeActivation<?>> inputActs) {
        ThreadState<T, A> th = ak.node.getThreadState(doc.threadId, true);
        Set<NodeActivation<?>> iActs = th.added.get(ak);
        if (iActs == null) {
            iActs = new TreeSet<>();
            th.added.put(ak, iActs);
        }
        iActs.addAll(inputActs);
        doc.queue.add(ak.node);
    }


    boolean computeAndParents(Model m, int threadId, Integer offset, SortedSet<Refinement> inputs, Map<Refinement, Provider<? extends Node>> parents, Config config, long v) throws ThreadState.RidOutOfRange {
        RidVisited nv = getThreadState(threadId, true).lookupVisited(offset);
        if (nv.computeParents == v) return true;
        nv.computeParents = v;

        if (inputs.size() == 1) {
            parents.put(inputs.first(), provider);
            return true;
        }

        for (Refinement ref : inputs) {
            SortedSet<Refinement> childInputs = new TreeSet<>(inputs);
            childInputs.remove(ref);

            Refinement nRef = new Refinement(ref.getRelativePosition(), offset, ref.input);
            lock.acquireReadLock();
            Provider<AndNode> cp = andChildren != null ? andChildren.get(nRef) : null;
            lock.releaseReadLock();

            if (cp == null) {
                if (config != null) return false;
                cp = AndNode.createNextLevelNode(m, threadId, this, nRef, config).provider;
                if (cp == null) return false;
            }

            Integer nOffset = Utils.nullSafeMin(ref.getRelativePosition(), offset);
            if (!cp.get().computeAndParents(m, threadId, nOffset, childInputs, parents, config, v)) {
                return false;
            }
        }
        return true;
    }


    public void remove() {
        assert !isRemoved;

        lock.acquireWriteLock();
        setModified();
        while (andChildren != null && !andChildren.isEmpty()) {
            andChildren.firstEntry().getValue().get().remove();
        }

        while (orChildren != null && !orChildren.isEmpty()) {
            orChildren.pollFirst().node.get().remove();
        }
        lock.releaseWriteLock();

        isRemoved = true;
    }


    Provider<AndNode> getAndChild(Refinement ref) {
        lock.acquireReadLock();
        Provider<AndNode> result = andChildren != null ? andChildren.get(ref) : null;
        lock.releaseReadLock();
        return result;
    }


    public boolean isRequired() {
        return numberOfNeuronRefs.get() > 0 || isDiscovered;
    }


    public void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        ThreadState th = getThreadState(threadId, true);
        if (th.visited == v) return;
        th.visited = v;
        numberOfNeuronRefs.addAndGet(d);
    }


    public String getNeuronLabel() {
        return "";
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getNeuronLabel());
        sb.append(" - ");
        sb.append(logicToString());
        sb.append(" - ");
        sb.append(weightsToString());
        return sb.toString();
    }


    public String weightsToString() {
        return "";
    }


    public int compareTo(Node n) {
        if (this == n) return 0;
        if (this == MIN_NODE) return -1;
        if (n == MIN_NODE) return 1;
        if (this == MAX_NODE) return 1;
        if (n == MAX_NODE) return -1;

        return provider.compareTo(n.provider);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(level);

        out.writeBoolean(statistic != null);
        if(statistic != null) {
            statistic.write(out);
        }

        out.writeBoolean(isDiscovered);

        out.writeInt(numberOfNeuronRefs.get());

        if (andChildren != null) {
            out.writeInt(andChildren.size());
            for (Map.Entry<Refinement, Provider<AndNode>> me : andChildren.entrySet()) {
                me.getKey().write(out);
                out.writeInt(me.getValue().id);
            }
        } else {
            out.writeInt(0);
        }

        if (orChildren != null) {
            out.writeInt(orChildren.size());
            for (OrEntry oe : orChildren) {
                oe.write(out);
            }
        } else {
            out.writeInt(0);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        level = in.readInt();

        if(in.readBoolean()) {
            statistic = m.nodeStatisticFactory.createStatisticObject();
            statistic.readFields(in, m);
        }

        isDiscovered = in.readBoolean();

        numberOfNeuronRefs.set(in.readInt());

        int s = in.readInt();
        for (int i = 0; i < s; i++) {
            addAndChild(Refinement.read(in, m), m.lookupNodeProvider(in.readInt()));
        }

        s = in.readInt();
        for (int i = 0; i < s; i++) {
            if (orChildren == null) {
                orChildren = new TreeSet<>();
            }
            orChildren.add(OrEntry.read(in, m));
        }

        threads = new ThreadState[m.numberOfThreads];
    }


    public static Node readNode(DataInput in, Provider p) throws IOException {
        char type = in.readChar();
        Node n = null;
        switch (type) {
            case 'I':
                n = new InputNode();
                break;
            case 'A':
                n = new AndNode();
                break;
            case 'O':
                n = new OrNode();
                break;
        }
        n.provider = p;

        n.readFields(in, p.model);
        return n;
    }


    static class ReverseAndRefinement implements Comparable<ReverseAndRefinement> {
        boolean dir;
        Provider node;

        public ReverseAndRefinement(Provider n, Integer a, Integer b) {
            this.node = n;
            this.dir = Utils.compareNullSafe(a, b);
        }

        @Override
        public int compareTo(ReverseAndRefinement rar) {
            int r = node.compareTo(rar.node);
            if (r != 0) return r;
            return Boolean.compare(dir, rar.dir);
        }
    }
}
