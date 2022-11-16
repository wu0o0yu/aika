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

import network.aika.Model;
import network.aika.neuron.activation.Element;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Lukas Molzberger
 */
public class Field<R extends Element> implements FieldInput, FieldOutput, Writable {

    private String label;
    private R reference;

    protected double currentValue;
    protected double newValue;

    protected boolean withinUpdate;

    private Collection<FieldLink> inputs;

    private Collection<FieldLink> receivers;


    public Field(R reference, String label) {
        this(reference, label, false);
    }

    public Field(R reference, String label, boolean weakRefs) {
        this.reference = reference;
        this.label = label;

        initIO(weakRefs);
    }

    public Field(R reference, String label, boolean weakRefs, double initialValue) {
        this(reference, label, weakRefs);

        currentValue = initialValue;
    }

    public Field(R reference, String label, double initialValue) {
        this(reference, label, false);

        currentValue = initialValue;
    }

    private void initIO(boolean weakRefs) {
        // TODO: implement weakRefs
        inputs = new ArrayList<>();
        receivers = new ArrayList<>();
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public void setNewValue(double newValue) {
        this.newValue = newValue;
    }

    public void setValue(double v) {
        newValue = v;

        triggerUpdate();
    }

    @Override
    public R getReference() {
        return reference;
    }

    public void setReference(R reference) {
        this.reference = reference;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public double getNewValue() {
        return newValue;
    }

    public Collection<FieldLink> getReceivers() {
        return receivers;
    }

    @Override
    public void addOutput(FieldLink fl) {
        this.receivers.add(fl);
    }

    @Override
    public void removeOutput(FieldLink fl) {
        this.receivers.remove(fl);
    }

    public void receiveUpdate(FieldLink fl, double u) {
        receiveUpdate(u);
    }

    public void receiveUpdate(double u) {
        assert !withinUpdate;

        newValue += u;
        triggerUpdate();
    }

    public void triggerUpdate() {
        if(Utils.belowTolerance(newValue - currentValue))
            return;

        triggerInternal();
    }

    protected void triggerInternal() {
        withinUpdate = true;

        propagateUpdate(newValue - currentValue);
        currentValue = newValue;

        withinUpdate = false;
    }

    protected void propagateUpdate(double update) {
        FieldLink[] recs = receivers.toArray(new FieldLink[0]);

        for(int i = 0; i < recs.length; i++) {
            recs[i].receiveUpdate(update);
        }
    }

    public int getNextArg() {
        return inputs.size();
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

    public FieldLink getInputLinkByArg(int arg) {
        FieldLink[] in = inputs.toArray(new FieldLink[0]);
        return in[arg];
    }

    public double getInputValueByArg(int arg) {
        return getInputLinkByArg(arg).getCurrentInputValue();
    }

    @Override
    public Collection<FieldLink> getInputs() {
        return inputs;
    }

    @Override
    public void disconnect() {
        receivers.forEach(lf ->
                lf.disconnectAndUnlink()
        );
        receivers.clear();

        inputs.stream()
                .forEach(l ->
                        l.getInput().removeOutput(l)
                );
        inputs.clear();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(currentValue);
        out.writeDouble(newValue);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        currentValue = in.readDouble();
        newValue = in.readDouble();
    }

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        return "[ov:" + Utils.round(getCurrentValue()) + " nv:" + Utils.round(getNewValue()) + "]";
    }
}
