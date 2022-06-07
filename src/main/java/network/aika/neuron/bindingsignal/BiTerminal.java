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

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public class BiTerminal implements Terminal {

    private BiTransition transition;
    private FixedTerminal activeTerminal;
    private FixedTerminal passiveTerminal;

    public BiTerminal(BiTransition transition, FixedTerminal activeTerminal, FixedTerminal passiveTerminal) {
        this.transition = transition;
        this.activeTerminal = activeTerminal;
        this.passiveTerminal = passiveTerminal;
    }

    @Override
    public Direction getType() {
        return Direction.INPUT;
    }

    @Override
    public Transition getTransition() {
        return transition;
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        FieldOutput activeBSEvent = activeTerminal.getBSEvent(act);

        FieldOutput inputEvent = mul(
                "input bi transition event",
                activeTerminal.getBSEvent(act),
                passiveTerminal.getBSEvent(act)
        );

        FieldOutput transitionEvent = getTransitionEvent(ts, act, OUTPUT, inputEvent);
        transitionEvent.addEventListener(() ->
                transition.linkAndPropagate(ts, activeBSEvent, OUTPUT)
        );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        // nothing to do
    }

    private static FieldOutput getTransitionEvent(Synapse ts, Activation act, Direction dir, FieldOutput inputEvent) {
        FieldOutput actEvent = ts.getLinkingEvent(act, dir);
        return actEvent != null ? mul("transition event (syn: " + ts + ")",
                inputEvent,
                actEvent
        ) :
                inputEvent;
    }

    @Override
    public BindingSignal propagate(BindingSignal bs) {
        BindingSignal nextBS = activeTerminal.propagate(bs);
        if(nextBS != null)
            return nextBS;
        return passiveTerminal.propagate(bs);
    }
}
