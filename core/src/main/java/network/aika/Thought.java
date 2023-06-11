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


import network.aika.callbacks.ActivationCheckCallback;
import network.aika.callbacks.EventListener;
import network.aika.callbacks.EventType;
import network.aika.callbacks.InstantiationCallback;
import network.aika.elements.activations.Activation;
import network.aika.elements.Element;
import network.aika.elements.activations.Timestamp;
import network.aika.fields.*;
import network.aika.elements.neurons.PreActivation;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.neurons.Range;
import network.aika.steps.Phase;
import network.aika.steps.keys.QueueKey;
import network.aika.steps.Step;
import network.aika.steps.activation.InactiveLinks;
import network.aika.steps.activation.Instantiation;
import network.aika.steps.thought.AnnealStep;

import java.util.*;
import java.util.function.Consumer;

import static network.aika.callbacks.EventType.*;
import static network.aika.steps.Phase.*;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Thought implements Element {

    private Field annealing;

    protected final Model model;

    private Long id;
    private long absoluteBegin;

    private Timestamp timestampOnProcess = new Timestamp(0);
    private long timestampCounter = 0;
    private int activationIdCounter = 0;

    private long visitorCounter = 0;

    private Step currentStep;

    int round = 0;

    private final NavigableMap<QueueKey, Step> queue = new TreeMap<>(QueueKey.COMPARATOR);

    private final TreeMap<Integer, Activation> activationsById = new TreeMap<>();
    private final Map<NeuronProvider, PreActivation<? extends Activation>> actsPerNeuron = new HashMap<>();
    private final List<network.aika.callbacks.EventListener> eventListeners = new ArrayList<>();

    private Config config;

    private ActivationCheckCallback activationCheckCallback;
    private InstantiationCallback instantiationCallback;


    public Thought(Model m) {
        model = m;
        id = model.createThoughtId();
        absoluteBegin = m.getN();

        annealing = new ConstantField(this, "anneal", 0.0);

        assert m.getCurrentThought() == null;
        m.setCurrentThought(this);
    }

    public int getRound() {
        return round;
    }

    public void updateRound(int r) {
        round = Math.max(round, r);
    }

    public void incrementRound() {
        round++;
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

    public Step getCurrentStep() {
        return currentStep;
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

    public ActivationCheckCallback getActivationCheckCallBack() {
        return this.activationCheckCallback;
    }

    public void setActivationCheckCallback(ActivationCheckCallback activationCheckCallback) {
        this.activationCheckCallback = activationCheckCallback;
    }

    public InstantiationCallback getInstantiationCallback() {
        return instantiationCallback;
    }

    public void setInstantiationCallback(InstantiationCallback instantiationCallback) {
        this.instantiationCallback = instantiationCallback;
    }

    private void callEventListener(Consumer<network.aika.callbacks.EventListener> el) {
        getEventListeners().forEach(el);
    }

    public synchronized Collection<network.aika.callbacks.EventListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }

    public synchronized void addEventListener(network.aika.callbacks.EventListener l) {
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

            currentStep = queue.pollFirstEntry().getValue();
            currentStep.removeQueueKey();

            timestampOnProcess = getCurrentTimestamp();

            queueEvent(BEFORE, currentStep);
            currentStep.process();
            queueEvent(AFTER, currentStep);
            currentStep = null;
        }
    }

    private boolean checkMaxPhaseReached(Phase maxPhase) {
        return maxPhase == null ?
                false :
                maxPhase.compareTo(queue.firstEntry().getValue().getPhase()) < 0;
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
                .toList();
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

        getActivations()
                .forEach(act ->
                        act.disconnect()
                );
    }

    public void anneal() {
        AnnealStep.add(this);
        process(ANNEAL); // Anneal needs to be finished before instantiation can start.
    }

    public void train() {
        activationsById.values()
                .forEach(InactiveLinks::add);

        process(TRAINING);
    }

    public void instantiateTemplates() {
        if (!getConfig().isMetaInstantiationEnabled())
            return;
        
        activationsById.values().stream()
                .filter(act -> act.getNeuron().isAbstract())
                .filter(act -> act.isFired())
                .forEach(Instantiation::add);
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
