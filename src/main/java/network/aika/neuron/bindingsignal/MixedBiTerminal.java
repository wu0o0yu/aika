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

import java.util.stream.Stream;

import static network.aika.fields.Fields.isTrue;

/**
 * @author Lukas Molzberger
 */
public class MixedBiTerminal extends BiTerminal<VariableTerminal> {


    public MixedBiTerminal(Direction type, BiTransition transition, VariableTerminal activeTerminal, FixedTerminal passiveTerminal) {
        this.type = type;
        this.transition = transition;
        this.activeTerminal = activeTerminal;
        this.passiveTerminal = passiveTerminal;
    }

    @Override
    public void initFixedTerminal(Synapse ts, Activation act) {
        Terminal.getPreconditionEvent(
                        ts,
                        act,
                        type.invert(),
                        passiveTerminal.getSlot(act)
                )
                .addEventListener(() ->
                        notifyInternal(ts, act)
                );
    }

    private void notifyInternal(Synapse ts, Activation act) {
        Stream<BindingSignal> bsStream = act.getBindingSignals(activeTerminal.state);
        bsStream
                .forEach(bs ->
                        transition.linkAndPropagate(ts, bs, type.invert())
                );
    }

    @Override
    public void notify(Synapse ts, BindingSignal bs) {
        if(isTrue(passiveTerminal.getSlot(bs.getActivation())))
            transition.linkAndPropagate(ts, bs, type.invert());
    }
}
