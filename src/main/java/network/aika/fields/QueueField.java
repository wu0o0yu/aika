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
import network.aika.neuron.activation.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Step;

import static network.aika.fields.FieldLink.createEventListener;

/**
 * @author Lukas Molzberger
 */
public class QueueField extends Field {

    protected boolean isQueued;
    protected FieldStep step;

    protected FieldObserver observer;

    public QueueField(Element e, String label) {
        super(e, label);
        step = new FieldStep(e, this);
    }

    public QueueField(Element e, String label, double initialValue) {
        super(e, label, initialValue);
        step = new FieldStep(e, this);
    }

    public QueueField(Element e, String label, FieldOnTrueEvent fieldListener) {
        this(e, label);
        addOutput(createEventListener(fieldListener), true);
    }

    public void setStep(FieldStep s) {
        this.step = s;
    }

    public FieldStep getStep() {
        return step;
    }

    public boolean isQueued() {
        return isQueued;
    }

    public void setObserver(FieldObserver observer) {
        this.observer = observer;
    }

    public void triggerUpdate() {
        if(observer != null)
            observer.receiveUpdate(currentValue, update);

        if(!isQueued) {
            Step.add(step);
            isQueued = true;
        }
    }

    @Override
    public void set(double v) {
        super.set(v);
        process();
    }

    public void process() {
        isQueued = false;
        triggerInternal();

        if(observer != null)
            observer.receiveUpdate(currentValue, update);
    }
}
