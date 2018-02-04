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
package org.aika.corpus;


import org.aika.*;
import org.aika.lattice.*;
import org.aika.lattice.Node.ThreadState;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.corpus.SearchNode.Weight;
import org.aika.neuron.Synapse;
import org.aika.training.SupervisedTraining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aika.corpus.InterpretationNode.State.SELECTED;
import static org.aika.corpus.InterpretationNode.State.UNKNOWN;

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

    public static boolean APPLY_DEBUG_OUTPUT = false;
    public static boolean OPTIMIZE_DEBUG_OUTPUT = false;
    public static int CLEANUP_INTERVAL = 500;
    public static int MAX_ROUND = 20;

    public final int id;
    private final String content;

    public long visitedCounter = 1;
    public int interpretationIdCounter = 1;
    public int activationIdCounter = 0;
    public int searchNodeIdCounter = 0;
    public int searchStepCounter = 0;

    public InterpretationNode bottom = new InterpretationNode(this, -1, 0, 0, InterpretationNode.State.SELECTED);

    public Model model;
    public int threadId;

    public Queue queue = new Queue();
    public ValueQueue vQueue = new ValueQueue();
    public UpperBoundQueue ubQueue = new UpperBoundQueue();

    public TreeSet<Node> activatedNodes = new TreeSet<>();
    public TreeSet<INeuron> activatedNeurons = new TreeSet<>();
    public TreeSet<INeuron> finallyActivatedNeurons = new TreeSet<>();
    public TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    public TreeMap<INeuron, Set<Synapse>> modifiedWeights = new TreeMap<>();

    public SupervisedTraining supervisedTraining = new SupervisedTraining(this);

    public TreeMap<NodeActivation.Key, Activation> activationsByRid = new TreeMap<>((act1, act2) -> {
        int r = Integer.compare(act1.rid, act2.rid);
        if (r != 0) return r;
        return act1.compareTo(act2);
    });
    public TreeSet<Node> addedNodes = new TreeSet<>();
    public ArrayList<NodeActivation> addedNodeActivations = new ArrayList<>();
    public ArrayList<Activation> addedActivations = new ArrayList<>();


    public SearchNode rootSearchNode = new SearchNode(this, null, null, null, -1, Collections.emptySet());
    public SearchNode selectedSearchNode = null;
    public ArrayList<Candidate> candidates;
    public List<InterpretationNode> bestInterpretation = null;

    public long createV;


    public static Comparator<NodeActivation> ACTIVATIONS_OUTPUT_COMPARATOR = (act1, act2) -> {
        int r = Range.compare(act1.key.range, act2.key.range, false);
        if (r != 0) return r;
        r = Utils.compareInteger(act1.key.rid, act2.key.rid);
        if (r != 0) return r;
        r = act1.key.interpretation.compareTo(act2.key.interpretation);
        if (r != 0) return r;
        return act1.key.node.compareTo(act2.key.node);
    };


    public Document(int id, String content, Model model, int threadId) {
        this.id = id;
        this.content = content;

        this.model = model;
        this.threadId = threadId;
    }


    public String getContent() {
        return content;
    }


    public int length() {
        return content.length();
    }


    public String toString() {
		return content;
	}


    public String getText(Range r) {
        return content.substring(
                Math.max(0, Math.min(r.begin, length())),
                Math.max(0, Math.min(r.end, length()))
        );
    }


    public Stream<Activation> getFinalActivations() {
        return finallyActivatedNeurons.stream()
                .flatMap(in -> in.getFinalActivationsStream(this));
    }


    public Stream<Activation> getActivations() {
        return activatedNeurons.stream()
                .flatMap(in -> in.getActivationsStream(this));
    }


    public String bestInterpretationToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Best Interpretation:\n");
        sb.append(bestInterpretation.toString());
        sb.append("\n");
        return sb.toString();
    }


    @Override
    public int compareTo(Document doc) {
        return Integer.compare(id, doc.id);
    }


    public void propagate() {
        for(Node n: addedNodes) {
            n.reprocessInputs(this);
        }
        addedNodes.clear();

        boolean flag = true;
        while(flag) {
            queue.processChanges();
            flag = ubQueue.process();
        }
    }


    public void generateCandidates() {
        TreeSet<Candidate> tmp = new TreeSet<>();
        int i = 0;
        for (InterpretationNode cn : bottom.children) {
            if (cn.state == UNKNOWN && cn.activation.upperBound > 0.0) {
                tmp.add(new Candidate(cn, i++));
            }
        }

        long v = visitedCounter++;
        markCandidateSelected(bottom, v);

        i = 0;
        candidates = new ArrayList<>();
        while (!tmp.isEmpty()) {
            int oldSize = tmp.size();
            for (Candidate c : tmp) {
                if (c.checkDependenciesSatisfied(v)) {
                    tmp.remove(c);
                    c.id = i++;
                    candidates.add(c);

                    markCandidateSelected(c.refinement, v);
                    break;
                }
            }

            if(tmp.size() == oldSize) {
                log.error("Cycle detected in the activations that is not marked recurrent.");

                throw new RuntimeException("Cycle detected in the activations that is not marked recurrent.");
            }
        }
    }


    private static void markCandidateSelected(InterpretationNode n, long v) {
        if (n.activations != null) {
            for (Activation act : n.activations) {
                act.visited = v;
            }
        }
    }


    /**
     * The method <code>process</code> needs to be called after all the input activations have been added to the
     * network. It performs the search for the best interpretation.
     */
    public void process() {
        inputNeuronActivations.forEach(act -> vQueue.propagateActivationValue(0, act));

        if (Document.OPTIMIZE_DEBUG_OUTPUT) {
            log.info("Root SearchNode:" + toString());
        }

        generateCandidates();

        addedActivations.clear();

        Candidate c = !candidates.isEmpty() ? candidates.get(0) : null;

        SearchNode child = new SearchNode(this, rootSearchNode, null, c, 0, Collections.singleton(bottom));
        SearchNode.searchIterative(this, child);
//        child.searchRecursive(this);

        ArrayList<InterpretationNode> results = new ArrayList<>();
        results.add(bottom);
        if (selectedSearchNode != null) {
            selectedSearchNode.reconstructSelectedResult(this);
            selectedSearchNode.collectResults(results);
        }

        bestInterpretation = results;

        if (Document.OPTIMIZE_DEBUG_OUTPUT) {
            dumpDebugCandidateStatistics();
        }
    }


    public void dumpDebugCandidateStatistics() {
        for (Candidate c : candidates) {
            log.info(c.toString());
        }
    }


    public void notifyWeightsModified(INeuron n, Collection<Synapse> inputSynapses) {
        Set<Synapse> is = modifiedWeights.get(n);
        if(is == null) {
            is = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
            modifiedWeights.put(n, is);
        }
        is.addAll(inputSynapses);
    }


    /**
     * Updates the model after the training step.
     * It applies the weight and bias delta values and reflects the changes in the logic node structure.
     */
    public void commit() {
        modifiedWeights.forEach((n, inputSyns) -> Converter.convert(model, threadId, this, n, inputSyns));
        modifiedWeights.clear();
    }


    /**
     * Removes the activations of this document from the model again.
     */
    public void clearActivations() {
        activatedNeurons.forEach(n -> n.clearActivations(this));
        activatedNodes.forEach(n -> n.clearActivations(this));

        addedActivations.clear();
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
        StringBuilder sb = new StringBuilder();
        finallyActivatedNeurons.stream()
                .filter(n -> n.outputText != null)
                .forEach(n -> {
            for (Activation act : n.getFinalActivations(this)) {
                sb.replace(act.key.range.begin, act.key.range.end, n.outputText);
            }
        });

        return sb.toString();
    }


    public String activationsToString() {
        return activationsToString(false, false);
    }


    public String activationsToString(boolean withTextSnipped, boolean withLogic) {
        return activationsToString(null, withTextSnipped, withLogic);
    }


    public String activationsToString(SearchNode sn, boolean withTextSnippet, boolean withLogic) {
        Set<Activation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        for (INeuron n : activatedNeurons) {
            Stream<Activation> s = Activation.select(this, n, null, null, null, null, InterpretationNode.Relation.CONTAINED_IN);
            acts.addAll(s.collect(Collectors.toList()));
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Activation ID -");
        sb.append((sn != null ? " Interpr. Node State | SequenceNr. |" : ""));
        sb.append(" Range" + (withTextSnippet ? " | Text Snippet" : ""));
        sb.append(" -");
        sb.append(" Interpr. Node -");
        sb.append(" Neuron Label -");
        sb.append((withLogic ? " Logic Layer -" : ""));
        sb.append(" Relational ID (Word Pos.) -");
        sb.append(" Upper Bound -");
        sb.append(" Simulation Rounds [Round | Value | Weight | Norm] -");
        sb.append(" Final Value | Final Weight | Final Norm -");
        sb.append(" Input Value |");
        sb.append(" Target Value");
        sb.append("\n");
        sb.append("\n");

        for(Activation act: acts) {
            if(act.upperBound <= 0.0 && (act.targetValue == null || act.targetValue <= 0.0)) {
                continue;
            }

            sb.append(act.toString(sn, withTextSnippet, withLogic));
            sb.append("\n");
        }

        if(selectedSearchNode != null) {
            sb.append("\n Final SearchNode:" + selectedSearchNode.id + "  WeightSum:" + selectedSearchNode.accumulatedWeight.toString() + "\n");
        }
        return sb.toString();
    }


    public Stream<NodeActivation> getAllActivationsStream() {
        return activatedNodes.stream().flatMap(n -> n.getActivations(this).stream());
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

                if(APPLY_DEBUG_OUTPUT) {
                    log.info("QueueId:" + th.queueId);
                    log.info(n.toString() + "\n");
                    log.info("\n" + activationsToString( true, true));
                }
            }
        }
    }


    public class UpperBoundQueue {
        public final ArrayDeque<Activation> queue = new ArrayDeque<>();


        public void add(Activation act) {
            if(!act.ubQueued) {
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
            for(Activation.SynapseActivation sa: act.neuronOutputs) {
                int r = sa.synapse.key.isRecurrent ? round + 1 : round;
                add(r, sa.output);
            }
        }


        private void addAll(Collection<Activation> acts) {
            for(Activation act: acts) {
                add(0, act);
                for(Activation.SynapseActivation sa: act.neuronOutputs) {
                    if(sa.synapse.key.isRecurrent) {
                        add(0, sa.output);
                    }
                }
            }
        }


        public void add(int round, Activation act) {
            if(act.rounds.isQueued(round) || act.key.interpretation.state == InterpretationNode.State.UNKNOWN) return;

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


        public Weight process(SearchNode sn, Collection<InterpretationNode> changed) {
            long v = visitedCounter++;

            for(InterpretationNode n: changed) {
                addAll(n.getActivations());
            }

            Weight delta = Weight.ZERO;
            for(int round = 0; round < queue.size(); round++) {
                TreeSet<Activation> q = queue.get(round);
                while (!q.isEmpty()) {
                    Activation act = q.pollFirst();
                    act.rounds.setQueued(round, false);

                    delta = delta.add(act.process(sn, round, v));
                }
            }
            return delta;
        }
    }


    public void dumpOscillatingActivations() {
        activatedNeurons.stream()
                .flatMap(n -> n.getAllActivations(this).stream())
                .filter(act -> act.rounds.getLastRound() != null && act.rounds.getLastRound() > MAX_ROUND - 5)
                .forEach(act -> {
                    log.error(act.key + " " + act.key.interpretation.state + " " + act.rounds);
                    log.error(act.linksToString());
                    log.error("");
                });
    }

}
