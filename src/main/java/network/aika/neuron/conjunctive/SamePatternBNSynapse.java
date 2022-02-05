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
import network.aika.neuron.activation.SamePatternBNLink;
import network.aika.neuron.bindingsignal.PatternBindingSignal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.bindingsignal.BranchBindingSignal.isSeparateBranch;
import static network.aika.neuron.bindingsignal.Scope.*;

/**
 * The Same Pattern Binding Neuron Synapse is an inner synapse between two binding neurons of the same pattern.
 *
 * @author Lukas Molzberger
 */
public class SamePatternBNSynapse extends BindingNeuronSynapse<SamePatternBNSynapse, BindingNeuron, SamePatternBNLink, BindingActivation> {

    private int looseLinkingRange;
    private boolean allowLooseLinking;


    @Override
    public SamePatternBNLink createLink(BindingActivation input, BindingActivation output) {
        return new SamePatternBNLink(this, input, output);
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
    public PatternBindingSignal propagatePatternBindingSignal(SamePatternBNLink l, PatternBindingSignal iBS) {
        if(iBS.getScope() != SAME)
            return null;

        return iBS.next(l.getOutput(), false);
    }

    @Override
    public boolean checkRelatedPatternBindingSignal(PatternBindingSignal iBS, PatternBindingSignal oBS) {
        if(allowLooseLinking) {
            return iBS.getOrigin() != oBS.getOrigin() && iBS.getScope() == INPUT && oBS.getScope() == INPUT;
        } else {
            return iBS.getScope() == INPUT && oBS.getScope() == RELATED;
        }
    }

    @Override
    public boolean checkLinkingPreConditions(BindingActivation iAct, BindingActivation oAct) {
        if(isSeparateBranch(iAct, oAct))
            return false;

        if(!super.checkLinkingPreConditions(iAct, oAct))
            return false;

        PatternBindingSignal iSamePBS = iAct.getSamePatternBindingSignal();
        PatternBindingSignal oSamePBS = oAct.getSamePatternBindingSignal();

        // The Input and Output BindingActivations belong to different Patterns.
        return oSamePBS == null || oSamePBS == iSamePBS;
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
