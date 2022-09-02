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
import network.aika.utils.Utils;

/**
 * @author Lukas Molzberger
 */
public class FieldLink {

    private FieldOutput input;
    private int arg;
    private UpdateListener output;

    private boolean isInitialized;

    public static FieldLink createEventListener(FieldOutput in, FieldOnTrueEvent eventListener) {
        return createUpdateListener(in, (arg, u) -> {
            if (u > 0.0)
                eventListener.onTrue();
        });
    }

    public static FieldLink createUpdateListener(FieldOutput in, UpdateListener updateListener) {
        return new FieldLink(in, 0, updateListener);
    }


    public static void reconnect(FieldLink fl, Field newInput) {
        fl.disconnect();
        fl.getInput().removeOutput(fl);
        fl.setInput(newInput);
        newInput.addOutput(fl);
        fl.connect();
    }

    public static FieldLink connect(FieldOutput in, FieldInput out) {
        return connect(in, out.getNextArg(), out);
    }

    public static FieldLink link(FieldOutput in, FieldInput out) {
        return link(in, out.getNextArg(), out);
    }

    public static FieldLink connect(FieldOutput in, int arg, FieldInput out) {
        FieldLink fl = link(in, arg, out);
        fl.connect();
        return fl;
    }

    public static FieldLink link(FieldOutput in, int arg, FieldInput out) {
        FieldLink fl = new FieldLink(in, arg, out);
        out.addInput(fl);
        in.addOutput(fl);
        return fl;
    }

    public static void connectAll(FieldOutput in, FieldInput... out) {
        assert in != null;

        for(FieldInput o : out) {
            if(o != null) {
                connect(in, 0, o);
            }
        }
    }

    public static void disconnect(FieldOutput in, FieldInput out) {
        disconnect(in, 0, out);
    }

    public static void disconnect(FieldOutput in, int arg, FieldInput out) {
        FieldLink fl = new FieldLink(in, arg, out);
        out.removeInput(fl);
        in.removeOutput(fl);
    }

    public FieldLink(FieldOutput input, int arg, UpdateListener output) {
        this.input = input;
        this.arg = arg;
        this.output = output;
    }

    public void setInput(FieldOutput input) {
        this.input = input;
    }

    public void receiveUpdate(double u) {
        output.receiveUpdate(this, u);
    }

    public void connect() {
        assert !isInitialized;

        double cv = input.getCurrentValue();
        if(!Utils.belowTolerance(cv))
            output.receiveUpdate(this, cv);

        isInitialized = true;
    }

    public void disconnect() {
        assert isInitialized;

        double cv = input.getCurrentValue();
        if(!Utils.belowTolerance(cv))
            output.receiveUpdate(this, -cv);

        isInitialized = false;
        if(output instanceof FieldInput) {
            FieldInput fo = (FieldInput) output;
            fo.removeInput(this);
        }
    }

    public double getCurrentInputValue() {
        return isInitialized ? input.getCurrentValue() : 0.0;
    }

    public int getArgument() {
        return arg;
    }

    public FieldOutput getInput() {
        return input;
    }

    public UpdateListener getOutput() {
        return output;
    }

    @Override
    public boolean equals(Object o) {
        FieldLink fLink = (FieldLink) o;
        if(arg != fLink.arg)
            return false;

        if(!output.equals(fLink.output))
            return false;

        if(input == fLink.input)
            return true;

        return input != null && input.equals(fLink.input);
    }

    public String toString() {
        return input + " --" + arg + "--> " + output;
    }
}
