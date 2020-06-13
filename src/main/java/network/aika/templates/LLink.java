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
package network.aika.templates;

import network.aika.Phase;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;

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

    protected Class<S> synapseClass;

    public LLink() {
    }

    public LLink setLabel(String label) {
        this.label = label;
        return this;
    }

    public LLink setInput(LNode input) {
        this.input = input;
        input.addLink(this);
        return this;
    }

    public LLink setOutput(LNode output) {
        this.output = output;
        output.addLink(this);
        return this;
    }

    public LLink setSynapseClass(Class<S> synapseClass) {
        this.synapseClass = synapseClass;
        return this;
    }

    public LLink setDirection(boolean dir) {
        return this;
    }

    public abstract void followBackwards(Link l);

    protected abstract void follow(Activation act, LNode from, Activation startAct);

    public void follow(Activation act, Direction dir) {
        if(act.getThought().getPhase() == Phase.INDUCTION &&
                !act.getThought().getTrainingConfig().getMaturityCheck().test(act)) {
            return;
        }

        LNode n = getNode(dir.getInverted());
        n.follow(act.getNeuron(), act, n.isOpenEnd() ? null : this, act);
    }

    protected LNode getNode(Direction dir) {
        return dir == INPUT ? input : output;
    }

    protected boolean checkSynapse(Synapse s) {
        if(synapseClass != null && !synapseClass.equals(s.getClass())) {
            return false;
        }

        return true;
    }

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

    public abstract String getTypeStr();

    public String toString() {
        return getTypeStr() + " " + label + " " + input.label  + " -> " + output.label;
    }
}
