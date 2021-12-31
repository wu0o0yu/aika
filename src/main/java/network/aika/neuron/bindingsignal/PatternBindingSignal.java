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

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;

/**
 * @author Lukas Molzberger
 */
public class PatternBindingSignal extends BindingSignal<PatternBindingSignal> {

    private PBSType type;
    private byte scope;

    public PatternBindingSignal(PatternActivation act, byte scope) {
        this.origin = this;
        this.activation = act;

        this.type = null;
        this.scope = scope;
    }

    public PatternBindingSignal(PatternBindingSignal parent, Activation activation, PBSType type, byte scope) {
        this.origin = parent.getOrigin();
        this.activation = activation;
        this.depth = (byte) (getDepth() + 1);

        this.type = type;
        this.scope = scope;
    }


    public PatternActivation getOriginActivation() {
        return (PatternActivation) origin.getActivation();
    }

    public boolean checkRelatedBindingSignal(Synapse s, BindingSignal toBS, Direction dir) {
        return s.checkRelatedPatternBindingSignal(
                (PatternBindingSignal) dir.getInputBindingSignal(this, toBS),
                (PatternBindingSignal) dir.getOutputBindingSignal(this, toBS)
        );
    }

    @Override
    public void link() {
        getActivation().registerPatternBindingSignal(this);
        getOriginActivation().registerReverseBindingSignal(getActivation(), this);
    }

    public boolean exists() {
        PatternBindingSignal existingBSScope = getActivation().getPatternBindingSignals().get(getOriginActivation());
        return existingBSScope != null && existingBSScope.getScope() <= getScope();
    }

    @Override
    public boolean checkPropagate() {
        return getActivation().checkPropagatePatternBindingSignal(this);
    }

    protected BindingSignal propagate(Link l) {
        return l.getSynapse().propagatePatternBindingSignal(l, this);
    }


    public PBSType getType() {
        return type;
    }

    public byte getScope() {
        return scope;
    }

    public String toString() {
        return "[" + getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ",t:" + type + ",s:" + scope + ",d:" + getDepth() + "]";
    }
}
