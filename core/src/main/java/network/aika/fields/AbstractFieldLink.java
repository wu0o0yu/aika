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

import network.aika.callbacks.UpdateListener;

import static network.aika.fields.Field.FIRST_ROUND;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractFieldLink<O extends UpdateListener> {

    protected FieldOutput input;
    private int arg;
    protected O output;

    protected boolean connected;

    protected boolean propagateUpdates = true;

    private boolean incrementRound;

    public AbstractFieldLink(FieldOutput input, int arg, O output) {
        this.input = input;
        this.arg = arg;
        this.output = output;
    }

    public void setPropagateUpdates(boolean propagateUpdates) {
        this.propagateUpdates = propagateUpdates;
    }

    public boolean isPropagateUpdates() {
        return propagateUpdates;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setIncrementRound(boolean incrementRound) {
        this.incrementRound = incrementRound;
    }

    public boolean isIncrementRound() {
        return incrementRound;
    }

    public void setInput(FieldOutput input) {
        this.input = input;
    }

    public void relinkInput(FieldOutput inputValue) {
        input.removeOutput(this);
        input = inputValue;
        input.addOutput(this);
    }

    public void receiveUpdate(int r, double u) {
        if(connected && propagateUpdates)
            output.receiveUpdate(this, updateRound(r, 1), u);
    }

    public void connect(boolean initialize) {
        if(connected)
            return;

        if(initialize) {
            int r = updateRound(FIRST_ROUND, -1);
            double cv = input.getValue(r);
            output.receiveUpdate(this, r, cv);
        }

        connected = true;
    }

    public void disconnect(boolean deinitialize) {
        if(!connected)
            return;

        if(deinitialize) {
            int r = updateRound(FIRST_ROUND, -1);
            double cv = input.getValue(r);
            output.receiveUpdate(this, r, -cv);
        }

        connected = false;
    }

    public abstract void unlink();

    public double getInputValue(int r) {
        return connected ?
                input.getValue(updateRound(r, -1)) :
                0.0;
    }

    public double getUpdatedInputValue(int r) {
        return connected ?
                input.getUpdatedValue(updateRound(r, -1)) :
                0.0;
    }

    private int updateRound(int r, int dir) {
        return incrementRound ? r + dir : r;
    }

    public int getArgument() {
        return arg;
    }

    public FieldOutput getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    @Override
    public boolean equals(Object o) {
        AbstractFieldLink fLink = (AbstractFieldLink) o;
        if(arg != fLink.arg)
            return false;

        if(!output.equals(fLink.output))
            return false;

        if(input == fLink.input)
            return true;

        return input != null && input.equals(fLink.input);
    }

    @Override
    public String toString() {
        return input + " --" + arg + "--> " + output;
    }
}
