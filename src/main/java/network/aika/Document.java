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
package network.aika;


import network.aika.lattice.Node;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Activation.Link;
import network.aika.neuron.activation.Candidate;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.SearchNode;
import network.aika.lattice.Node.ThreadState;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.SearchNode.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.SearchNode.Decision.SELECTED;
import static network.aika.neuron.activation.SearchNode.Decision.UNKNOWN;


/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * <p>When the document is not needed any more, the method {@code clearActivations} must be called, since Aika only
 * supports a single document per thread and model.
 *
 * @author Lukas Molzberger
 */
public class Document implements Comparable<Document> {
    private static final Logger log = LoggerFactory.getLogger(Document.class);

    public static int CLEANUP_INTERVAL = 500;
    public static int MAX_ROUND = 20;
    public static int ROUND_LIMIT = -1;

    /**
     * Experimental code: not working yet!
     */
    public static boolean INCREMENTAL_MODE = false;

    public final int id;
    private final StringBuilder content;

    private long visitedCounter = 1;
    public int activationIdCounter = 0;
    public int logicNodeActivationIdCounter = 0;
    public int searchNodeIdCounter = 0;
    public int searchStepCounter = 0;
    public int positionIdCounter = 0;

    public Model model;
    private int threadId;

    public Queue queue = new Queue();
    public ValueQueue vQueue = new ValueQueue();
    public UpperBoundQueue ubQueue = new UpperBoundQueue();
    public Linker linker;

    public TreeMap<Integer, Position> positions = new TreeMap<>();
    public TreeSet<Node> activatedNodes = new TreeSet<>();
    public TreeSet<INeuron> activatedNeurons = new TreeSet<>();
    public TreeSet<INeuron> finallyActivatedNeurons = new TreeSet<>();
    public TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    public TreeMap<INeuron, Set<Synapse>> modifiedWeights = new TreeMap<>();


    private TreeMap<ActKey, Activation> activationsBySlotAndPosition = new TreeMap<>((ak1, ak2) -> {
        int r = Integer.compare(ak1.slot, ak2.slot);
        if (r != 0) return r;
        r = Position.compare(ak1.pos, ak2.pos);
        if (r != 0) return r;
        r = ak1.node.compareTo(ak2.node);
        if (r != 0) return r;
        return Integer.compare(ak1.actId, ak2.actId);
    });

    private TreeMap<ActKey, Activation> activationsByPosition = new TreeMap<>((ak1, ak2) -> {
        int r = Position.compare(ak1.pos, ak2.pos);
        if (r != 0) return r;
        r = ak1.node.compareTo(ak2.node);
        if (r != 0) return r;
        return Integer.compare(ak1.actId, ak2.actId);
    });


    public TreeMap<Integer, Activation> activationsById = new TreeMap<>();


    private int lastProcessedActivationId = -1;


    public static class ActKey {
        int slot;
        Position pos;
        Node node;
        int actId;

        public ActKey(int slot, Position pos, Node node, int actId) {
            this.slot = slot;
            this.pos = pos;
            this.node = node;
            this.actId = actId;
        }
    }

    public TreeSet<Node> addedNodes = new TreeSet<>();
    public ArrayList<NodeActivation> addedNodeActivations = new ArrayList<>();


    public SearchNode selectedSearchNode;
    public ArrayList<Candidate> candidates = new ArrayList<>();

    public long createV;


    public static Comparator<Activation> ACTIVATIONS_OUTPUT_COMPARATOR = (act1, act2) -> {
        int r = Position.compare(act1.getSlot(Activation.BEGIN), act2.getSlot(Activation.BEGIN));
        if (r != 0) return r;
        r = act1.getNode().compareTo(act2.getNode());
        if (r != 0) return r;
        return Integer.compare(act1.id, act2.id);
    };


    public Document(int id, String content, Model model, int threadId) {
        this.id = id;
        this.content = new StringBuilder(content);

        this.model = model;
        this.threadId = threadId;
        this.linker = model.getLinkerFactory().createLinker(this);
    }


    public long getVisitedId() {
        return visitedCounter++;
    }

    public int getThreadId() {
        return threadId;
    }

    public void append(String txt) {
        content.append(txt);
    }


    public char charAt(int i) {
        return content.charAt(i);
    }


    public String getContent() {
        return content.toString();
    }


    public int length() {
        return content.length();
    }


    public String toString() {
		return content.toString();
	}


	public Position lookupFinalPosition(int pos) {
        Position p = positions.get(pos);

        if(p == null) {
            p = new Position(this, pos);
            positions.put(pos, p);
        }
        return p;
    }


    public String getText(Position begin, Position end) {
        return getText(begin.getFinalPosition(), end.getFinalPosition());
    }


    public String getText(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }


    public void addActivation(Activation act) {
        for(Map.Entry<Integer, Position> me : act.slots.entrySet()) {
            Position pos = me.getValue();
            if (pos != null && pos.getFinalPosition() != null) {
                ActKey dak = new ActKey(me.getKey(), pos, act.getNode(), act.id);
                activationsBySlotAndPosition.put(dak, act);
                activationsByPosition.put(dak, act);
            }
        }
        activationsById.put(act.id, act);
    }


    public Collection<Activation> getActivations(boolean onlyFinal) {
        if(!onlyFinal) {
            return activationsById.values();
        } else {
            return activationsById
                    .values()
                    .stream()
                    .filter(act -> act.isFinalActivation())
                    .collect(Collectors.toList());
        }
    }


    public Collection<Activation> getActivationsByPosition(int fromSlot, Position fromPos, boolean fromInclusive, int toSlot, Position toPos, boolean toInclusive) {
        return activationsBySlotAndPosition.subMap(
                new Document.ActKey(fromSlot, fromPos, Node.MIN_NODE, Integer.MIN_VALUE),
                fromInclusive,
                new Document.ActKey(toSlot, toPos, Node.MAX_NODE, Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    public Collection<Activation> getActivationsByPosition(Position fromPos, boolean fromInclusive, Position toPos, boolean toInclusive) {
        return activationsByPosition.subMap(
                new Document.ActKey(-1, fromPos, Node.MIN_NODE, Integer.MIN_VALUE),
                fromInclusive,
                new Document.ActKey(-1, toPos, Node.MAX_NODE, Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    public Activation getNextActivation(Activation currentAct) {
        Map.Entry<Integer, Activation> me = currentAct == null ?
                activationsById.firstEntry() :
                activationsById.higherEntry(currentAct.id);
        return me != null ? me.getValue() : null;
    }


    public int getNumberOfActivations() {
        return activationsById.size();
    }


    @Override
    public int compareTo(Document doc) {
        return Integer.compare(id, doc.id);
    }


    public void propagate() {
        boolean flag = true;
        while(flag) {
            queue.processChanges();
            flag = ubQueue.process();
        }
    }


    public void generateCandidates() {
        TreeSet<Candidate> tmp = new TreeSet<>();
        int i = 0;

        if(!INCREMENTAL_MODE) {
            candidates.clear();
        }

        for(Activation act: activationsById.subMap(INCREMENTAL_MODE ? lastProcessedActivationId : -1, false, Integer.MAX_VALUE, true).values()) {
            if (act.decision == UNKNOWN && act.upperBound > 0.0) {
                SearchNode.invalidateCachedDecision(act);
                tmp.add(new Candidate(act, i++));

                lastProcessedActivationId = Math.max(lastProcessedActivationId, act.id);
            }
        }

        long v = visitedCounter++;
        for(Activation act: inputNeuronActivations) {
            act.markedHasCandidate = v;
        }

        while (!tmp.isEmpty()) {
            int oldSize = tmp.size();
            for (Candidate c : tmp) {
                if (c.checkDependenciesSatisfied(v)) {
                    tmp.remove(c);
                    c.id = candidates.size();
                    candidates.add(c);

                    c.activation.markedHasCandidate = v;
                    break;
                }
            }

            if(tmp.size() == oldSize) {
                log.error("Cycle detected in the activations that is not marked recurrent.");

                throw new RuntimeException("Cycle detected in the activations that is not marked recurrent.");
            }
        }
    }


    /**
     * The method <code>process</code> needs to be called after all the input activations have been added to the
     * network. It performs the search for the best interpretation.
     */
    public void process() {
        process(null);
    }


    public void process(Long timeoutInMilliSeconds) throws SearchNode.TimeoutException {
        linker.lateLinking();

        inputNeuronActivations.forEach(act -> vQueue.propagateActivationValue(0, act));

        generateCandidates();

        SearchNode rootNode = null;
        if(selectedSearchNode == null || !INCREMENTAL_MODE) {
            selectedSearchNode = new SearchNode(this, null, null, 0);
            rootNode = selectedSearchNode;
        }

        SearchNode.search(this, selectedSearchNode, visitedCounter++, timeoutInMilliSeconds);

        for(Activation act: activationsById.values()) {
            if(act.isFinalActivation()) {
                finallyActivatedNeurons.add(act.getINeuron());
            }
        }

        if(SearchNode.COMPUTE_SOFT_MAX) {
            SearchNode.computeCachedFactor(rootNode);
            computeSoftMax(rootNode);
        }
    }


    private void computeSoftMax(SearchNode rootNode) {
        for (Activation act : activationsById.values()) {
            double offset = Double.MAX_VALUE;
            for (Activation.Option option : act.options) {
                offset = Math.min(offset, Math.log(option.cacheFactor) + option.weight);
            }

            double norm = 0.0;
            for (Activation.Option option : act.options) {
                norm += Math.exp(Math.log(option.cacheFactor) + option.weight - offset);
            }

            for (Activation.Option option : act.options) {
                if (option.decision == SELECTED) {
                    option.p = Math.exp(Math.log(option.cacheFactor) + option.weight - offset) / norm;
                }
            }
        }
    }


    public void dumpDebugCandidateStatistics() {
        for (Candidate c : candidates) {
            log.info(c.toString());
        }
    }


    public void notifyWeightModified(Synapse synapse) {
        Set<Synapse> is = modifiedWeights.get(synapse.output.get());
        if(is == null) {
            is = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
            modifiedWeights.put(synapse.output.get(), is);
        }
        is.add(synapse);
    }


    /**
     * Updates the model after the training step.
     * It applies the weight and bias delta values and reflects the changes in the logic node structure.
     */
    public void commit() {
        modifiedWeights.forEach((n, inputSyns) -> Converter.convert(threadId, this, n, inputSyns));
        modifiedWeights.clear();
    }


    /**
     * Removes the activations of this document from the model again.
     */
    public void clearActivations() {
        activatedNeurons.forEach(n -> n.clearActivations(this));
        activatedNodes.forEach(n -> n.clearActivations(this));

        activationsById.clear();
        addedNodeActivations.clear();
        activatedNeurons.clear();
        activatedNodes.clear();
        addedNodes.clear();

        if(model.lastCleanup[threadId] + CLEANUP_INTERVAL < id) {
            model.lastCleanup[threadId] = id;

            List<Provider<? extends AbstractNode>> tmp;
            synchronized(model.activeProviders) {
                tmp = new ArrayList<>(model.activeProviders.values());
            }

            tmp.forEach(np -> {
                AbstractNode an = np.getIfNotSuspended();
                if (an != null && an instanceof Node) {
                    Node n = (Node) an;
                    Node.ThreadState th = n.threads[threadId];
                    if (th != null && th.lastUsed + CLEANUP_INTERVAL < id) {
                        n.threads[threadId] = null;
                    }
                }
            });
        }

        model.docs[threadId] = null;
    }


    public String generateOutputText() {
        int oldLength = length();

        TreeSet<Position> queue = new TreeSet<>(Comparator.comparingInt(p -> p.id));

        for(Activation act: activationsById.values()) {
            if(act.getINeuron().getOutputText() != null && act.getSlot(Activation.BEGIN).getFinalPosition() != null && act.getSlot(Activation.END).getFinalPosition() == null) {
                queue.add(act.getSlot(Activation.BEGIN));
            }
        }

        while(!queue.isEmpty()) {
            Position pos = queue.pollFirst();

            pos.getActivations(Activation.BEGIN)
                    .filter(act -> act.getINeuron().getOutputText() != null && act.isFinalActivation())
                    .forEach(act -> {
                        String outText = act.getINeuron().getOutputText();
                        Position nextPos = act.getSlot(Activation.END);
                        nextPos.setFinalPosition(pos.getFinalPosition() + outText.length());

                        content.replace(act.getSlot(Activation.BEGIN).getFinalPosition(), act.getSlot(Activation.END).getFinalPosition(), outText);

                        queue.add(nextPos);
                    });
        }
        return content.substring(oldLength, length());
    }


    public String activationsToString() {
        return activationsToString(false);
    }


    public String activationsToString(boolean withLogic) {
        Set<Activation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        acts.addAll(activationsById.values());

        StringBuilder sb = new StringBuilder();

        sb.append("Id -");

        sb.append(" Decision -");

        sb.append(" Range | Text Snippet");
        sb.append(" | Identity -");
        sb.append(" Neuron Label -");
        sb.append((withLogic ? " Logic Layer -" : ""));
        sb.append(" Upper Bound -");
        sb.append(" Value | Net | Weight -");
        sb.append(" Input Value |");
        sb.append(" Target Value");
        sb.append("\n");
        sb.append("\n");

        for(Activation act: acts) {
            if(act.upperBound <= 0.0 && (act.targetValue == null || act.targetValue <= 0.0)) {
                continue;
            }

            sb.append(act.toString(withLogic));
            sb.append("\n");
        }

        if(selectedSearchNode != null) {
            sb.append("\n Final SearchNode:" + selectedSearchNode.id + "  WeightSum:" + selectedSearchNode.accumulatedWeight + "\n");
        }
        return sb.toString();
    }


    public Stream<NodeActivation> getAllActivationsStream() {
        return addedNodeActivations.stream();
    }


    public class Queue {

        public final TreeSet<Node> queue = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                int r = Integer.compare(n1.level, n2.level);
                if(r != 0) return r;

                ThreadState th1 = n1.getThreadState(threadId, true);
                ThreadState th2 = n2.getThreadState(threadId, true);
                return Long.compare(th1.queueId, th2.queueId);
            }
        });

        private long queueIdCounter = 0;


        public void add(Node n) {
            ThreadState th = n.getThreadState(threadId, true);

            if(!th.isQueued) {
                th.isQueued = true;
                th.queueId = queueIdCounter++;
                queue.add(n);
            }
        }


        public void processChanges() {
            while(!queue.isEmpty()) {
                Node n = queue.pollFirst();
                ThreadState th = n.getThreadState(threadId, true);

                th.isQueued = false;
                n.processChanges(Document.this);
            }
        }
    }


    public class UpperBoundQueue {
        public final ArrayDeque<Activation> queue = new ArrayDeque<>();


        public void add(Link l) {
            if(!l.synapse.isRecurrent) {
                add(l.output);
            }
        }


        public void add(Activation act) {
            if(!act.ubQueued && act.inputValue == null) {
                act.ubQueued = true;
                queue.addLast(act);
            }
        }


        public boolean process() {
            boolean flag = false;
            while(!queue.isEmpty()) {
                flag = true;
                Activation act = queue.pollFirst();
                act.ubQueued = false;

                act.processBounds();
            }
            return flag;
        }
    }


    private static Comparator<Activation> VALUE_QUEUE_COMP = (a, b) -> {
        int r = Integer.compare(a.getSequence(), b.getSequence());
        if(r != 0) return r;
        return Integer.compare(a.id, b.id);
    };


    public class ValueQueue {
        public final ArrayList<TreeSet<Activation>> queue = new ArrayList<>();

        public void propagateActivationValue(int round, Activation act)  {
            act.getOutputLinks()
                    .forEach(l -> add(l.synapse.isRecurrent ? round + 1 : round, l.output));
        }


        private void add(Activation act) {
            add(0, act);
            act.getOutputLinks()
                    .filter(l -> l.synapse.isRecurrent)
                    .forEach(l -> add(0, l.output));
        }


        public void add(int round, Activation act) {
            if(act.rounds.isQueued(round) || act.decision == Decision.UNKNOWN) return;

            TreeSet<Activation> q;
            if(round < queue.size()) {
                q = queue.get(round);
            } else {
                assert round == queue.size();
                q = new TreeSet<>(VALUE_QUEUE_COMP);
                queue.add(q);
            }

            act.rounds.setQueued(round, true);
            q.add(act);
        }


        public double process(SearchNode sn) {
            long v = visitedCounter++;

            if(sn.getParent() != null && sn.getParent().candidate != null) {
                add(sn.getParent().candidate.activation);
            }

            double delta = 0.0;
            for(int round = 0; round < queue.size(); round++) {
                TreeSet<Activation> q = queue.get(round);
                while (!q.isEmpty()) {
                    Activation act = q.pollFirst();
                    act.rounds.setQueued(round, false);

                    delta += act.process(sn, round, v);
                }
            }
            return delta;
        }
    }


    public void dumpOscillatingActivations() {
        activatedNeurons.stream()
                .flatMap(n -> n.getActivations(this, false))
                .filter(act -> act.rounds.getLastRound() != null && act.rounds.getLastRound() > MAX_ROUND - 5)
                .forEach(act -> {
                    log.error(act.id + " " + act.slotsToString() + " " + act.decision + " " + act.rounds);
                    log.error(act.linksToString());
                    log.error("");
                });
    }
}
