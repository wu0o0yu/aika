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


import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Activation.OscillatingActivationsException;
import network.aika.neuron.activation.link.Linker;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;
import network.aika.neuron.activation.search.SearchNode.TimeoutException;
import network.aika.neuron.activation.*;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.search.Decision.UNKNOWN;


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

    private final int id;
    private final StringBuilder content;

    private long visitedCounter = 1;
    private int activationIdCounter = 0;
    private int nodeActivationIdCounter = 0;
    public int searchNodeIdCounter = 0;
    public int searchStepCounter = 0;

    private Model model;
    private int threadId;

    private ValueQueue valueQueue = new ValueQueue();
    private UpperBoundQueue ubQueue = new UpperBoundQueue();
    private Linker linker;

    private TreeSet<INeuron> activatedNeurons = new TreeSet<>();
    private TreeSet<INeuron> finallyActivatedNeurons = new TreeSet<>();
    private TreeSet<Activation> inputNeuronActivations = new TreeSet<>();
    private TreeMap<INeuron, Set<Synapse>> modifiedWeights = new TreeMap<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();


    public SearchNode selectedSearchNode;
    public ArrayList<Activation> candidates = new ArrayList<>();

    public long createV;


    public Document(Model model, String content) {
        this(model, content, 0);
    }


    public Document(Model model, String content, int threadId) {
        this(model, model.getNewDocumentId(), content, threadId);
    }


    public Document(Model model, int id, String content, int threadId) {
        this.id = id;
        this.content = new StringBuilder(content);

        this.model = model;
        this.threadId = threadId;
        this.linker = initLinker();

        model.acquireThread(threadId, this);
    }


    protected Linker initLinker() {
        return new Linker(this);
    }


    public int getId() {
        return id;
    }


    public Model getModel() {
        return model;
    }


    public Linker getLinker() {
        return linker;
    }


    public ValueQueue getValueQueue() {
        return valueQueue;
    }

    public long getNewVisitedId() {
        return visitedCounter++;
    }


    public int getNewActivationId() {
        return activationIdCounter++;
    }

    public int getNewNodeActivationId() {
        return nodeActivationIdCounter++;
    }


    public void addInputNeuronActivation(Activation act) {
        inputNeuronActivations.add(act);
    }


    public void addFinallyActivatedNeuron(INeuron n) {
        finallyActivatedNeurons.add(n);
    }


    public void addActivatedNeuron(INeuron n) {
        activatedNeurons.add(n);
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


    public UpperBoundQueue getUpperBoundQueue() {
        return ubQueue;
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
        activationsById.put(act.getId(), act);
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


    public Activation getNextActivation(Activation currentAct) {
        Map.Entry<Integer, Activation> me = currentAct == null ?
                activationsById.firstEntry() :
                activationsById.higherEntry(currentAct.getId());
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
        while(ubQueue.process()) {}
    }


    public void generateCandidates() throws CyclicDependencyException {
        TreeSet<Activation> tmp = new TreeSet<>(CANDIDATE_COMP);
        int i = 0;

        for (Activation act : activationsById.values()) {
            if (act.getType() == EXCITATORY && act.getDecision() == UNKNOWN && act.getUpperBound() > 0.0) {
                act.setCandidateId(i++);
                tmp.add(act);
            }
        }

        long v = visitedCounter++;
        for(Activation act: inputNeuronActivations) {
            act.markHasCandidate(v);
        }

        while (!tmp.isEmpty()) {
            int oldSize = tmp.size();
            for (Activation act : tmp) {
                if (act.checkDependenciesSatisfied(v)) {
                    tmp.remove(act);
                    act.setCandidateId(candidates.size());
                    candidates.add(act);

                    act.markHasCandidate(v);
                    break;
                }
            }

            if(tmp.size() == oldSize) {
                log.info(activationsToString());
                throw new CyclicDependencyException();
            }
        }
    }


    /**
     * The method <code>process</code> needs to be called after all the input activations have been added to the
     * network. It performs the search for the best interpretation.
     */
    public void process() throws TimeoutException, CyclicDependencyException, OscillatingActivationsException {
        process(null);
    }


    public void process(Long timeoutInMilliSeconds) throws TimeoutException, CyclicDependencyException, OscillatingActivationsException {
        linker.lateLinking();

        inputNeuronActivations.forEach(act -> {
            valueQueue.propagateActivationValue(act, null, true, true);
        });

        generateCandidates();

        selectedSearchNode = new SearchNode(this, null, null, 0);
        selectedSearchNode.updateActivations(this);
        storeFinalState();

        SearchNode rootNode = selectedSearchNode;

        SearchNode.search(this, selectedSearchNode, visitedCounter++, timeoutInMilliSeconds);

        for(Activation act: activationsById.values()) {
            if(act.isFinalActivation()) {
                finallyActivatedNeurons.add(act.getINeuron());
            }
        }

        if(SearchNode.COMPUTE_SOFT_MAX) {
            SearchNode.computeCachedFactor(rootNode);
            computeOptionProbabilities();
        }
    }


    public void storeFinalState() {
        for(Activation act: activationsById.values()) {
            act.finalOption = act.currentOption;
        }
    }


    private void computeOptionProbabilities() {
        for (Activation act : activationsById.values()) {
            act.computeOptionProbabilities();
        }
    }


    public void dumpDebugCandidateStatistics() {
        for (Activation act : candidates) {
            log.info(act.searchStateToString());
        }
    }


    public void notifyWeightModified(Synapse synapse) {
        Set<Synapse> is = modifiedWeights.get(synapse.getOutput().get());
        if(is == null) {
            is = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
            modifiedWeights.put(synapse.getOutput().get(), is);
        }
        is.add(synapse);
    }


    public Map<INeuron, Set<Synapse>> getModifiedWeights() {
        return modifiedWeights;
    }

    /**
     * Updates the model after the training step.
     * It applies the weight and bias delta values and reflects the changes in the logic node structure.
     */
    public void commit() {
        modifiedWeights.forEach((n, inputSyns) -> {
            n.commit(inputSyns);
        });
        modifiedWeights.clear();
    }


    /**
     * Removes the activations of this document from the model again.
     */
    public void clearActivations() {
        activatedNeurons.forEach(n -> n.clearActivations(this));

        activationsById.clear();
        activatedNeurons.clear();

        if(model.lastCleanup[threadId] + CLEANUP_INTERVAL < id) {
            model.lastCleanup[threadId] = id;
        }

        model.docs[threadId] = null;
    }



    public String activationsToString() {
        Set<Activation> acts = new TreeSet<>();

        acts.addAll(activationsById.values());

        StringBuilder sb = new StringBuilder();

        sb.append("Id -");

        sb.append(" Decision -");

        sb.append(" Range | Text Snippet");
        sb.append(" | Identity -");
        sb.append(" Neuron Label -");
        sb.append(" Upper Bound -");
        sb.append(" Value | Net | Weight -");
        sb.append(" Input Value |");
        sb.append(" Target Value");
        sb.append("\n");
        sb.append("\n");

        for(Activation act: acts) {
            if(act.getUpperBound() <= 0.0 && (act.getTargetValue() == null || act.getTargetValue() <= 0.0)) {
                continue;
            }

            sb.append(act.toStringDetailed());
            sb.append("\n");
        }

        if(selectedSearchNode != null) {
            sb.append("\n Final SearchNode:" + selectedSearchNode.getId() + "  WeightSum:" + selectedSearchNode.getAccumulatedWeight() + "\n");
        }
        return sb.toString();
    }


    public static class CyclicDependencyException extends RuntimeException {

        public CyclicDependencyException() {
            super("Cycle detected in the activations that is not marked recurrent.");
        }
    }



    public void train(Config c) {
        createV = getNewVisitedId();

        Function<Activation, ExcitatoryNeuron> callback = act -> new ExcitatoryNeuron(getModel(), act.getLabel());

        for(Activation act: new ArrayList<>(getActivations(false))) {
            if(act.getUpperBound() > 0.0) {
                TNeuron n = act.getINeuron();

                for (Option o : act.getOptions()) {
                    n.prepareTrainingStep(c, o, callback);
                }
            }
        }

        for(Activation act: new ArrayList<>(getActivations(false))) {
            if(act.getUpperBound() > 0.0) {
                TNeuron n = act.getINeuron();

                n.updateFrequencies(act);
                n.initCountValues();

                for (Option o : act.getOptions()) {
                    n.train(c, o);

                    o.targetNeuron.commit(o.targetNeuron.getInputSynapses());
                }
            }
        }

        propagate();

        getModifiedWeights().forEach((n, inputSyns) -> {
            TNeuron tn = (TNeuron) n;
            tn.computeOutputRelations();
        });

        commit();

        getModel().charCounter += length();
    }
}
