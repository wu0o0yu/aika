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
package network.aika.neuron.activation.linker;

import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LLink<S extends Synapse> {
    protected  String label;

    protected  LNode input;
    protected  LNode output;

    protected PatternScope patternScope;
    protected Class<S> synapseClass;

    public LLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label) {
        this.input = input;
        this.output = output;
        this.patternScope = patternScope;
        this.synapseClass = synapseClass;
        this.label = label;

        input.addLink(this);
        output.addLink(this);
    }

    protected abstract void follow(Activation act, LNode from, Activation startAct);

    public void follow(Activation act, Direction dir, boolean closedCycle) {
        getNode(dir.getInverted()).follow(act.getNeuron(), act, closedCycle ? this : null, act);
    }

    protected LNode getNode(Direction dir) {
        return dir == INPUT ? input : output;
    }

    protected boolean checkSynapse(Synapse s) {
        if(synapseClass != null && !synapseClass.equals(s.getClass())) {
            return false;
        }

        if(patternScope != null && patternScope != s.getPatternScope()) {
            return false;
        }

        return true;
    }

    public abstract String getTypeStr();

    protected Direction getDirection(LNode from) {
        if(from == input) {
            return OUTPUT;
        }
        if(from == output) {
            return INPUT;
        }
        return null;
    }

    protected LNode getTo(LNode from) {
        if(from == input) {
            return output;
        }
        if(from == output) {
            return input;
        }
        return null;
    }

    public String toString() {
        return getTypeStr() + " " + label + " " + (patternScope != null ? patternScope : "X") + " " + input.label  + " -> " + output.label;
    }
}
