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

import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryNeuron extends Neuron<InhibitorySynapse> {

    public static byte type;

    protected InhibitoryNeuron() {
        super();
    }

    public InhibitoryNeuron(NeuronProvider p) {
        super(p);
    }

    public InhibitoryNeuron(Model model, String descriptionLabel, Boolean isInputNeuron) {
        super(model, descriptionLabel, isInputNeuron);
    }

    @Override
    public byte getType() {
        return type;
    }

    public Visitor transition(Visitor v) {
        return v;
    }

    @Override
    public void updateReference(Link nl) {
        nl.getOutput().setReference(nl.getInput().getReference());
    }

    @Override
    public void induceNeuron(Activation activation) {

    }

    public Link induceSynapse(Activation iAct, Activation oAct, Visitor c) {
        InhibitorySynapse s = new InhibitorySynapse(iAct.getNeuron(), (InhibitoryNeuron) oAct.getNeuron());
        s.setWeight(1.0);

        Link l = new Link(s, iAct, oAct, false);

        l.linkOutput();

        l.computeInitialGradient();
        l.removeGradientDependencies();

        oAct.updateSelfGradient();

        s.linkInput();
        l.linkInput();

        return l;
    }

    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }

    /*
    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < -getBias();
    }
*/

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }


    @Override
    public Synapse getInputSynapse(NeuronProvider n) {
        return n.getNeuron().getOutputSynapse(getProvider());
    }

    @Override
    public boolean containsInputSynapse(Synapse s) {
        return false;
    }

    @Override
    public void addInputSynapse(InhibitorySynapse s) {
    }

    @Override
    public boolean containsOutputSynapse(Synapse synapse) {
        return false;
    }

    @Override
    public void addOutputSynapse(Synapse s) {
    }

    @Override
    public void removeInputSynapse(InhibitorySynapse s) {
    }

    @Override
    public void removeOutputSynapse(Synapse s) {
    }
}
