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

import java.util.List;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractBiFunction extends FieldNode implements FieldInput, FieldOutput {

    protected FieldLink in1;
    protected FieldLink in2;


    public AbstractBiFunction(Element ref, String label) {
        super(ref, label);
    }

    public FieldLink getInput1() {
        return in1;
    }

    public FieldLink getInput2() {
        return in2;
    }

    @Override
    public int getNextArg() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FieldLink> getInputs() {
        return List.of(in1, in2);
    }

    public void addInput(FieldLink l) {
        if(l.getArgument() == 1)
            in1 = l;
        else
            in2 = l;
    }

    @Override
    public void removeInput(FieldLink l) {
        if(l.getArgument() == 1)
            in1 = null;
        else
            in2 = null;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if(in1 != null) {
            in1.getInput().removeOutput(in1, false);
            in1 = null;
        }
        if(in2 != null) {
            in2.getInput().removeOutput(in2, false);
            in2 = null;
        }
    }

    @Override
    public Element getReference() {
        return null;
    }

    private boolean isInitialized(FieldLink fl) {
        FieldLink in = fl == in1 ? in2 : in1;

        return in != null && in.getInput().isInitialized();
    }

    @Override
    public boolean isInitialized() {
        return in1 != null && in1.getInput().isInitialized() &&
                in2 != null && in2.getInput().isInitialized();
    }

    public void receiveUpdate(FieldLink fl, double inputCV, double u) {
        if(isInitialized(fl)) {
            double ownCV = getCurrentValue(fl, inputCV);
            propagateUpdate(
                    ownCV,
                    computeUpdate(fl, inputCV, ownCV, u)
            );
        }
    }

    protected abstract double getCurrentValue(FieldLink fl, double inputCV);

    protected abstract double computeUpdate(FieldLink fl, double inputCV, double ownCV, double u);

    @Override
    public String toString() {
        return getLabel() + ":" + getValueString();
    }

    public String getValueString() {
        if(!isInitialized())
            return "--";

        return "[v:" + Utils.round(getCurrentValue()) + "]";
    }
}
