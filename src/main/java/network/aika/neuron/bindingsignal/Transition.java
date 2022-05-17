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
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.bindingsignal.PropagateBS.FALSE;
import static network.aika.neuron.bindingsignal.PropagateBS.ONLY;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private State input;
    private State output;

    private boolean checkPrimaryInput;
    private boolean checkSamePrimaryInput;
    private boolean checkLooseLinking;
    private boolean checkSelfRef;

    private PropagateBS propagateBS = PropagateBS.TRUE;

    protected Transition(State input, State output) {
        this.input = input;
        this.output = output;
    }

    public TransitionListener createListener(Synapse ts, BindingSignal bs, Direction dir) {
        return new TransitionListener(this, bs, dir, ts);
    }

    public static Transition transition(State input, State output) {
        return new Transition(input, output);
    }

    public Transition setCheckPrimaryInput(boolean checkPrimaryInput) {
        this.checkPrimaryInput = checkPrimaryInput;
        return this;
    }

    public Transition setCheckSamePrimaryInput(boolean checkSamePrimaryInput) {
        this.checkSamePrimaryInput = checkSamePrimaryInput;
        return this;
    }

    public Transition setCheckLooseLinking(boolean checkLooseLinking) {
        this.checkLooseLinking = checkLooseLinking;
        return this;
    }

    public Transition setCheckSelfRef(boolean checkSelfRef) {
        this.checkSelfRef = checkSelfRef;
        return this;
    }

    public Transition setPropagateBS(PropagateBS propagateBS) {
        this.propagateBS = propagateBS;
        return this;
    }

    public State getInput() {
        return input;
    }

    public State getOutput() {
        return output;
    }

    public boolean isCheckPrimaryInput() {
        return checkPrimaryInput;
    }

    public PropagateBS getPropagateBS() {
        return propagateBS;
    }

    public State next(Direction dir) {
        return dir == OUTPUT ? output : input;
    }

    public boolean check(BindingSignal bs, Direction dir) {
        return propagateBS != ONLY &&
                dir.getFromState(this) == bs.getState();
    }

    public boolean linkCheck(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(propagateBS == ONLY)
            return false;

        if(fromBS.getOrigin() != toBS.getOrigin())
            return false;

        if (!isTrue(ts.getLinkingEvent(toBS, dir)))
            return false;

        if(dir.getToState(this) != toBS.getState())
            return false;

        if(checkSamePrimaryInput && !verifySamePrimaryInput(
                dir.getInput(fromBS, toBS),
                (BindingNeuron) ts.getOutput()
        ))
            return false;

        if(checkLooseLinking && !ts.allowLooseLinking())
            return false;

        return true;
    }

    private boolean verifySamePrimaryInput(BindingSignal refBS, BindingNeuron on) {
        Activation originAct = refBS.getOriginActivation();
        PrimaryInputSynapse primaryInputSyn = on.getPrimaryInputSynapse();
        if(primaryInputSyn == null)
            return false;

        return originAct.getReverseBindingSignals(primaryInputSyn.getInput())
                .findAny()
                .isPresent();
    }

    public boolean checkPropagate(State from) {
        if(propagateBS == FALSE)
            return false;

        return from == input;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " CheckPrimaryInput:" + checkPrimaryInput +
                " CheckSamePrimaryInput:" + checkSamePrimaryInput +
                " CheckLooseLinking:" + checkLooseLinking +
                " CheckSelfRef:" + checkSelfRef +
                " PropagateBS:" + propagateBS;
    }
}
