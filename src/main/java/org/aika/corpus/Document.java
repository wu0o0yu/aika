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
import org.aika.neuron.Activation.State;
import org.aika.neuron.INeuron;
import org.aika.neuron.INeuron.NormWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public final int id = docIdCounter.addAndGet(1);
    public static AtomicInteger docIdCounter = new AtomicInteger(0);

    public int activationIdCounter = 0;

    private static final Logger log = LoggerFactory.getLogger(Document.class);

    public static boolean APPLY_DEBUG_OUTPUT = false;
    public static boolean OPTIMIZE_DEBUG_OUTPUT = false;

    public static int CLEANUP_INTERVAL = 50;

    public static int MAX_ROUND = 20;

    private String content;

    public int visitedCounter = 1;
    public int interprIdCounter = 1;
    public int searchNodeIdCounter = 0;

    public InterprNode bottom = new InterprNode(this, -1, 0, 0);

    public SearchNode selectedSearchNode = null;
    public List<InterprNode> bestInterpretation = null;

    public Model m;
    public int threadId;
    public boolean interrupted;

    public Queue queue = new Queue();
    public ValueQueue vQueue = new ValueQueue();
    public UpperBoundQueue ubQueue = new UpperBoundQueue();
    public BackPropagationQueue bQueue = new BackPropagationQueue();


    public TreeSet<Node> activatedNodes = new TreeSet<>();
    public TreeSet<Node> activatedNodesForTraining = new TreeSet<>();
    public TreeSet<INeuron> activatedNeurons = new TreeSet<>();
    public TreeSet<INeuron> finallyActivatedNeurons = new TreeSet<>();
    public TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    public TreeMap<NodeActivation.Key, NodeActivation> activationsByRid = new TreeMap<>(new Comparator<NodeActivation.Key>() {
        @Override
        public int compare(NodeActivation.Key act1, NodeActivation.Key act2) {
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

    public static Comparator<NodeActivation> ACTIVATIONS_OUTPUT_COMPARATOR = new Comparator<NodeActivation>() {
        @Override
        public int compare(NodeActivation act1, NodeActivation act2) {
            int r = Range.compare(act1.key.r, act2.key.r, false);
            if(r != 0) return r;
            r = Utils.compareInteger(act1.key.rid, act2.key.rid);
            if(r != 0) return r;
            r = act1.key.o.compareTo(act2.key.o);
            if(r != 0) return r;
            return act1.key.n.compareTo(act2.key.n);
        }
    };


    public Document(String content, Model m, int threadId) {
        this.content = content;

        this.m = m;
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
        boolean flag = true;
        while(flag) {
            queue.processChanges();
            flag = ubQueue.process();
        }
    }


    /**
     * The method <code>process</code> needs to be called after all the input activations have been added to the
     * network. It performs the search for the best interpretation.
     */
    public void process() {
        for(Activation act: inputNeuronActivations) {
            vQueue.propagateWeight(0, act);
        }
        interrupted = false;
        SearchNode root = new SearchNode(this, null, null, null, -1, Collections.emptyList());
        root.computeBestInterpretation(this);
    }


    public void train(TrainConfig trainConfig) {
        for(Node n: activatedNodes) {
            trainConfig.counter.count(this, n);

            if(trainConfig.checkExpandable.evaluate(n)) {
                ThreadState<?, NodeActivation<?>> th = n.getThreadState(threadId, false);
                if(th != null) {
                    for (NodeActivation act : th.activations.values()) {
                        n.discover(this, act, trainConfig);
                    }
                }
            }
        }

        bQueue.backpropagtion();

        for (INeuron n : finallyActivatedNeurons) {
            ThreadState<OrNode, Activation> th = n.node.get().getThreadState(threadId, false);
            if (th != null) {
                for (Activation act : th.activations.values()) {
                    n.train(this, act, trainConfig.learnRate, trainConfig.synapseEvaluation);
                }
            }
        }
    }

    /**
     * Removes the activations of this document from the model again.
     */

    // TODO: don't use providersInMemory
    public void clearActivations() {
        for(Node n: activatedNodes) {
            n.clearActivations(this);
        }
        for(Node n: activatedNodesForTraining) {
            n.clearActivations(this);
        }
        activatedNodes.clear();
        addedNodes.clear();

        if(m.lastCleanup[threadId] + CLEANUP_INTERVAL < id) {
            m.lastCleanup[threadId] = id;

            List<Provider<? extends AbstractNode>> tmp;
            synchronized(m.activeProviders) {
                tmp = new ArrayList<>(m.activeProviders.values());
            }

            for (Provider<? extends AbstractNode> np : tmp) {
                if (np != null) {
                    AbstractNode an = np.getIfNotSuspended();
                    if (an != null && an instanceof Node) {
                        Node n = (Node) an;
                        Node.ThreadState th = n.threads[threadId];
                        if (th != null && th.lastUsed + CLEANUP_INTERVAL < id) {
                            n.threads[threadId] = null;
                        }
                    }
                }
            }
        }

        m.docs[threadId] = null;
    }


    public void changeNumberOfPositions(int delta) {
        numberOfPositionsDelta += delta;
    }


    public String generateOutputText() {
        StringBuilder sb = new StringBuilder();
        for(INeuron n: finallyActivatedNeurons) {
            if(n.outputText != null) {
                for (Activation act : n.getFinalActivations(this)) {
                    sb.replace(act.key.r.begin, act.key.r.end, n.outputText);
                }
            }
        }

        return sb.toString();
    }


    public String neuronActivationsToString(boolean withWeights) {
        return neuronActivationsToString(withWeights, false, false);
    }


    public String neuronActivationsToString(boolean withWeights, boolean withTextSnipped, boolean withLogic) {
        Set<Activation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        for (INeuron n : activatedNeurons) {
            Stream<Activation> s = NodeActivation.select(this, n.node.get(), null, null, null, null, null, InterprNode.Relation.CONTAINED_IN);
            acts.addAll(s.collect(Collectors.toList()));
        }

        StringBuilder sb = new StringBuilder();
        for(Activation act: acts) {
            if(act.upperBound <= 0.0) {
                continue;
            }

            sb.append(act.id + " ");
            sb.append(act.key.r);
            if(withTextSnipped) {
                sb.append(" ");
                if(act.key.n.neuron.get().outputText != null) {
                    sb.append(collapseText(act.key.n.neuron.get().outputText));
                } else {
                    sb.append(collapseText(getText(act.key.r)));
                }
            }
            sb.append(" - ");

            sb.append(act.key.o);
            sb.append(" - ");

            sb.append(withLogic ? act.key.n.toString() : act.key.n.getNeuronLabel());
            sb.append(" - Rid:");
            sb.append(act.key.rid);
            sb.append(" - UB:");
            sb.append(Utils.round(act.upperBound));
            if (withWeights) {
                sb.append(" - ");
                for(Map.Entry<Integer, Activation.State> me: act.rounds.rounds.entrySet()) {
                    Activation.State s = me.getValue();
                    sb.append("[R:" + me.getKey());
                    sb.append(" VALUE:" + Utils.round(s.value));
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
            sb.append("\n");
        }

        if(selectedSearchNode != null) {
            sb.append("\n Final SearchNode:" + selectedSearchNode.id + "  WeightSum:" + selectedSearchNode.accumulatedWeight.toString() + "\n");
        }
        return sb.toString();
    }


    public String nodeActivationsToString(boolean withTextSnipped, boolean withLogic) {
        Set<NodeActivation> acts = new TreeSet<>(ACTIVATIONS_OUTPUT_COMPARATOR);

        for(Node<?, NodeActivation<?>> n: activatedNodes) {
            acts.addAll(NodeActivation.select(this, n, null, null, null, null, null, InterprNode.Relation.CONTAINED_IN).collect(Collectors.toList()));
        }
        StringBuilder sb = new StringBuilder();
        for(NodeActivation act: acts) {
            sb.append(act.id + " ");
            sb.append(act.key.r);
            if(withTextSnipped) {
                sb.append(" ");
                sb.append(collapseText(getText(act.key.r)));
            }
            sb.append(" - ");

            sb.append(act.key.o);
            sb.append(" - ");

            sb.append(withLogic ? act.key.n.toString() : act.key.n.getNeuronLabel());
            sb.append(" - Rid:");
            sb.append(act.key.rid);
            sb.append("\n");
        }
        return sb.toString();
    }


    private String collapseText(String txt) {
        if (txt.length() <= 10) {
            return txt;
        } else {
            return txt.substring(0, 5) + "..." + txt.substring(txt.length() - 5);
        }
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
                    log.info("\n" + nodeActivationsToString( true, false));
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

                double oldUpperBound = act.isInput ? 0.0 : act.upperBound;

                INeuron n = act.key.n.neuron.get();

                if(!act.isInput) {
                    n.computeBounds(act);
                }

                if(Math.abs(act.upperBound - oldUpperBound) > 0.01) {
                    for(Activation.SynapseActivation sa: act.neuronOutputs) {
                        add(sa.output);
                    }
                }

                if(oldUpperBound <= 0.0 && act.upperBound > 0.0) {
                    for(Provider<InputNode> out: n.outputNodes.values()) {
                        out.get().addActivation(Document.this, act);
                    }
                } else if(oldUpperBound > 0.0 && act.upperBound <= 0.0) {
                    for(Provider<InputNode> out: n.outputNodes.values()) {
                        out.get().removeActivation(Document.this, act);
                    }
                }
            }
            return flag;
        }

    }


    public class ValueQueue {
        public final ArrayList<ArrayDeque<Activation>> queue = new ArrayList<>();

        public void propagateWeight(int round, Activation act)  {
            for(Activation.SynapseActivation sa: act.neuronOutputs) {
                int r = sa.s.key.isRecurrent ? round + 1 : round;
                add(r, sa.output);
            }
        }


        public NormWeight adjustWeight(SearchNode cand, List<InterprNode> changed) {
            long v = NodeActivation.visitedCounter++;

            for(InterprNode n: changed) {
                addAllActs(n.getNeuronActivations());

                // Does not need to be expanded recursively, because the activation will be propagated anyway.
                if(n.refByOrInterprNode != null) {
                    for (InterprNode on: n.refByOrInterprNode) {
                        addAllActs(on.getNeuronActivations());
                    }
                }
            }

            return processChanges(cand, v);
        }


        private void addAllActs(Collection<Activation> acts) {
            for(Activation act: acts) {
                add(0, act);
                for(Activation.SynapseActivation sa: act.neuronOutputs) {
                    if(sa.s.key.isRecurrent) {
                        add(0, sa.output);
                    }
                }
            }
        }


        public void add(int round, Activation act) {
            if(act.rounds.isQueued(round)) return;

            ArrayDeque<Activation> q;
            if(round < queue.size()) {
                q = queue.get(round);
            } else {
                assert round == queue.size();
                q = new ArrayDeque<>();
                queue.add(q);
            }

            act.rounds.setQueued(round, true);
            q.addLast(act);
        }


        public INeuron.NormWeight processChanges(SearchNode sn, long v) {
            NormWeight delta = NormWeight.ZERO_WEIGHT;
            for(int round = 0; round < queue.size(); round++) {
                ArrayDeque<Activation> q = queue.get(round);
                while (!q.isEmpty()) {
                    Activation act = q.pollLast();
                    act.rounds.setQueued(round, false);

                    State s = act.isInput ? act.finalState : act.key.n.neuron.get().computeWeight(round, act, sn, Document.this);

                    if (OPTIMIZE_DEBUG_OUTPUT) {
                        log.info(act.key + " Round:" + round);
                        log.info("Value:" + s.value + "  Weight:" + s.weight.w + "  Norm:" + s.weight.n + "\n");
                    }

                    if (round == 0 || !act.rounds.get(round).equalsWithWeights(s)) {
                        SearchNode.StateChange.saveOldState(sn.modifiedActs, act, v);

                        State oldState = act.rounds.get(round);

                        boolean propagate = act.rounds.set(round, s);

                        SearchNode.StateChange.saveNewState(act);

                        if (propagate) {
                            if(round > MAX_ROUND) {
                                log.error("Maximum number of rounds reached.");
                                sn.dumpDebugState();
                            } else {
                                propagateWeight(round, act);
                            }
                        }

                        if (round == 0) {
                            // In case that there is a positive feedback loop.
                            add(1, act);
                        }

                        if (act.rounds.getLastRound() != null && round >= act.rounds.getLastRound()) { // Consider only the final round.
                            delta = delta.add(s.weight.sub(oldState.weight));
                        }
                    }
                }
            }
            return delta;
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
            if(!act.isQueued) {
                act.isQueued = true;
                act.queueId = queueIdCounter++;
                queue.add(act);
            }
        }


        public void backpropagtion() {
            while(!queue.isEmpty()) {
                Activation act = queue.pollFirst();

                act.isQueued = false;
                act.key.n.neuron.get().computeErrorSignal(Document.this, act);
            }
        }
    }
}
