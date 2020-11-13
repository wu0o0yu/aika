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
import network.aika.neuron.activation.*;
import network.aika.neuron.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static network.aika.neuron.phase.Phase.*;


public abstract class Thought {
    private static final Logger log = LoggerFactory.getLogger(Thought.class);

    private int activationIdCounter = 0;

    private final TreeSet<Activation> activationsQueue = new TreeSet<>();

    private final Deque<Link> linkQueue = new ArrayDeque<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private Map<NeuronProvider, SortedSet<Activation>> actsPerNeuron = null;

    private Config trainingConfig;

    public Thought() {
    }

    public Thought(Config trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public abstract int length();

    public void registerActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void addActivationToQueue(Activation act) {
        activationsQueue.add(act);
    }

    public abstract void linkInputRelations(Activation act);

    public void addLinkToQueue(Link l) {
        linkQueue.add(l);
    }

    public void process(Model m) {
        while (!activationsQueue.isEmpty()) {
            activationsQueue
                    .pollFirst()
                    .process();
        }
        m.addToN(length());
    }

    public void processLinks() {
        while (!linkQueue.isEmpty()) {
            linkQueue
                    .pollFirst()
                    .propagate();
        }
    }

    public Config getConfig() {
        return trainingConfig;
    }

    public void setConfig(Config trainingConfig) {
        this.trainingConfig = trainingConfig;
    }

    public int createActivationId() {
        return activationIdCounter++;
    }

    public Collection<Activation> getActivations() {
        return activationsById.values();
    }

    public int getNumberOfActivations() {
        return activationsById.size();
    }

    public Set<Activation> getActivations(NeuronProvider n) {
        return getActivations(n.getNeuron());
    }

    public Set<Activation> getActivations(Neuron n) {
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

    public String activationsToString() {
        return activationsToString(false);
    }

    public String activationsToString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
/*
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
*/
        for(Activation act: activationsById.values()) {
/*            if(!act.isActive()) {
                continue;
            }
*/
            if(act.getNeuron().isInputNeuron()) {
                continue;
            }

            sb.append(act.toString(includeLink));
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
