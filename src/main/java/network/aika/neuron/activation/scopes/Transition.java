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
    private Synapse templateSynapse;
    private boolean isTargetAllowed = false;
    private Scope input;
    private Scope output;


    private Transition(Synapse templateSynapse) {
        this.templateSynapse = templateSynapse;
    }

    private Transition(Synapse templateSynapse, Scope input, Scope output) {
        this(templateSynapse);
        this.input = input;
        this.output = output;

        input.getOutputs().add(this);
        output.getInputs().add(this);
    }

    private Transition(Synapse templateSynapse, boolean isTargetAllowed) {
        this(templateSynapse);
        this.isTargetAllowed = isTargetAllowed;
    }

    private Transition(Synapse templateSynapse, boolean isTargetAllowed, Scope input, Scope output) {
        this(templateSynapse, input, output);
        this.isTargetAllowed = isTargetAllowed;
    }

    public static void add(Scope input, Scope output, Synapse... templateSynapse) {
        add(false, input, output, templateSynapse);
    }

    public static void add(boolean isTarget, Scope input, Scope output, Synapse... templateSynapse) {
        for(Synapse ts: templateSynapse) {
            new Transition(ts, isTarget, input, output);
        }
    }

    public Transition getTemplate() {
        return template;
    }

    public Transition getInstance(Direction dir, Scope from) {
        Transition t = new Transition(templateSynapse, isTargetAllowed);
        t.template = this;
        dir.setFromScope(from, t);
        return t;
    }

    public boolean check(Synapse s, boolean isTargetLink) {
        return s.getTemplate() == templateSynapse && (this.isTargetAllowed || !isTargetLink);
    }

    public Synapse getSynapseTemplate() {
        return templateSynapse;
    }

    public boolean isTargetAllowed() {
        return isTargetAllowed;
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
                templateSynapse.getTemplateInfo().getLabel() +
                (isTargetAllowed ? ":T" : "") +
                ":" + output +
                ">";
    }
}
