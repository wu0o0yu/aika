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
    private PropagateBS propagateBS = PropagateBS.TRUE;

    private boolean checkPrimaryInput;

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

    public static Transition transition(State input, State output, PropagateBS propagateBS) {
        return transition(input, output)
                .setPropagateBS(propagateBS);
    }

    public Transition setCheckPrimaryInput(boolean checkPrimaryInput) {
        this.checkPrimaryInput = checkPrimaryInput;
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

        return true;
    }

    public boolean checkPropagate(State from) {
        if(propagateBS == FALSE)
            return false;

        return from == input;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " PropagateBS:" + propagateBS +
                " CheckPrimaryInput:" + checkPrimaryInput;
    }
}
