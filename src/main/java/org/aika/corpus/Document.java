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


import org.aika.Activation;
import org.aika.Model;
import org.aika.Utils;
import org.aika.lattice.AndNode;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The <code>Document</code> class represents a single document which may be either used as processing input or
 * training input. The <code>Document</code> class contains the actual text, the option lattice containing
 * all the possible interpretations of this document.
 *
 * @author Lukas Molzberger
 */
public class Document implements Comparable<Document> {
    public final int id = docIdCounter++;
    public static int docIdCounter = 0;


    public int activationIdCounter = 0;

    private static final Logger log = LoggerFactory.getLogger(Document.class);

    public static boolean APPLY_DEBUG_OUTPUT = false;
    public static boolean OPTIMIZE_DEBUG_OUTPUT = false;
    public static boolean TRAIN_DEBUG_OUTPUT = false;

    public static int CLEANUP_INTERVAL = 20;

    private String content;

    public int optionIdCounter = 1;
    public int expandNodeIdCounter = 0;

    public Option bottom = new Option(this, -1, 0, 0);

    public ExpandNode root = ExpandNode.createInitialExpandNode(this);
    public ExpandNode selectedExpandNode = null;
    public List<Option> selectedOption = null;
    public long selectedMark = -1;


    public Model m;
    public int threadId;
    public long iterationId;
    public boolean interrupted;

    public Queue queue = new Queue();
    public ValueQueue vQueue = new ValueQueue();
    public UpperBoundQueue ubQueue = new UpperBoundQueue();
    public BackPropagationQueue bQueue = new BackPropagationQueue();


    public TreeSet<Node> activatedNodes = new TreeSet<>();
    public TreeSet<Node> activatedNodesForTraining = new TreeSet<>();
    public TreeSet<Neuron> activatedInputNeurons = new TreeSet<>();
    public TreeSet<Neuron> activatedNeurons = new TreeSet<>();
    public TreeSet<Neuron> finallyActivatedNeurons = new TreeSet<>();
    public TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    public TreeSet<Activation> inputNodeActivations = new TreeSet<>();
    public TreeMap<Activation.Key, Activation> activationsByRid = new TreeMap<>(new Comparator<Activation.Key>() {
        @Override
        public int compare(Activation.Key act1, Activation.Key act2) {
            int r = Integer.compare(act1.rid, act2.rid);
            if(r != 0) return r;
            return act1.compareTo(act2);
        }
    });
    public TreeSet<Node> addedNodes = new TreeSet<>();

    public static int numberOfPositionsDelta;

    public int debugActId = -1;
    public double debugActWeight = 0.0;
    public String debugOutput = "";


    public static Comparator<Activation> ACTIVATIONS_OUTPUT_COMPARATOR = new Comparator<Activation>() {
        @Override
        public int compare(Activation act1, Activation act2) {
            int r = Range.compare(act1.key.r, act2.key.r, false);
            if(r != 0) return r;
            r = Utils.compareInteger(act1.key.rid, act2.key.rid);
            if(r != 0) return r;
            r = act1.key.o.compareTo(act2.key.o);
            if(r != 0) return r;
            return Integer.compare(act1.key.n.id, act2.key.n.id);
        }
    };




    public Document(String content, Model m, int threadId, long iterationId) {
        this.content = content;

        this.m = m;
        this.threadId = threadId;
        this.iterationId = iterationId;
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
        return content.substring(Math.max(0, Math.min(r.begin, length())), Math.max(0, Math.min(r.end, length())));
    }


    public String conflictsToString() {
        HashSet<Option> conflicts = new HashSet<>();
        bottom.collectConflicts(conflicts, Option.visitedCounter++);

        StringBuilder sb = new StringBuilder();
        sb.append("Conflicts:\n");
        for(Option n: conflicts) {
            sb.append(n.conflicts.primaryToString());
        }
        sb.append("\n");
        return sb.toString();
    }


    public String selectedOptionsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Selected Options:\n");
        sb.append(selectedOption.toString());
        sb.append("\n");
        return sb.toString();
    }


    public boolean contains(Range r) {
        return r.begin >= 0 && r.end <= length();
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


    public void process() {
        for(Activation act: inputNeuronActivations) {
            vQueue.propagateWeight(0, act, Activation.visitedCounter++);
        }
        root.computeSelectedOption(this);
    }


    public void count() {
        for(Node n: activatedNodes) {
            n.count(this);
        }

        for(Neuron n: finallyActivatedNeurons) {
            n.count(this);
        }
    }


    public void train() {
        m.numberOfPositions += numberOfPositionsDelta;
        numberOfPositionsDelta = 0;

        long v = Node.visitedCounter++;

        count();

        for(Node n: activatedNodes) {
            if(n.neuron instanceof InputNeuron) continue;

            n.computeNullHyp(m);
            if(n.frequencyHasChanged && !n.isBlocked && n.isFrequent()) {
                n.frequencyHasChanged = false;

                if(n instanceof AndNode) {
                    AndNode an = (AndNode) n;
                    an.updateWeight(this, v);
                }

                for(Activation act: n.getThreadState(this).activations.values()) {
                    n.discover(this, act);
                }
            }
        }

        while(true) {
            AndNode n = !m.numberOfPositionsQueue.isEmpty() ? m.numberOfPositionsQueue.iterator().next() : null;

            if(n == null || n.numberOfPositionsNotify > m.numberOfPositions) break;

            n.updateWeight(this, v);
        }

        bQueue.backpropagtion();

        for(Neuron n: finallyActivatedNeurons) {
            if(!n.noTraining) {
                for (Activation act : n.node.getThreadState(this).activations.values()) {
                    n.train(this, act);
                }
            }
        }
    }


    public void clearActivations() {
        for(Node n: activatedNodes) {
            n.clearActivations(this);
        }
        for(Node n: activatedNodesForTraining) {
            n.clearActivations(this);
        }
        activatedNodes.clear();
        addedNodes.clear();

        if(m.lastCleanup[threadId] + CLEANUP_INTERVAL < iterationId) {
            for (Node n : m.allNodes[threadId]) {
                Node.ThreadState th = n.threads[threadId];
                if (th != null && th.lastUsed + CLEANUP_INTERVAL < iterationId) {
                    n.threads[threadId] = null;
                }
            }
        }

        m.docs[threadId] = null;
    }


    public void changeNumberOfPositions(int delta) {
        numberOfPositionsDelta += delta;
    }




    public String networkStateToString(boolean withWeights) {
        return networkStateToString(true, withWeights);
    }


    public String networkStateToString(boolean neuronsOnly, boolean withWeights) {
        Set<Activation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        if(neuronsOnly) {
            for (Neuron n : m.neurons.values()) {
                acts.addAll(Activation.select(this, n.node, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
            }
        } else {
            if(m.initialNodes != null) {
                for (Node n : m.initialNodes.values()) {
                    acts.addAll(Activation.select(this, n, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
                }
            }
            for(int th = 0; th < m.numberOfThreads; th++) {
                for (Node n : m.allNodes[th]) {
                    acts.addAll(Activation.select(this, n, null, null, null, null, Option.Relation.CONTAINED_IN).collect(Collectors.toList()));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        Neuron.NormWeight weightSum = Neuron.NormWeight.ZERO_WEIGHT;
        for(Activation act: acts) {
            sb.append(act.id + " ");
            sb.append(act.key.r);
            sb.append(" - ");

            sb.append(act.key.o);
            sb.append(" - ");

            sb.append(act.key.n);
            sb.append(" - Rid:");
            sb.append(act.key.rid);
            sb.append(" - UB:");
            sb.append(Utils.round(act.upperBound));
            if (withWeights) {
                if(act.key.n instanceof AndNode) {
                    AndNode an = (AndNode) act.key.n;
                    sb.append(" - BW:");
                    sb.append(an.weight);
                }

                sb.append(" - ");
                for(Map.Entry<Integer, Activation.State> me: act.rounds.rounds.entrySet()) {
                    Activation.State s = me.getValue();
                    sb.append("[R:" + me.getKey());
                    sb.append(" V:" + Utils.round(s.value));
                    sb.append(" F:" + s.fired);
                    sb.append(" W:" + Utils.round(s.weight.w));
                    sb.append(" N:" + Utils.round(s.weight.n));
                    sb.append("]");
                }

                if (act.finalState != null && act.finalState.weight != null) {
                    sb.append(" - FV:" + Utils.round(act.finalState.value));
                    sb.append(" FW:" + Utils.round(act.finalState.weight.w));
                    sb.append(" FN:" + Utils.round(act.finalState.weight.n));
                }
            }
            if (act.finalState != null && act.finalState.weight != null) {
                weightSum = weightSum.add(act.finalState.weight);
            }
            sb.append("\n");
        }
        sb.append("\nWeightSum:" + weightSum.toString() + "\n");
        return sb.toString();
    }


    public class Queue {

        public final TreeSet<Node> queue = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                int r = Integer.compare(n1.level, n2.level);
                if(r != 0) return r;
                return Long.compare(n1.queueId, n2.queueId);
            }
        });

        private long queueIdCounter = 0;


        public void add(Node n) {
            if(!n.isQueued) {
                n.isQueued = true;
                n.queueId = queueIdCounter++;
                queue.add(n);
            }
        }


        public void processChanges() {
            while(!queue.isEmpty()) {
                Node n = queue.pollFirst();

                n.isQueued = false;
                n.processChanges(Document.this);

                if(APPLY_DEBUG_OUTPUT) {
                    log.info("QueueId:" + n.queueId);
                    log.info(n.toString() + "\n");
                    log.info("\n" + networkStateToString(false, false));
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

                double oldUpperBound = act.upperBound;

                Neuron n = act.key.n.neuron;

                n.computeBounds(act);

                if(Math.abs(act.upperBound - oldUpperBound) > 0.01) {
                    for(Activation.SynapseActivation sa: act.neuronOutputs) {
                        add(sa.output);
                    }
                }

                if(oldUpperBound <= 0.0 && act.upperBound > 0.0) {
                    for(InputNode out: n.outputNodes.values()) {
                        out.addActivation(Document.this, act);
                    }
                } else if(oldUpperBound > 0.0 && act.upperBound <= 0.0) {
                    for(InputNode out: n.outputNodes.values()) {
                        out.removeActivation(Document.this, act);
                    }
                }
            }
            return flag;
        }

    }


    public class ValueQueue {
        public final TreeSet<VEntry> queue = new TreeSet<>();

        public void propagateWeight(int round, Activation act, long v)  {
            for(Activation.SynapseActivation sa: act.neuronOutputs) {
                int r = sa.s.key.isRecurrent ? round + 1 : round;
                add(r, sa.output, v);
            }
        }


        public Neuron.NormWeight adjustWeight(ExpandNode cand, List<Option> changed) {
            long v = Activation.visitedCounter++;

            for(Option n: changed) {
                addAllActs(n.getNeuronActivations(), v);

                // Does not need to be expanded recursively, because the activation will be propagated anyway.
                if(n.refByOrOption != null) {
                    for (Option on: n.refByOrOption) {
                        addAllActs(on.getNeuronActivations(), v);
                    }
                }
            }

            return processChanges(cand, v);
        }


        private void addAllActs(Collection<Activation> acts, long v) {
            for(Activation act: acts) {
                if(!(act.key.n.neuron instanceof InputNeuron)) {
                    add(0, act, v);
                }
            }
        }


        public void add(int round, Activation act, long v) {
            queue.add(new VEntry(round, act));
        }


        public Neuron.NormWeight processChanges(ExpandNode en, long v) {
            Neuron.NormWeight delta = Neuron.NormWeight.ZERO_WEIGHT;
            while(!queue.isEmpty()) {
                VEntry e = queue.pollFirst();
                int round = e.round;
                Activation act = e.act;

                Activation.State s = act.key.n.neuron.computeWeight(e.round, act, en, Document.this);

                if(OPTIMIZE_DEBUG_OUTPUT) {
                    log.info(act.key + " Round:" + round);
                    log.info("Value:" + s.value + "  Weight:" + s.weight.w + "  Norm:" + s.weight.n + "\n");
                }

                if(round == 0 || !act.rounds.get(round).equalsWithWeights(s)) {
                    ExpandNode.StateChange.saveOldState(en.modifiedActs, act, v);

                    Activation.State oldState = act.rounds.get(round);

                    boolean propagate = act.rounds.set(round, s);

                    ExpandNode.StateChange.saveNewState(act);

                    if(propagate) {
                        propagateWeight(round, act, v);
                    }

                    if(round == 0) {
                        // In case that there is a positive feedback loop.
                        add(1, act, v);
                    }

                    if(act.rounds.getLastRound() != null && round >= act.rounds.getLastRound()) { // Consider only the final round.
                        Neuron.NormWeight oldWeight = oldState.weight;
                        delta = delta.add(s.weight.sub(oldWeight));
                    }

                }
            }
            return delta;
        }
    }

    public static class VEntry implements Comparable<VEntry> {
        public int round;
        public Activation act;


        public VEntry(int round, Activation act) {
            this.round = round;
            this.act = act;
        }

        @Override
        public int compareTo(VEntry ve) {
            int r = Integer.compare(round, ve.round);
            if(r != 0) return r;
            return act.compareTo(ve.act);
        }
    }


    public class BackPropagationQueue {

        public final TreeSet<Activation> queue = new TreeSet<>(new Comparator<Activation>() {
            @Override
            public int compare(Activation act1, Activation act2) {
                Activation.State fs1 = act1.finalState;
                Activation.State fs2 = act2.finalState;

                int r = 0;
                if(fs2 == null && fs1 != null) return -1;
                if(fs2 != null && fs1 == null) return 1;
                if(fs2 != null && fs1 != null) {
                    r = Integer.compare(fs2.fired, fs1.fired);
                }
                if(r != 0) return r;
                return act1.key.compareTo(act2.key);
            }
        });

        private long queueIdCounter = 0;


        public void add(Activation act) {
            if(!act.isQueued && !act.key.n.neuron.noTraining) {
                act.isQueued = true;
                act.queueId = queueIdCounter++;
                queue.add(act);
            }
        }


        public void backpropagtion() {
            while(!queue.isEmpty()) {
                Activation act = queue.pollFirst();

                act.isQueued = false;
                act.key.n.neuron.computeErrorSignal(Document.this, act);
            }
        }
    }

}
