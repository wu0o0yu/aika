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

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractFunction extends Field implements FieldInput {

    private FieldLink[] inputs;

    public AbstractFunction(FieldObject ref, String label, Double tolerance) {
        super(ref, label, tolerance);
    }

    public AbstractFunction(FieldObject ref, String label) {
        this(ref, label, null);
    }

    @Override
    protected void initIO(boolean weakRefs) {
        super.initIO(weakRefs);

        // TODO: implement weakRefs
        inputs = new FieldLink[getNumberOfFunctionArguments()];
    }

    protected int getNumberOfFunctionArguments() {
        return 1;
    }

    @Override
    public int getNextArg() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInput(FieldLink l) {
        inputs[l.getArgument()] = l;
    }

    @Override
    public void removeInput(FieldLink l) {
        inputs[l.getArgument()] = null;
    }

    @Override
    public Collection<FieldLink> getInputs() {
        return Arrays.asList(inputs);
    }

    public double getInputValueByArg(int arg) {
        return getInputLinkByArg(arg).getCurrentInputValue();
    }

    public FieldLink getInputLinkByArg(int arg) {
        return inputs[arg];
    }

    protected abstract double computeUpdate(AbstractFieldLink fl, double u);

    @Override
    public void receiveUpdate(AbstractFieldLink fl, double u) {
        double update = computeUpdate(fl, u);
        if(update == 0.0)
            return;

        newValue += update;
        triggerUpdate(fl.isFeedback());
    }
}
