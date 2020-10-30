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
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;

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

    public InhibitoryNeuron(Model model, Boolean isInputNeuron) {
        super(model, isInputNeuron);
    }

    @Override
    public byte getType() {
        return type;
    }

    public Visitor transition(Visitor v) {
        return v;
    }

    public boolean isInitialized() {
        return false;
    }

    @Override
    public void updateReference(Link nl) {
        nl.getOutput().setReference(nl.getInput().getReference());
    }

    @Override
    public void induceNeuron(Activation activation) {

    }

    public static void induce(Activation iAct) {
        if(!iAct.getConfig().checkInhibitoryNeuronInduction(iAct.getNeuron())) {
//            System.out.println("N  " + "dbg:" + (Neuron.debugId++) + " " + act.getNeuron().getDescriptionLabel() + "  " + Utils.round(s) + " below threshold");
            return;
        }

        if (!iAct.checkIfOutputLinkExists(syn -> syn instanceof PrimaryInhibitorySynapse)) {
            Neuron n = new InhibitoryNeuron(iAct.getModel(), false);
            n.initInstance(iAct.getReference());
            n.initInducedNeuron(iAct);
        }
    }

    public Link induceSynapse(Activation iAct, Activation oAct, Visitor v) {
        InhibitorySynapse s = null;

        if(iAct.getNeuron() instanceof PatternNeuron) {
            s = new PrimaryInhibitorySynapse(iAct.getNeuron(), this);
        } else if(iAct.getNeuron() instanceof PatternPartNeuron) {
            s = new InhibitorySynapse(iAct.getNeuron(), this);
        }

        s.setWeight(1.0);
        s.initInstance(iAct.getReference());

        return s.initInducedSynapse(iAct, oAct, v);
    }

    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }

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
