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
package network.aika.neuron.inhibitory;

import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.Scope;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Visitor.Transition.ACT;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends Synapse<Neuron<?>, InhibitoryNeuron> {

    public static byte type;

    public InhibitorySynapse() {
        super();
    }

    public InhibitorySynapse(Neuron<?> input, InhibitoryNeuron output, Synapse template) {
        super(input, output, template);
    }

    @Override
    public void updateReference(Link l) {
        l.getOutput().propagateReference(
                l.getInput().getReference()
        );
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return false;
    }

    @Override
    public InhibitorySynapse instantiateTemplate(Neuron<?> input, InhibitoryNeuron output) {
        if(!input.getTemplates().contains(getInput()))
            return null;

        return new InhibitorySynapse(input, output, this);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        return oAct;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        input.getNeuron().setModified(true);
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        input.getNeuron().setModified(true);
    }

/*
    public void transition(Visitor v, Activation fromAct, Activation toAct, boolean create) {
        if(v.downUpDir == INPUT & v.startDir == INPUT && v.origin.getNeuron() == getOutput()) {
            return;
        }

        Visitor nv = v.prepareNextStep(toAct, ACT);
        nv.incrementPathLength();

        nv.scopes = v.scopes
                .stream()
                .map(s -> transition(s, v.downUpDir))
                .collect(Collectors.toList());

        follow(fromAct, toAct, nv, create);
    }
*/

    @Override
    public Collection<Scope> transition(Scope s, Direction dir) {
        return Collections.singleton(s);
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }
}
