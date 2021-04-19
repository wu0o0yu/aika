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

/**
 * @author Lukas Molzberger
 */
public class Transition implements Comparable<Transition> {

    private Class<? extends Synapse> type;
    private Direction dir;
    private boolean isTarget = false;
    private Scope input;
    private Scope output;


    private Transition(Class<? extends Synapse> type, Direction dir, Scope input, Scope output) {
        this.type = type;
        this.dir = dir;
        this.input = input;
        this.output = output;
    }

    private Transition(Class<? extends Synapse> type, Direction dir, boolean isTarget, Scope input, Scope output) {
        this(type, dir, input, output);
        this.isTarget = isTarget;
    }

    public static void add(Class<? extends Synapse> type, Direction dir, Scope input, Scope output) {
        add(type, dir, false, input, output);
    }

    public static void add(Class<? extends Synapse> type, Direction dir, boolean isTarget, Scope input, Scope output) {
        Transition t = new Transition(type, dir, isTarget, input, output);

        input.outputs.add(t);
        output.inputs.add(t);
    }

    public boolean check(Direction dir, Direction startDir, boolean isTarget) {
        return (this.dir == null || this.dir == dir) && this.isTarget == isTarget;
    }

    public Class<? extends Synapse> getType() {
        return type;
    }

    public Scope getInput() {
        return input;
    }

    public Scope getOutput() {
        return output;
    }

    public Direction getDir() {
        return dir;
    }

    private Scope getNextScope() {
        return dir == INPUT ? input : output;
    }

    @Override
    public int compareTo(Transition t) {
        int r = type.getSimpleName().compareTo(t.type.getSimpleName());
        if(r != 0) return r;
        r = getNextScope().getLabel().compareTo(t.getNextScope().getLabel());
        if(r != 0) return r;
        r = Direction.compare(dir, t.dir);
        if(r != 0) return r;
        return Boolean.compare(isTarget, t.isTarget);
    }
}
