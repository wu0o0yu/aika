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

import network.aika.neuron.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.direction.Direction;

import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.Scope.*;
import static network.aika.neuron.activation.Scope.P_SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryNeuron extends Neuron<InhibitorySynapse> {

    public static byte type;

    public InhibitoryNeuron() {
        super();
    }

    public InhibitoryNeuron(NeuronProvider p) {
        super(p);
    }

    private InhibitoryNeuron(Model model) {
        super(model);
    }

    @Override
    public boolean checkGradientThreshold(Activation act) {
/*        if(act.gradientSumIsZero())
            return false;*/
        return false;
    }

    @Override
    public Set<ScopeEntry> getInitialScopes(Direction dir) {
        Set<ScopeEntry> result = new TreeSet<>();
        result.add(new ScopeEntry(0, I_SAME));
        if(dir == Direction.OUTPUT) {
            result.add(new ScopeEntry(1, PP_INPUT));
        }
        return result;
    }

    @Override
    public InhibitoryNeuron instantiateTemplate() {
        InhibitoryNeuron n = new InhibitoryNeuron(getModel());
        n.getTemplates().add(this);
        n.getTemplates().addAll(getTemplates());
        return n;
    }

    @Override
    public void addDummyLinks(Activation act) {
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }
}