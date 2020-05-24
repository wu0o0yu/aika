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

    private Phase phase = PRELIMINARY_LINKING;

    public Thought() {
    }

    public abstract int length();

    public void process() {
        phase = FINAL_LINKING;
        activationsById
                .values()
                .stream()
                .filter(act -> act.assumePosRecLinks())
                .forEach(act ->
                        act.getNeuron().link(act)
                );

        processLinks();
        processActivations();

        activationsById
                .values()
                .stream()
                .filter(act -> !act.hasBranches())
                .forEach(act -> act.computeP());

        processActivations();
        phase = null;
    }

    public void add(Activation act) {
        activationsQueue.add(act);
    }

    public void add(Link l) {
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
                    .process();
        }
    }

    public void processGradients() {
        while (!gradientQueue.isEmpty()) {
            gradientQueue
                    .pollFirst()
                    .processGradient();
        }
    }

    public int createActivationId() {
        return activationIdCounter++;
    }

    public Phase getPhase() {
        return phase;
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

    public Map<Neuron, SortedSet<Activation>> getActivationsPerNeuron() {
        Map<Neuron, SortedSet<Activation>> results = new TreeMap<>();

        activationsById.values().stream()
                .filter(act -> act.isActive())
                .forEach(act -> {
                    Set<Activation> acts = results.computeIfAbsent(
                            act.getNeuron(),
                            n -> new TreeSet<>()
                    );
                    acts.add(act);
                });

        return results;
    }

    public void train(Model m) {
        phase = INDUCTION;

        if(m.getTrainingConfig().getAlpha() != null) {
            Set<Neuron> activatedNeurons = new TreeSet<>();
            getActivations()
                .stream()
                .filter(act -> act.isActive())
                .forEach(act -> activatedNeurons.add(act.getNeuron()));

            m.applyMovingAverage();
            activatedNeurons.forEach(n ->
                n.applyMovingAverage()
            );
        }

        getActivations()
                .forEach(act ->
                        act.count()
                );

        new ArrayList<>(getActivations())
                .forEach(act ->
                        act.getNeuron().train(act)
                );

        processGradients();

//        process();

        m.addToN(length());

        phase = null;
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
