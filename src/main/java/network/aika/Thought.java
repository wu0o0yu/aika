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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public abstract class Thought {
    private static final Logger log = LoggerFactory.getLogger(Thought.class);

    private int activationIdCounter = 0;

    protected final TreeSet<QueueEntry> queue = new TreeSet<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private Map<NeuronProvider, SortedSet<Activation>> actsPerNeuron = null;

    private List<EventListener> eventListeners = new ArrayList<>();
    private List<VisitorEventListener> visitorEventListeners = new ArrayList<>();

    public Thought() {
    }

    public void onActivationCreationEvent(Activation act, Activation originAct) {
        eventListeners.forEach(
                el -> el.onActivationCreationEvent(act, originAct)
        );
    }

    public void onActivationProcessedEvent(Activation act) {
        eventListeners.forEach(
                el -> el.onActivationProcessedEvent(act)
        );
    }

    public void onLinkProcessedEvent(Link l) {
        eventListeners.forEach(
                el -> el.onLinkProcessedEvent(l)
        );
    }

    public void onVisitorEvent(Visitor v) {
        visitorEventListeners.forEach(
                el -> el.onVisitorEvent(v)
        );
    }

    public abstract int length();

    public void addEventListener(EventListener l) {
        eventListeners.add(l);
    }

    public void removeEventListener(EventListener l) {
        eventListeners.remove(l);
    }

    public void addVisitorEventListener(VisitorEventListener l) {
        visitorEventListeners.add(l);
    }

    public void removeVisitorEventListener(VisitorEventListener l) {
        visitorEventListeners.remove(l);
    }

    public void registerActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void addToQueue(QueueEntry qe) {
        queue.add(qe);
    }

    public void removeActivationFromQueue(QueueEntry qe) {
        boolean isRemoved = queue.remove(qe);
        assert isRemoved;
    }

    public abstract void linkInputRelations(Activation act);


    public void process(Model m) {
        while (!queue.isEmpty()) {
            QueueEntry<?> qe = queue.pollFirst();
            qe.onProcessEvent();
            qe.process();
        }
        m.addToN(length());
    }

    public int createActivationId() {
        return activationIdCounter++;
    }


    public Activation getActivation(Integer id) {
        return activationsById.get(id);
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

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeLink) {
        StringBuilder sb = new StringBuilder();
        for(Activation act: activationsById.values()) {
/*            if(!act.isActive())
                continue;
*/
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
