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
import network.aika.callbacks.EventType;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.ConjunctiveActivation;
import network.aika.elements.Element;
import network.aika.elements.activations.Timestamp;
import network.aika.fields.*;
import network.aika.elements.links.NegativeFeedbackLink;
import network.aika.elements.neurons.PreActivation;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.neurons.Range;
import network.aika.steps.Phase;
import network.aika.steps.QueueKey;
import network.aika.steps.Step;
import network.aika.steps.activation.InstantiationNodes;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static network.aika.callbacks.EventType.*;
import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.invert;
import static network.aika.steps.Phase.*;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Thought extends FieldObject {

    private Field annealing;
    private Field isOpen;

    private Field isClosed;

    protected final Model model;

    private Long id;
    private long absoluteBegin;

    private Timestamp timestampOnProcess = new Timestamp(0);
    private long timestampCounter = 0;
    private int activationIdCounter = 0;

    private long visitorCounter = 0;

    private final NavigableMap<QueueKey, Step> queue = new TreeMap<>(QueueKey.COMPARATOR);

    private final TreeMap<Integer, Activation> activationsById = new TreeMap<>();
    private final Map<NeuronProvider, PreActivation<? extends Activation>> actsPerNeuron = new HashMap<>();
    private final List<EventListener> eventListeners = new ArrayList<>();

    private Config config;

    public Thought(Model m) {
        model = m;
        id = model.createThoughtId();
        absoluteBegin = m.getN();

        isOpen = new ConstantField(this, "isOpen", 1.0);
        isClosed = invert(this, "isClosed", isOpen);

        annealing = new ConstantField(this, "anneal", 0.0);

        connect(INPUT, true, false);

        assert m.getCurrentThought() == null;
        m.setCurrentThought(this);
    }

    public long getNewVisitorId() {
        return visitorCounter++;
    }

    public Long getId() {
        return id;
    }

    public void updateModel() {
        model.addToN(length());
    }

    public Model getModel() {
        return model;
    }

    public Field getIsOpen() {
        return isOpen;
    }

    public Field getIsClosed() {
        return isClosed;
    }

    public void setIsOpen(double isOpen) {
        this.isOpen.setValue(isOpen);
    }

    public Field getAnnealing() {
        return annealing;
    }

    public abstract int length();

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void queueEvent(EventType et, Step s) {
        callEventListener(el ->
                el.onQueueEvent(et, s)
        );
    }

    public void onElementEvent(EventType et, Element e) {
        callEventListener(el ->
                el.onElementEvent(et, e)
        );
    }

    private void callEventListener(Consumer<EventListener> el) {
        getEventListeners().forEach(el);
    }

    public synchronized Collection<EventListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }

    public synchronized void addEventListener(EventListener l) {
        eventListeners.add(l);
    }

    public synchronized void removeEventListener(EventListener l) {
        eventListeners.remove(l);
    }

    public void register(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public void register(NeuronProvider np, PreActivation<? extends Activation> acts) {
        actsPerNeuron.put(np, acts);
    }

    public void addStep(Step s) {
        s.createQueueKey(getNextTimestamp());
        queue.put(s.getQueueKey(), s);
        queueEvent(ADDED, s);
    }

    public void removeStep(Step s) {
        Step removedStep = queue.remove(s.getQueueKey());
        assert removedStep != null;
        s.removeQueueKey();
    }

    public Collection<Step> getQueue() {
        return queue.values();
    }

    public Range getRange() {
        return new Range(absoluteBegin, absoluteBegin + length());
    }

    public void process(Phase maxPhase) {
        while (!queue.isEmpty()) {
            if(checkMaxPhaseReached(maxPhase))
                break;

            Step s = queue.pollFirstEntry().getValue();
            s.removeQueueKey();

            timestampOnProcess = getCurrentTimestamp();

            queueEvent(BEFORE, s);
            s.process();
            queueEvent(AFTER, s);
        }
    }

    private boolean checkMaxPhaseReached(Phase maxPhase) {
        return maxPhase == null ?
                false :
                maxPhase.compareTo(queue.firstEntry().getValue().getPhase()) < 0;
    }

    public void train() {
        process(TRAINING);
    }

    /**
     * The postprocessing steps such as counting, cleanup or save are executed.
     */
    public void postProcessing() {
        process(null);
    }

    public Timestamp getTimestampOnProcess() {
        return timestampOnProcess;
    }

    public Timestamp getCurrentTimestamp() {
        return new Timestamp(timestampCounter);
    }

    public Timestamp getNextTimestamp() {
        return new Timestamp(timestampCounter++);
    }

    public <E extends Element> List<Step> getStepsByElement(E element) {
        return queue
                .values()
                .stream()
                .filter(s -> s.getElement() == element)
                .collect(Collectors.toList());
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

    public void disconnect() {
        if(model.getCurrentThought() == this)
            model.setCurrentThought(null);

        getActivations().forEach(act ->
                act.disconnect()
        );
    }

    public void anneal() {
        double maxAnnealStep;

        while(true) {
            maxAnnealStep = Double.MAX_VALUE;
            for (AbstractFieldLink fl : annealing.getReceivers()) {
                if (!(fl.getOutput() instanceof Field))
                    continue;

                Field f = (Field) fl.getOutput();
                NegativeFeedbackLink negFeedbackLink = (NegativeFeedbackLink) f.getReference();
                double x = negFeedbackLink.getMaxInput().getCurrentValue();
                double w = negFeedbackLink.getSynapse().getWeight().getCurrentValue();
                double wi = x * w;
                if(wi >= 0.0)
                    continue;

                for (AbstractFieldLink flNet : f.getReceivers()) {
                    if (!(flNet.getOutput() instanceof Field))
                        continue;

                    Field fNet = (Field) flNet.getOutput();
                    if (fNet.getCurrentValue() > 0.0) {
                        System.out.println(negFeedbackLink + " " + fNet.getReference() + " " + fNet.getLabel() + " " + fNet.getCurrentValue());

                        maxAnnealStep = Math.min(maxAnnealStep, fNet.getCurrentValue() / -wi);
                    }
                }
            }

            if(maxAnnealStep <= 0.0 || maxAnnealStep == Double.MAX_VALUE)
                break;

            annealing.setValue(maxAnnealStep);
            process(INFERENCE);
        }
    }

    public void instantiateTemplates() {
        if (!getConfig().isMetaInstantiationEnabled())
            return;

        activationsById.values().stream()
                .filter(act -> act.getNeuron().isAbstract())
                .filter(act -> act.isFired())
                .filter(act -> act instanceof ConjunctiveActivation<?>)
                .forEach(act ->
                        InstantiationNodes.add((ConjunctiveActivation) act)
                );
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(Activation act: activationsById.values()) {
            sb.append(act.toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}
