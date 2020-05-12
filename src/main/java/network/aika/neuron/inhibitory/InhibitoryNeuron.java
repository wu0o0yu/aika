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
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.Config;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.linker.LinkGraphs;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

import java.util.Collection;
import java.util.List;

import static network.aika.neuron.activation.linker.Mode.LINKING;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryNeuron extends INeuron<InhibitorySynapse> {

    public static byte type;

    private byte outerType;

    protected InhibitoryNeuron() {
        super();
    }

    public InhibitoryNeuron(Neuron p) {
        super(p);
    }

    public InhibitoryNeuron(Model model, String label, byte outerType, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
        this.outerType = outerType;
    }

    public double propagateRangeCoverage(Activation iAct) {
        return iAct.rangeCoverage;
    }

    @Override
    public void linkForwards(Activation act) {
        super.linkForwards(act);

        LinkGraphs.inhibitoryLinkT.followForwards(LINKING, act);
    }

    @Override
    public void linkBackwards(Link l) {
        // Nothing to do!
    }

    @Override
    public void linkPosRecSynapses(Activation act) {

    }

    @Override
    public void induceStructure(Activation act) {

    }

    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }

    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < -getBias();
    }

    public byte getType() {
        return type;
    }

    @Override
    public byte getOuterType() {
        return outerType;
    }

    public double getTotalBias(boolean assumePosRecLinks, Synapse.State state) {
        return getBias(state);
    }

    @Override
    public boolean hasPositiveRecurrentSynapses() {
        return false;
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }

    @Override
    public void addInputSynapse(InhibitorySynapse inhibitorySynapse) {
    }

    @Override
    public void addOutputSynapse(Synapse synapse) {
    }

    @Override
    public void removeInputSynapse(InhibitorySynapse inhibitorySynapse) {
    }

    @Override
    public void removeOutputSynapse(Synapse s) {
    }

    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        commitBias();

        for (Synapse s : modifiedSynapses) {
            s.commit();
        }

        setModified();
    }

    public boolean isMature() {
        return true;
    }

    @Override
    public void dumpStat() {
    }

    public String typeToString() {
        return "INHIBITORY";
    }
}
