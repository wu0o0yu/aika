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

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private Class<? extends Synapse> type;
    private Direction startDir;
    private boolean isTarget = false;
    private Scope input;
    private Scope output;


    private Transition(Class<? extends Synapse> type, Direction startDir, Scope input, Scope output) {
        this.type = type;
        this.startDir = startDir;
        this.input = input;
        this.output = output;
    }

    private Transition(Class<? extends Synapse> type, Direction startDir, boolean isTarget, Scope input, Scope output) {
        this(type, startDir, input, output);
        this.isTarget = isTarget;
    }

    public static void add(Class<? extends Synapse> type, Direction dir, Direction startDir, Scope input, Scope output) {
        add(type, dir, startDir, false, input, output);
    }

    public static void add(Class<? extends Synapse> type, Direction dir, Direction startDir, boolean isTarget, Scope input, Scope output) {
        Transition t = new Transition(type, startDir, isTarget, input, output);

        if(dir == null || dir == OUTPUT)
            t.link(OUTPUT);

        if(dir == null || dir == INPUT)
            t.invert().link(INPUT);
    }

    private void link(Direction dir) {
        dir.getTransitions(input).add(this);
    }

    private Transition invert() {
        return new Transition(type, startDir, output, input);
    }

    public boolean check(Synapse s, Direction startDir, boolean isTarget) {
        return s.getClass() == type && (this.startDir == null || this.startDir == startDir) && this.isTarget == isTarget;
    }

    public Class<? extends Synapse> getType() {
        return type;
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
        return input + " -- " + type.getSimpleName() + " -- " + (startDir != null ? startDir : "X") + " -- " + (isTarget ? "T" : "-")+ " --> " + output;
    }
}
