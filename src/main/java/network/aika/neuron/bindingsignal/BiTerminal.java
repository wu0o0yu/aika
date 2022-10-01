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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.InputMismatchException;
import java.util.stream.Stream;

import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public abstract class BiTerminal<A extends PrimitiveTerminal> implements Terminal {

    protected Direction type;
    protected BiTransition transition;
    protected A firstTerminal;
    protected FixedTerminal secondTerminal;

    public BiTerminal(Direction type, BiTransition transition, A firstTerminal, FixedTerminal secondTerminal) {
        this.type = type;
        this.transition = transition;
        this.firstTerminal = firstTerminal;
        this.secondTerminal = secondTerminal;

        this.firstTerminal.setParent(this);
        this.secondTerminal.setParent(this);
    }

    public static BiTerminal biTerminal(Direction type, BiTransition biTransition, PrimitiveTerminal activeTerminal, FixedTerminal passiveTerminal) {
        if(activeTerminal instanceof FixedTerminal)
            return new FixedBiTerminal(type, biTransition, (FixedTerminal) activeTerminal, passiveTerminal);

        if(activeTerminal instanceof VariableTerminal)
            return new MixedBiTerminal(type, biTransition, (VariableTerminal) activeTerminal, passiveTerminal);

        throw new InputMismatchException();
    }


    public static BiTerminal optionalBiTerminal(Direction type, BiTransition biTransition, PrimitiveTerminal activeTerminal, FixedTerminal passiveTerminal) {
        if(activeTerminal instanceof FixedTerminal)
            return new OptionalFixedBiTerminal(type, biTransition, (FixedTerminal) activeTerminal, passiveTerminal);

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
    public Stream<PrimitiveTerminal> getPrimitiveTerminals() {
        return Stream.of(firstTerminal, secondTerminal);
    }

    @Override
    public void propagate(BindingSignal bs, Link l, Activation act) {
        firstTerminal.propagate(bs, l, act);
        secondTerminal.propagate(bs, l, act);
    }

    @Override
    public boolean matchesState(State s) {
        return firstTerminal.matchesState(s) ||
                secondTerminal.matchesState(s);
    }
}
