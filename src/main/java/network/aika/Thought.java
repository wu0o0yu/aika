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


import network.aika.callbacks.EventListener;
import network.aika.callbacks.VisitorEventListener;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Thought {
    private static final Logger log = LoggerFactory.getLogger(Thought.class);

    private int activationIdCounter = 0;

    protected final TreeSet<QueueEntry> queue = new TreeSet<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    private Map<NeuronProvider, SortedSet<Activation>> actsPerNeuron = null;

    private List<network.aika.callbacks.EventListener> eventListeners = new ArrayList<>();
    private List<VisitorEventListener> visitorEventListeners = new ArrayList<>();

    public Thought() {
    }

    public abstract int length();

    public abstract void linkInputRelations(Activation act);


    public void onActivationCreationEvent(Activation act, Activation originAct) {
        getEventListeners()
                .forEach(
                        el -> el.onActivationCreationEvent(act, originAct)
                );
    }

    public void onActivationProcessedEvent(Phase p, Activation act) {
        getEventListeners()
                .forEach(
                        el -> el.onActivationProcessedEvent(p, act)
                );
    }

    public void afterActivationProcessedEvent(Phase p, Activation act) {
        getEventListeners()
                .forEach(
                        el -> el.afterActivationProcessedEvent(p, act)
                );
    }

    public void onLinkProcessedEvent(Phase p, Link l) {
        getEventListeners()
                .forEach(
                        el -> el.onLinkProcessedEvent(p, l)
                );
    }

    public void afterLinkProcessedEvent(Phase p, Link l) {
        getEventListeners()
                .forEach(
                        el -> el.afterLinkProcessedEvent(p, l)
                );
    }

    public void onLinkCreationEvent(Link l) {
        getEventListeners()
                .forEach(
                        el -> el.onLinkCreationEvent(l)
                );
    }

    public void onVisitorEvent(Visitor v, boolean dir) {
        getVisitorEventListeners()
                .forEach(
                        el -> el.onVisitorEvent(v, dir)
                );
    }

    public synchronized Collection<network.aika.callbacks.EventListener> getEventListeners() {
        return eventListeners
                .stream()
                .collect(Collectors.toList());
    }

    public synchronized void addEventListener(network.aika.callbacks.EventListener l) {
        eventListeners.add(l);
    }

    public synchronized void removeEventListener(EventListener l) {
        eventListeners.remove(l);
    }

    public synchronized void addVisitorEventListener(VisitorEventListener l) {
        visitorEventListeners.add(l);
    }

    public synchronized void removeVisitorEventListener(VisitorEventListener l) {
        visitorEventListeners.remove(l);
    }

    public synchronized Collection<VisitorEventListener> getVisitorEventListeners() {
        return visitorEventListeners
                .stream()
                .collect(Collectors.toList());
    }

    public void registerActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void addToQueue(Activation act, ActivationPhase... phases) {
        addToQueueIntern(act, phases);
    }

    public void addToQueue(Link l, LinkPhase... phases) {
        addToQueueIntern(l, phases);
    }

    private <P extends Phase, E extends Element> void addToQueueIntern(E e, P... phases) {
        for(P p: phases) {
            if(p == null)
                continue;

            QueueEntry qe = new QueueEntry(0, p, e);
            e.addQueuedPhase(qe);
            addQueueEntry(qe);
        }
    }

    public void addQueueEntry(QueueEntry qe) {
        queue.add(qe);
    }

    public void removeQueueEntry(QueueEntry qe) {
        boolean isRemoved = queue.remove(qe);
        assert isRemoved;
    }

    public void removeQueueEntries(Collection<QueueEntry> qe) {
        queue.removeAll(qe);
    }

    public SortedSet<QueueEntry> getQueue() {
        return queue;
    }

    public void process(Model m) {
        while (!queue.isEmpty()) {
            QueueEntry qe = queue.pollFirst();
            qe.getElement().removeQueuedPhase(qe);
            qe.process();
        }
        m.addToN(length());
    }

    public <E extends Element> List<Phase> getPhasesForElement(E element) {
        return queue
                .stream()
                .filter(qe -> qe.getElement() == element)
                .map(qe -> qe.getPhase())
                .collect(Collectors.toList());
    }

    public int createActivationId() {
        return activationIdCounter++;
    }

    public Activation createActivation(Neuron n, Activation fromAct) {
        Activation toAct = new Activation(
                createActivationId(),
                this,
                n
        );

        onActivationCreationEvent(toAct, fromAct);

        return toAct;
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
                .filter(act -> act.isActive(false))
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
