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
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.ScopeEntry;

import java.util.Collections;
import java.util.Set;


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
        assert input.getTemplates().contains(getInput());

        InhibitorySynapse s = new InhibitorySynapse(input, output, this);
        initFromTemplate(s);
        return s;
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
    @Override
    public Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement) {
        if(checkFinalRequirement && s.getScope() != I_SAME) {
            return Collections.emptySet();
        }

        return Collections.singleton(s);
    }
     */

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }
}
