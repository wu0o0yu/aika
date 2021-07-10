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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private Synapse templateSynapse;
    private boolean isTarget;
    private Direction pathDir;
    private Scope input;
    private Scope output;


    private Transition(Synapse templateSynapse, boolean isTarget, Direction pathDir, Scope input, Scope output) {
        this.templateSynapse = templateSynapse;
        this.input = input;
        this.output = output;

        input.getOutputs().add(this);
        output.getInputs().add(this);
        this.isTarget = isTarget;
        this.pathDir = pathDir;
    }

    public static List<Transition> add(boolean isTarget, Direction pathDir, Scope input, Scope output, Synapse... templateSynapse) {
        ArrayList<Transition> transitions = new ArrayList<>();

        for(Synapse ts: templateSynapse)
            transitions.add(new Transition(ts, isTarget, pathDir, input, output));

        return transitions;
    }

    public boolean check(Synapse s, boolean isTargetLink) {
        return s.getTemplate() == templateSynapse && (this.isTarget || !isTargetLink);
    }

    public Synapse getSynapseTemplate() {
        return templateSynapse;
    }


    public Direction getPathDir() {
        return pathDir;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public Scope getInput() {
        return input;
    }

    public Scope getOutput() {
        return output;
    }

    public String toString() {
        return "<" +
                input +
                ":" +
                templateSynapse.getTemplateInfo().getLabel() +
                (isTarget ? ":T" : "") +
                ":" + output +
                ">";
    }
}
