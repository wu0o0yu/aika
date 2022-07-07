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

import java.util.InputMismatchException;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public abstract class BiTerminal<A extends SingleTerminal> implements Terminal {

    protected Direction type;
    protected BiTransition transition;
    protected A activeTerminal;
    protected FixedTerminal passiveTerminal;


    public static BiTerminal biTerminal(Direction type, BiTransition biTransition, SingleTerminal activeTerminal, FixedTerminal passiveTerminal) {
        if(activeTerminal instanceof FixedTerminal)
            return new FixedBiTerminal(type, biTransition, (FixedTerminal) activeTerminal, passiveTerminal);

        if(activeTerminal instanceof VariableTerminal)
            return new MixedBiTerminal(type, biTransition, (VariableTerminal) activeTerminal, passiveTerminal);

        throw new InputMismatchException();
    }

    @Override
    public void setType(Direction type) {
        this.type = type;
    }

    @Override
    public Direction getType() {
        return type;
    }

    @Override
    public Transition getTransition() {
        return transition;
    }

    @Override
    public Stream<BindingSignal> propagate(BindingSignal bs) {
        return Stream.concat(
                activeTerminal.propagate(bs),
                passiveTerminal.propagate(bs)
        );
    }

    @Override
    public boolean matchesState(State s) {
        return activeTerminal.matchesState(s) ||
                passiveTerminal.matchesState(s);
    }
}
