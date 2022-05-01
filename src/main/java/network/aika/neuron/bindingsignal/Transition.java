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
import network.aika.fields.Field;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.BindingSignal.originEquals;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private State input;
    private State output;
    private boolean check;
    private boolean checkPrimaryInput;
    private boolean checkSamePrimaryInput;
    private boolean checkBoundToSamePattern;
    private boolean checkIfPrimaryInputAlreadyExists;
    private boolean checkLooseLinking;
    private boolean checkSelfRef;


    private Integer propagate;

    private Transition(State input, State output) {
        this.input = input;
        this.output = output;
    }

    public static Transition transition(State input, State output) {
        return new Transition(input, output);
    }

    public Transition setCheck(boolean check) {
        this.check = check;
        return this;
    }

    public Transition setCheckPrimaryInput(boolean checkPrimaryInput) {
        this.checkPrimaryInput = checkPrimaryInput;
        return this;
    }

    public Transition setCheckSamePrimaryInput(boolean checkSamePrimaryInput) {
        this.checkSamePrimaryInput = checkSamePrimaryInput;
        return this;
    }

    public Transition setCheckBoundToSamePattern(boolean checkBoundToSamePattern) {
        this.checkBoundToSamePattern = checkBoundToSamePattern;
        return this;
    }

    public Transition setCheckIfPrimaryInputAlreadyExists(boolean checkIfPrimaryInputAlreadyExists) {
        this.checkIfPrimaryInputAlreadyExists = checkIfPrimaryInputAlreadyExists;
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

    public Transition setPropagate(Integer propagate) {
        this.propagate = propagate;
        return this;
    }

    public State getInput() {
        return input;
    }

    public State getOutput() {
        return output;
    }

    public boolean isCheck() {
        return check;
    }

    public boolean isCheckPrimaryInput() {
        return checkPrimaryInput;
    }

    public Integer getPropagate() {
        return propagate;
    }

    public State next(Direction dir) {
        return dir == OUTPUT ? output : input;
    }

    public boolean eventCheck(Synapse ts, BindingSignal bs, Direction dir) {
        if(!check)
            return false;

        if(dir.invert().getState(this) != bs.getState())
            return false;

        if(dir == OUTPUT && checkSamePrimaryInput && !verifySamePrimaryInput(bs, (BindingNeuron) ts.getOutput()))
            return false;

        return true;
    }

    public boolean linkCheck(Synapse ts, BindingSignal iBS, BindingSignal oBS) {
        if(!check)
            return false;

        if(iBS.getState() != input)
            return false;

        if(oBS == null && !ts.isAllowPropagate())
            return false;

        if(oBS != null && oBS.getState() != output)
            return false;

        if(checkLooseLinking && !ts.allowLooseLinking()) // && iBS.getOrigin() != oBS.getOrigin()
            return false;

        if(checkBoundToSamePattern && !checkBoundToSamePattern(iBS, oBS))
            return false;

        return true;
    }

    protected boolean checkBoundToSamePattern(BindingSignal iBS, BindingSignal oBS) {
        Field<BindingSignal> iOnBound = iBS.getActivation().getOnBoundPattern();
        Field<BindingSignal> oOnBound = oBS.getActivation().getOnBoundPattern();

        return oOnBound.getReference() == null || originEquals(iOnBound.getReference(), oOnBound.getReference());
    }

    protected boolean verifySamePrimaryInput(BindingSignal iBS, BindingNeuron on) {
        Activation<?> iAct = iBS.getActivation();
        BindingSignal boundPatternBS = iAct.getOnBoundPattern().getReference();
        if(boundPatternBS == null)
            return false;

        PrimaryInputSynapse primaryInputSyn = on.getPrimaryInputSynapse();
        if(primaryInputSyn == null)
            return false;

        Activation originAct = boundPatternBS.getOriginActivation();
        return originAct.getReverseBindingSignals(primaryInputSyn.getInput())
                .findAny()
                .isPresent();
    }

    public boolean checkPropagate(State from) {
        if(propagate == 0)
            return false;

        return from == input;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " Check:" + check +
                " CheckPrimaryInput:" + checkPrimaryInput +
                " CheckSamePrimaryInput:" + checkSamePrimaryInput +
                " CheckBoundToSamePattern:" + checkBoundToSamePattern +
                " CheckIfPrimaryInputAlreadyExists:" + checkIfPrimaryInputAlreadyExists +
                " CheckLooseLinking:" + checkLooseLinking +
                " CheckSelfRef:" + checkSelfRef +
                " Propagate:" + (propagate == Integer.MAX_VALUE ? "UNLIMITED" : propagate);
    }
}
