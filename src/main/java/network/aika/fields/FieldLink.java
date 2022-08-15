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
public class FieldLink<O extends UpdateListener> {

    private FieldOutput input;
    private int arg;
    private O output;

    public static FieldLink createEventListener(FieldOnTrueEvent eventListener) {
        return createUpdateListener((arg, cv, u) -> {
            if (u > 0.0)
                eventListener.onTrue();
        });
    }

    public static <O extends UpdateListener> FieldLink<O> createUpdateListener(O updateListener) {
        return new FieldLink(null, 0, updateListener);
    }

    public FieldLink(FieldOutput input, int arg, O output) {
        this.input = input;
        this.arg = arg;
        this.output = output;
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
