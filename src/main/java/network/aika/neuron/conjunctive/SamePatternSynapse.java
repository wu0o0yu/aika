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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.SamePatternLink;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import static network.aika.neuron.bindingsignal.Transition.transition;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class SamePatternSynapse extends BindingNeuronSynapse<SamePatternSynapse, BindingNeuron, SamePatternLink, BindingActivation> {

    private static List<Transition> TRANSITIONS = List.of(
            transition(State.SAME, State.SAME, true, Integer.MAX_VALUE), // Same Pattern BindingSignal
            transition(State.INPUT, State.INPUT, true, Integer.MAX_VALUE) // Input BS becomes related
    );

    private int looseLinkingRange;
    private boolean allowLooseLinking;

    @Override
    public SamePatternLink createLink(BindingActivation input, BindingActivation output, boolean isSelfRef) {
        return new SamePatternLink(this, input, output, isSelfRef);
    }

    @Override
    protected double getSortingWeight() {
        if(allowLooseLinking)
            return 0.0;

        return super.getSortingWeight();
    }

    public void setLooseLinkingRange(int looseLinkingRange) {
        this.looseLinkingRange = looseLinkingRange;
    }

    public Integer getLooseLinkingRange() {
        return looseLinkingRange;
    }

    public void setAllowLooseLinking(boolean allowLooseLinking) {
        this.allowLooseLinking = allowLooseLinking;
    }

    public boolean allowLooseLinking() {
        return allowLooseLinking;
    }

    @Override
    public boolean linkingCheck(BindingSignal<BindingActivation> iBS, BindingSignal<BindingActivation> oBS) {

        if(isTemplate() && (iBS.getActivation().isNetworkInput() || oBS.getActivation().isNetworkInput()))
            return false;

        if(!iBS.getActivation().isBound())
            return false;

        if(oBS.getActivation().isBound() && iBS.getActivation().getBoundPatternBindingSignal().getOrigin() != oBS.getActivation().getBoundPatternBindingSignal().getOrigin())
            return false;

     //   if(isSeparateBranch(iAct, oAct))
     //       return false;

        if(allowLooseLinking) {
            return iBS.getOrigin() != oBS.getOrigin() &&
                    iBS.getState() == State.INPUT &&
                    oBS.getState() == State.INPUT &&
                    commonLinkingCheck(iBS, oBS);
        }

        if(!super.linkingCheck(iBS, oBS))
            return false;

        BindingSignal iSamePBS = iBS.getActivation().getBoundPatternBindingSignal();
        BindingSignal oSamePBS = oBS.getActivation().getBoundPatternBindingSignal();

        // The Input and Output BindingActivations belong to different Patterns.
        return oSamePBS == null || oSamePBS == iSamePBS;
    }

    @Override
    public List<Transition> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(allowLooseLinking);
        out.writeInt(looseLinkingRange);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        allowLooseLinking = in.readBoolean();
        looseLinkingRange = in.readInt();
    }
}
