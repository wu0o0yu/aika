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
package network.aika.neuron.bindingsignal;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;

import java.util.stream.Stream;


/**
 * @author Lukas Molzberger
 */
public class PatternBindingSignal extends BindingSignal<PatternBindingSignal> {

    private boolean isInput;
    private boolean isRelated;

    public PatternBindingSignal(PatternActivation act) {
        this.origin = this;
        this.activation = act;
    }

    protected PatternBindingSignal(PatternBindingSignal parent, boolean isInput, boolean isRelated) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.isInput = isInput;
        this.isRelated = isRelated;
        this.depth = (byte) (getDepth() + 1);
    }


    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public boolean isRelated() {
        return isRelated;
    }

    public void setRelated(boolean related) {
        isRelated = related;
    }

    @Override
    public Stream<? extends Synapse> getTargetSynapses(Neuron fromN, boolean postFired, boolean template) {
        return fromN.getTargetSynapses(postFired, template);
    }

    public PatternBindingSignal next(boolean isInput, boolean isRelated) {
        return new PatternBindingSignal(this, isInput, isRelated);
    }

    public PatternActivation getOriginActivation() {
        return (PatternActivation) origin.getActivation();
    }

    public boolean checkRelatedBindingSignal(Synapse s, BindingSignal outputBS) {
        return s.checkRelatedPatternBindingSignal(this, (PatternBindingSignal) outputBS);
    }

    @Override
    public void link() {
        getActivation().registerPatternBindingSignal(this);
        getOriginActivation().registerReverseBindingSignal(getActivation(), this);
    }

    public boolean exists() {
        PatternBindingSignal existingBSScope = getActivation().getPatternBindingSignals().get(getOriginActivation());
        if(existingBSScope == null)
            return false;

        return existingBSScope.isInput() == isInput() && existingBSScope.isRelated() == isRelated();
    }

    @Override
    public boolean checkPropagate() {
        return getActivation().checkPropagatePatternBindingSignal(this);
    }

    protected BindingSignal propagate(Link l) {
        PatternBindingSignal nextPBS = l.getSynapse().transitionPatternBindingSignal(this, true);
        if(nextPBS != null)
            nextPBS.activation = l.getOutput();

        return nextPBS;
    }

    public boolean match(PatternBindingSignal oBS) {
        return isInput == oBS.isInput && isRelated == oBS.isRelated;
    }

    public String toString() {
        return super.toString() + ", isInput:" + isInput + ", isRelated:" + isRelated;
    }
}
