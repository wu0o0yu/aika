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


import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static network.aika.Phase.*;


public abstract class Thought {
    private static final Logger log = LoggerFactory.getLogger(Thought.class);

    private int activationIdCounter = 0;

    private final TreeSet<Activation> activationsQueue = new TreeSet<>(
            Comparator.<Activation, Fired>comparing(act -> act.getFired())
                    .thenComparing(Activation::getId)
    );

    private final Deque<Link> linkQueue = new ArrayDeque<>();

    private final TreeSet<Activation> gradientQueue = new TreeSet<>(
            Comparator.<Activation, Fired>comparing(act -> act.getFired())
                    .thenComparing(Activation::getId)
                    .reversed()
    );

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private Map<NeuronProvider, SortedSet<Activation>> actsPerNeuron = null;

    private Phase phase = INITIAL_LINKING;

    private Config trainingConfig;

    public Thought() {
    }

    public Thought(Config trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public abstract int length();

    public void process() {
        phase = FINAL_LINKING;
        activationsById
                .values()
                .stream()
                .filter(act -> !act.getNeuron().isInputNeuron())
                .forEach(act -> act.updateForFinalPhase());

        processActivations();

        activationsById
                .values()
                .stream()
                .filter(act -> !act.hasBranches())
                .forEach(act -> act.computeP());

        processActivations();
        phase = null;
    }

    public void registerActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void addActivationToQueue(Activation act) {
        if(!act.isFinal()) {
            activationsQueue.add(act);
        }
    }

    public void addLinkToQueue(Link l) {
        linkQueue.add(l);
    }

    public void addToGradientQueue(Activation act) {
        gradientQueue.add(act);
    }

    public void processActivations() {
        while (!activationsQueue.isEmpty()) {
            activationsQueue
                    .pollFirst()
                    .process();
        }
    }

    public void processLinks() {
        while (!linkQueue.isEmpty()) {
            linkQueue
                    .pollFirst()
                    .propagate();
        }
    }

    public void processGradients() {
        while (!gradientQueue.isEmpty()) {
            gradientQueue
                    .pollFirst()
                    .processGradient();
        }

        activationsById
                .values()
                .stream()
                .flatMap(act -> act.getInputLinks())
                .forEach(l -> l.updateSynapse());
    }

    public Config getTrainingConfig() {
        return trainingConfig;
    }

    public void setTrainingConfig(Config trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public int createActivationId() {
        return activationIdCounter++;
    }

    public Phase getPhase() {
        return phase;
    }

    public Collection<Activation> getActivations() {
        return activationsById.values();
    }

    public Activation getNextActivation(Activation act) {
        Map.Entry<Integer, Activation> me = act == null ?
                activationsById.firstEntry() :
                activationsById.higherEntry(act.getId());

        return me != null ? me.getValue() : null;
    }

    public int getNumberOfActivations() {
        return activationsById.size();
    }

    public Set<Activation> getActivations(NeuronProvider n) {
        return getActivations(n.getNeuron());
    }

    public Set<Activation> getActivations(Neuron n) {
        phase = RESULTS;
        if(actsPerNeuron == null) {
            actsPerNeuron = getActivationsPerNeuron();
        }

        Set<Activation> acts = actsPerNeuron.get(n.getProvider());
        return acts != null ? acts : Collections.emptySet();
    }

    private Map<NeuronProvider, SortedSet<Activation>> getActivationsPerNeuron() {
        Map<NeuronProvider, SortedSet<Activation>> results = new TreeMap<>();

        activationsById.values().stream()
                .filter(act -> act.isActive())
                .forEach(act -> {
                    Set<Activation> acts = results.computeIfAbsent(
                            act.getNeuronProvider(),
                            n -> new TreeSet<>()
                    );
                    acts.add(act);
                });

        return results;
    }

    public void train(Model m) {
        phase = INDUCTION;
/*
        long v = createVisitedId();
        if(trainingConfig.getAlpha() != null) {
            Set<Neuron> activatedNeurons = new TreeSet<>();
            getActivations()
                .stream()
                .filter(act -> act.isActive())
                .map(act -> act.getNeuron())
                .filter(n -> n.checkVisited(v))
                .forEach(n -> n.applyMovingAverage(trainingConfig));

            m.applyMovingAverage(trainingConfig);
        }
*/
        count();

        {
            Activation act = null;
            while ((act = getNextActivation(act)) != null) {
                if(act.isActive()) {
                    act.getNeuron().train(act);
                }
            }
        }

        processGradients();

//        process();

        m.addToN(length());

        getActivations()
                .forEach(act ->
                        act.getNeuronProvider().save()
                );

        phase = null;
    }

    public void count() {
        getActivations()
                .forEach(act ->
                        act.count()
                );
    }

    public String activationsToString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Id -");

        sb.append(" Decision -");

        sb.append(" Range | Text Snippet");
        sb.append(" | Identity -");
        sb.append(" Neuron Label -");
        sb.append(" Upper Bound -");
        sb.append(" Value | Net | Weight -");
        sb.append(" Input Value |");
        sb.append("\n");
        sb.append("\n");

        for(Activation act: activationsById.values()) {
/*            if(!act.isActive()) {
                continue;
            }
*/
            sb.append(act.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public String gradientsToString() {
        StringBuilder sb = new StringBuilder();

        activationsById.values()
                .forEach(act -> sb.append(act.gradientsToString()));

        return sb.toString();
    }
}
