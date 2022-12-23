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
import network.aika.fields.ConstantField;
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.neuron.PreActivation;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Range;
import network.aika.neuron.activation.*;
import network.aika.steps.Phase;
import network.aika.steps.QueueKey;
import network.aika.steps.Step;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static network.aika.ExternalPhase.*;
import static network.aika.callbacks.EventType.*;
import static network.aika.fields.Fields.invert;
import static network.aika.steps.Phase.*;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Thought extends FieldObject {

    private Field annealing;
    private FieldOutput annealingInverted;

    private ExternalPhase externalPhase = NEUTRAL;

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

        annealing = new ConstantField(this, "anneal", 1.0);
        annealingInverted = invert(this, "!anneal", annealing);

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

    public Field getAnnealing() {
        return annealing;
    }

    public FieldOutput getAnnealingInverted() {
        return annealingInverted;
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

    public ExternalPhase getExternalPhase() {
        return externalPhase;
    }

    public void setExternalPhase(ExternalPhase externalPhase) {
        this.externalPhase = externalPhase;
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

    /**
     * The postprocessing steps such as counting, cleanup or save are executed.
     */
    public void postProcessing() {
        externalPhase = POST;
        process(null);
        externalPhase = NEUTRAL;
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
        externalPhase = DISCONNECT;
        if(model.getCurrentThought() == this)
            model.setCurrentThought(null);

        getActivations().forEach(act ->
                act.disconnect()
        );
        externalPhase = NEUTRAL;
    }

    public void anneal(double stepSize) {
        externalPhase = ANNEAL;
        anneal(stepSize, (act, x) ->
                annealing.setValue(x)
        );
        externalPhase = NEUTRAL;
    }

    private void anneal(double stepSize, BiConsumer<BindingActivation, Double> c) {
        for(double x = 1.0; x > -0.001; x -= stepSize) {
            final double xFinal = x;
            getActivations().stream()
                    .filter(act -> act instanceof BindingActivation)
                    .map(act -> (BindingActivation)act)
                    .forEach(act ->
                            c.accept(act, xFinal)
                    );

            process(PROCESSING);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("External Phase:" + externalPhase + "\n");

        for(Activation act: activationsById.values()) {
            sb.append(act.toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}
