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
import network.aika.neuron.bindingsignal.PatternBindingSignal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class SamePatternSynapse extends BindingNeuronSynapse<SamePatternSynapse, BindingNeuron, SamePatternLink, BindingActivation> {

    private int looseLinkingRange;
    private boolean allowLooseLinking;


    @Override
    public SamePatternLink createLink(BindingActivation input, BindingActivation output) {
        return new SamePatternLink(this, input, output);
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
    public boolean checkLinkingPreConditions(BindingActivation iAct, BindingActivation oAct) {
        if(oAct.isBound() && iAct.getBoundPatternBindingSignal().getOrigin() != oAct.getBoundPatternBindingSignal().getOrigin())
            return false;

     //   if(isSeparateBranch(iAct, oAct))
     //       return false;

        if(!super.checkLinkingPreConditions(iAct, oAct))
            return false;

        PatternBindingSignal iSamePBS = iAct.getSamePatternBindingSignal();
        PatternBindingSignal oSamePBS = oAct.getSamePatternBindingSignal();

        // The Input and Output BindingActivations belong to different Patterns.
        return oSamePBS == null || oSamePBS == iSamePBS;
    }

    @Override
    public PatternBindingSignal transitionPatternBindingSignal(PatternBindingSignal iBS, boolean propagate) {
        if(!iBS.isInput())
            return iBS.next(false, false); // Same Pattern BindingSignal
        else
            return iBS.next(true, true); // Input BS becomes related
    }

    @Override
    public boolean checkRelatedPatternBindingSignal(PatternBindingSignal iBS, PatternBindingSignal oBS) {
        if(allowLooseLinking) {
            return iBS.getOrigin() != oBS.getOrigin() && iBS.isInput() && oBS.isInput();
        } else {
            return super.checkRelatedPatternBindingSignal(iBS, oBS);
        }
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
