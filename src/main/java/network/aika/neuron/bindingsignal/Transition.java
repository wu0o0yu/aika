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

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;
import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    protected State input;
    protected State output;
    protected TransitionMode transitionMode;

    protected Transition(State input, State output, TransitionMode transitionMode) {
        this.input = input;
        this.output = output;
        this.transitionMode = transitionMode;
    }

    public TransitionListener createListener(Synapse ts, BindingSignal bs, Direction dir) {
        return new TransitionListener(this, bs, dir, ts);
    }

    public static Transition transition(State input, State output, TransitionMode transitionMode) {
        return new Transition(input, output, transitionMode);
    }

    public State getInput() {
        return input;
    }

    public State getOutput() {
        return output;
    }

    public TransitionMode getPropagateBS() {
        return transitionMode;
    }

    public State next(Direction dir) {
        return dir == OUTPUT ? output : input;
    }

    public boolean check(BindingSignal bs, Direction dir) {
        return transitionMode != PROPAGATE_ONLY &&
                dir.getFromState(this) == bs.getState();
    }

    public boolean linkCheck(Synapse ts, BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        if(transitionMode == PROPAGATE_ONLY)
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
        if(transitionMode == MATCH_ONLY)
            return false;

        return from == input;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " PropagateBS:" + transitionMode;
    }
}
