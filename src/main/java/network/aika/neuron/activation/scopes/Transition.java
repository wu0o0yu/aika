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
package network.aika.neuron.activation.scopes;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private Transition template;
    private Class<? extends Synapse> type;
    private boolean isTarget = false;
    private Scope input;
    private Scope output;


    private Transition(Class<? extends Synapse> type) {
        this.type = type;
    }

    private Transition(Class<? extends Synapse> type, Scope input, Scope output) {
        this(type);
        this.input = input;
        this.output = output;

        input.getOutputs().add(this);
        output.getInputs().add(this);
    }

    private Transition(Class<? extends Synapse> type, boolean isTarget) {
        this(type);
        this.isTarget = isTarget;
    }

    private Transition(Class<? extends Synapse> type, boolean isTarget, Scope input, Scope output) {
        this(type, input, output);
        this.isTarget = isTarget;
    }

    public static void add(Class<? extends Synapse> type, Scope input, Scope output) {
        add(type, false, input, output);
    }

    public static void add(Class<? extends Synapse> type, boolean isTarget, Scope input, Scope output) {
        new Transition(type, isTarget, input, output);
    }

    public Transition getTemplate() {
        return template;
    }

    public Transition getInstance(Direction dir, Scope from) {
        Transition t = new Transition(type, isTarget);
        t.template = this;
        dir.setFromScope(from, t);
        return t;
    }

    public boolean check(Synapse s, boolean isTarget) {
        return s.getClass() == type && (this.isTarget || !isTarget);
    }

    public Class<? extends Synapse> getType() {
        return type;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setInput(Scope input) {
        this.input = input;
    }

    public Scope getInput() {
        return input;
    }

    public void setOutput(Scope s) {
        output = s;
    }

    public Scope getOutput() {
        return output;
    }

    public String toString() {
        return "<" +
                input +
                ":" +
                type.getSimpleName() +
                (isTarget ? ":T" : "") +
                ":" + output +
                ">";
    }


}
