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
import network.aika.fields.FieldOutput;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

import java.util.stream.Stream;

import static network.aika.fields.Fields.mul;
import static network.aika.neuron.bindingsignal.TransitionMode.MATCH_ONLY;
import static network.aika.neuron.bindingsignal.TransitionMode.PROPAGATE_ONLY;


/**
 * @author Lukas Molzberger
 */
public class FixedTerminal extends SingleTerminal {

    public FixedTerminal(State state) {
        super(state);
    }

    public static FixedTerminal fixed(State s) {
        return new FixedTerminal(s);
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        if(transition.getMode() == PROPAGATE_ONLY)
            return;

        FieldOutput bsEvent = getBSEvent(act);
        if(bsEvent == null)
            return;

        FieldOutput transitionEvent = getTransitionEvent(
                ts,
                act,
                type.invert(),
                bsEvent
        );

        transitionEvent.addEventListener(() ->
                transition.linkAndPropagate(
                        ts,
                        getBindingSignal(bsEvent),
                        type.invert()
                )
        );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        // nothing to do here
    }

    private static FieldOutput getTransitionEvent(Synapse ts, Activation act, Direction dir, FieldOutput inputEvent) {
        FieldOutput actEvent = ts.getLinkingEvent(act, dir);
        return actEvent != null ? mul("transition event (syn: " + ts + ")",
                inputEvent,
                actEvent
        ) :
                inputEvent;
    }

    public FieldOutput getBSEvent(Activation act) {
        return act != null ?
                act.getFixedBSEvent(state) :
                null;
    }

    public BindingSignal getBindingSignal(Activation act) {
        return getBindingSignal(getBSEvent(act));
    }

    public BindingSignal getBindingSignal(FieldOutput bsEvent) {
        if(bsEvent == null)
            return null;

        return ((Activation)bsEvent.getReference()).getFixedBindingSignal(state);
    }

    public boolean linkCheck(Synapse ts, BindingSignal fromBS) {
        BindingSignal existingBS = getBindingSignal(fromBS.getActivation());
        if(existingBS != null && existingBS.getOriginActivation() != fromBS.getOriginActivation())
            return false;

        return super.linkCheck(ts, fromBS);
    }

    public String toString() {
        return "fixed(" + type + ":" + state + ")";
    }
}
