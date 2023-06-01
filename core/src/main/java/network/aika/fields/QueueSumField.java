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
package network.aika.fields;


import network.aika.callbacks.FieldObserver;
import network.aika.elements.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import java.util.ArrayList;
import java.util.List;



/**
 * @author Lukas Molzberger
 */
public class QueueSumField extends SumField implements IQueueField {

    private Phase phase;

    protected FieldStep currentStep;

    protected FieldStep nextStep;

    protected List<FieldObserver> observers = new ArrayList<>();

    public QueueSumField(FieldObject e, Phase p, String label, Double tolerance) {
        super(e, label, tolerance);
        phase = p;
    }

    public QueueSumField(FieldObject e, Phase p, String label, Double tolerance, boolean weakRefs) {
        super(e, label, tolerance, weakRefs);
        phase = p;
    }

    @Override
    public void addObserver(FieldObserver observer) {
        if(observers.contains(observer))
            return;

        observers.add(observer);
    }

    @Override
    public void removeObserver(FieldObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void receiveUpdate(double u, boolean isFeedback) {
        updateObservers();

        FieldStep s = getStep(isFeedback);
        if(s == null) {
            int r = currentStep != null ? currentStep.getRound() : 0;
            if(isFeedback)
                r++;
            s = new FieldStep<>((Element) getReference(), phase, r, this);
            setStep(isFeedback, s);
        }
        s.updateDelta(u);

        if(!s.isQueued()) {
            if(!Step.add(s)) {
                process(s);
            }
        }
    }

    private FieldStep getStep(boolean isFeedback) {
        return isFeedback ? nextStep : currentStep;
    }

    private void setStep(boolean isFeedback, FieldStep s) {
        if(isFeedback)
            nextStep = s;
        else
            currentStep = s;
    }

    public void process(FieldStep s) {
        if(nextStep == s) {
            currentStep = s;
            nextStep = null;
        }

        triggerUpdate(s.getDelta());

        currentStep.reset();
        updateObservers();
    }

    private void updateObservers() {
        observers.forEach(o ->
                o.receiveUpdate(value)
        );
    }
}
