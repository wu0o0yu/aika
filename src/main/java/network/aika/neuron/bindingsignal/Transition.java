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
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;

import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public interface Transition {

    void notify(Terminal t, Synapse ts, BindingSignal bs);

    void registerTransitionEvent(FixedTerminal t, Synapse ts, Activation act, FieldOutput bsEvent);

    TransitionMode getMode();

    Stream<SingleTransition> getBSPropagateTransitions(State s);

    Stream<FixedTerminal> getFixedTerminals(Synapse ts, Activation act, Direction dir);

    Stream<VariableTerminal> getVariableTerminals(Synapse ts, BindingSignal bs, Direction dir);

    default FieldOutput getTransitionEvent(Synapse ts, Activation act, Direction dir, FieldOutput inputEvent) {
        FieldOutput actEvent = ts.getLinkingEvent(act, dir);
        return actEvent != null ? mul("transition event (syn: " + ts + ")",
                inputEvent,
                actEvent
        ) :
                inputEvent;
    }
}
