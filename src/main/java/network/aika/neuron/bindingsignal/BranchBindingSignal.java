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
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.Link;

import java.util.Optional;

/**
 * @author Lukas Molzberger
 */
public class BranchBindingSignal extends BindingSignal<BranchBindingSignal> {

    public BranchBindingSignal(BindingActivation act) {
        this.origin = this;
        this.activation = act;
        this.depth = 0;
    }

    private BranchBindingSignal(BranchBindingSignal parent, Activation activation) {
        this.parent = parent;
        this.origin = parent.getOrigin();
        this.activation = activation;
        this.depth = (byte) (getDepth() + 1);
    }

    public static boolean isSeparateBranch(Activation<?> iAct, Activation<?> oAct) {
        Optional<BranchBindingSignal> branchBindingSignal = oAct.getBranchBindingSignals()
                .values()
                .stream()
                .filter(bs -> bs.getDepth() > 0)
                .findAny();

        return branchBindingSignal.isPresent() &&
                !iAct.getBranchBindingSignals().containsKey(
                        branchBindingSignal.get().getOriginActivation()
                );
    }

    public BranchBindingSignal next(Activation act) {
        return new BranchBindingSignal(this, act);
    }

    @Override
    public boolean checkPropagate() {
        return getActivation().checkPropagateBranchBindingSignal(this);
    }


    public boolean checkRelatedBindingSignal(Synapse s, BindingSignal outputBS, Activation oAct) {
        return s.checkRelatedBranchBindingSignal(this, (BranchBindingSignal) outputBS);
    }

    protected BindingSignal propagate(Link l) {
        return l.getSynapse().propagateBranchBindingSignal(l, this);
    }

    @Override
    public void link() {
        getActivation().registerBranchBindingSignal(this);
        getOriginActivation().registerReverseBindingSignal(getActivation(), this);
    }

    public BindingActivation getOriginActivation() {
        return (BindingActivation) origin.getActivation();
    }

    public boolean exists() {
        return getActivation().getBranchBindingSignals().containsKey(getOriginActivation());
    }

    public String toString() {
        return "[BRANCH:" + getOriginActivation().getId() + ":" + getOriginActivation().getLabel() + ",d:" + getDepth() + "]";
    }
}
