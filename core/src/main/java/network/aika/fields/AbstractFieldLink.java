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

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractFieldLink<O extends UpdateListener> {

    protected FieldOutput input;
    private int arg;
    protected O output;

    protected boolean connected;

    protected boolean propagateUpdates = true;


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

    public boolean crossesBorder() {
        if(output instanceof FieldOutput) {
            return input.getReference() != ((FieldOutput) output).getReference();
        } else
            return false;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setInput(FieldOutput input) {
        this.input = input;
    }

    public void receiveUpdate(double u) {
        if(connected && propagateUpdates)
            output.receiveUpdate(this, u);
    }

    public void connect(boolean initialize) {
        if(connected)
            return;

        if(initialize) {
            double cv = input.getCurrentValue();
            output.receiveUpdate(this, cv);
        }

        connected = true;
    }

    public void disconnect(boolean deinitialize) {
        if(!connected)
            return;

        if(deinitialize) {
            double cv = input.getCurrentValue();
            output.receiveUpdate(this, -cv);
        }

        connected = false;
    }

    public abstract void unlink();

    public double getCurrentInputValue() {
        return connected ?
                input.getCurrentValue() :
                0.0;
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
