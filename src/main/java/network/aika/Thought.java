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
import network.aika.neuron.activation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static network.aika.neuron.Synapse.INPUT_COMPARATOR;
import static network.aika.neuron.activation.LinkingMode.FINAL;
import static network.aika.neuron.activation.LinkingMode.PRELIMINARY;


public abstract class Thought {
    private static final Logger log = LoggerFactory.getLogger(Thought.class);

    public static int MAX_ROUND = 20;

    private int activationIdCounter = 0;

    private final TreeSet<Activation> activationsQueue = new TreeSet<>(
            Comparator.<Activation, Fired>comparing(act -> act.getFired())
                    .thenComparing(Activation::getId)
    );

    private final Deque<Link> linkQueue = new ArrayDeque<>();

    private TreeMap<INeuron, Set<Synapse>> modifiedWeights = new TreeMap<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private LinkingMode linkingMode = PRELIMINARY;

    public Thought() {
    }

    public abstract int length();

    public void process() {
        linkingMode = FINAL;
        activationsById
                .values()
                .stream()
                .filter(act -> act.assumePosRecLinks)
                .forEach(act ->
                        act.getINeuron().linkPosRecSynapses(act)
                );

        processLinks();
        processActivations();

        activationsById
                .values()
                .stream()
                .filter(act -> !act.branches.isEmpty())
                .forEach(act -> act.computeP());

        processActivations();
    }

    public void add(Activation act) {
        activationsQueue.add(act);
    }

    public void add(Link l) {
        linkQueue.add(l);
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
                    .process();
        }
    }

    public int getNewActivationId() {
        return activationIdCounter++;
    }

    public LinkingMode getLinkingMode() {
        return linkingMode;
    }

    public void addActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public Collection<Activation> getActivations() {
        return activationsById.values();
    }

    public int getNumberOfActivations() {
        return activationsById.size();
    }

    public Map<INeuron, SortedSet<Activation>> getActivationsPerNeuron() {
        Map<INeuron, SortedSet<Activation>> results = new TreeMap<>();

        activationsById.values().stream()
                .filter(act -> act.isActive())
                .forEach(act -> {
                    Set<Activation> acts = results.computeIfAbsent(
                            act.getINeuron(),
                            n -> new TreeSet<>()
                    );
                    acts.add(act);
                });

        return results;
    }

    public void train(Model m) {
        if(m.getTrainingConfig().getAlpha() != null) {
            Set<INeuron> activatedNeurons = new TreeSet<>();
            getActivations()
                .stream()
                .filter(act -> act.isActive())
                .forEach(act -> activatedNeurons.add(act.getINeuron()));

            m.applyMovingAverage();
            activatedNeurons.forEach(n ->
                n.applyMovingAverage()
            );
        }

        getActivations()
                .forEach(act ->
                        act.count()
                );

        getActivations()
                .forEach(act ->
                        act.getINeuron().train(act)
                );

//        process();
        commit();

        m.addToN(length());
    }


    public void notifyWeightModified(Synapse synapse) {
        Set<Synapse> is = modifiedWeights.get(synapse.getOutput());
        if(is == null) {
            is = new TreeSet<>(INPUT_COMPARATOR);
            modifiedWeights.put(synapse.getOutput(), is);
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
        modifiedWeights.forEach((n, inputSyns) ->
            n.commit(inputSyns)
        );
        modifiedWeights.clear();
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
}
