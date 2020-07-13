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
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;

import java.util.Collections;
import java.util.List;

import static network.aika.neuron.Sign.NEG;
import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Activation.TOLERANCE;
import static network.aika.neuron.activation.Direction.INPUT;


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

    public void tryToLink(Activation iAct, Activation oAct) {
    }

    @Override
    public byte getType() {
        return type;
    }

    protected void propagateCost(Activation act) {
        double cost = 0.0;

//        act.getLinks(INPUT).mapToDouble(l -> l.getInput().);

        if(Math.abs(cost) < TOLERANCE) {
            return;
        }

        act.getMutableGradient().gradient += cost;
    }

    @Override
    public Synapse getInputSynapse(NeuronProvider n) {
        throw new UnsupportedOperationException();
    }

    public double propagateRangeCoverage(Link l) {
        return l.getInput().getRangeCoverage();
    }

    public void induceNeuron(Activation act) {
        return;
    }

    public Synapse induceSynapse(Activation iAct, Activation oAct) {
        InhibitorySynapse s = new InhibitorySynapse(iAct.getNeuron(), (InhibitoryNeuron) oAct.getNeuron());
        s.setPropagate(true);
        s.setWeight(1.0);
        s.link();
        return s;
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
    public void addInputSynapse(InhibitorySynapse s) {
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
