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

import network.aika.neuron.activation.Element;
import network.aika.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Molzberger
 */
public class Field<R extends Element> extends AbstractField<R> {

    private static final Logger log = LoggerFactory.getLogger(Field.class);

    private PropagatePreCondition propagatePreCondition;

    private List<FieldLink> inputs = new ArrayList<>();

    public Field(R reference, String label) {
        super(reference, label);

        this.propagatePreCondition = (cv, nv, u) -> !Utils.belowTolerance(u);
    }

    public Field(R reference, String label, double initialValue) {
        this(reference, label);

        currentValue = initialValue;
    }

    public Field(R reference, String label, FieldOnTrueEvent fieldListener) {
        this(reference, label);
        addEventListener(fieldListener);
    }

    protected boolean checkPreCondition(Double cv, double nv, double u) {
        return propagatePreCondition.check(cv, nv, u);
    }

    public PropagatePreCondition getPropagatePreCondition() {
        return propagatePreCondition;
    }

    public void setPropagatePreCondition(PropagatePreCondition propagatePreCondition) {
        this.propagatePreCondition = propagatePreCondition;
    }

    @Override
    public void addInput(FieldLink l) {
        inputs.add(l);
    }

    @Override
    public void removeInput(FieldLink l) {
        inputs.remove(l);
    }

    public FieldLink getInputLink(Field f) {
        return inputs.stream()
                .filter(l -> l.getInput() == f)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<FieldLink> getInputs() {
        return inputs;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        inputs.stream()
                .forEach(l -> l.getInput().removeOutput(l, false));
        inputs.clear();
    }
}
