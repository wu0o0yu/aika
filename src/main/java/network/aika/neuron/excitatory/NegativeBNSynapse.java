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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.steps.link.SumUpLink;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public class NegativeBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public NegativeBNSynapse() {
        this.isRecurrent = true;
    }

    public void transition(ActVisitor v, Synapse s, Link l) {
        //Todo
    }

    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {

    }

    @Override
    public void inhibitoryTransitionLoop(ActVisitor v, Link l) {

    }

    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true) && l.isSelfRef())
            addWeight(-delta);
    }

    public void updateReference(Link l) {
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !v.getSelfRef())
            return null;

        if (!v.getSelfRef()) {
            oAct = oAct.createBranch(this);
        }
        return oAct;
    }
}
