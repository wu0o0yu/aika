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


import network.aika.FieldObject;
import network.aika.callbacks.FieldObserver;
import network.aika.elements.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.utils.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Lukas Molzberger
 */
public abstract class QueueField extends Field implements IQueueField {

    protected FieldStep step;

    protected List<FieldObserver> observers = new ArrayList<>();

    public QueueField(FieldObject e, Phase p, String label) {
        super(e, label);
        step = new FieldStep((Element) e, p, this);
    }

    public QueueField(FieldObject e, Phase p, String label, boolean weakRefs) {
        super(e, label, weakRefs);
        step = new FieldStep((Element) e, p, this);
    }


    public void setStep(FieldStep s) {
        this.step = s;
    }

    public FieldStep getStep() {
        return step;
    }

    public boolean isQueued() {
        return step.isQueued();
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
    public void triggerUpdate() {
        if(Utils.belowTolerance(newValue - currentValue))
            return;

        updateObservers();

        if(!isQueued()) {
            if(!Step.add(step)) {
                process();
            }
        }
    }

    public void process() {
        triggerInternal();
        updateObservers();
    }

    private void updateObservers() {
        observers.forEach(o ->
                o.receiveUpdate(currentValue, newValue)
        );
    }
}
