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

import java.util.InputMismatchException;
import java.util.stream.Stream;


/**
 * @author Lukas Molzberger
 */
public abstract class BiTerminal<A extends PrimitiveTerminal> extends Terminal<BiTransition> {

    protected Direction type;
    protected A firstTerminal;
    protected FixedTerminal secondTerminal;

    public BiTerminal(Direction type, A firstTerminal, FixedTerminal secondTerminal) {
        this.type = type;
        this.firstTerminal = firstTerminal;
        this.secondTerminal = secondTerminal;

        this.firstTerminal.setParent(this);
        this.secondTerminal.setParent(this);
    }

    public static BiTerminal biTerminal(Direction type, PrimitiveTerminal activeTerminal, FixedTerminal passiveTerminal) {
        if(activeTerminal instanceof FixedTerminal)
            return new FixedBiTerminal(type, (FixedTerminal) activeTerminal, passiveTerminal);

        if(activeTerminal instanceof VariableTerminal)
            return new MixedBiTerminal(type, (VariableTerminal) activeTerminal, passiveTerminal);

        throw new InputMismatchException();
    }


    public static BiTerminal optionalBiTerminal(Direction type, PrimitiveTerminal activeTerminal, FixedTerminal passiveTerminal) {
        if(activeTerminal instanceof FixedTerminal)
            return new OptionalFixedBiTerminal(type, (FixedTerminal) activeTerminal, passiveTerminal);

        if(activeTerminal instanceof VariableTerminal)
            return new MixedBiTerminal(type, (VariableTerminal) activeTerminal, passiveTerminal);

        throw new InputMismatchException();
    }

    @Override
    public Direction getType() {
        return type;
    }

    @Override
    public Stream<PrimitiveTerminal> getPrimitiveTerminals() {
        return Stream.of(firstTerminal, secondTerminal);
    }

    public A getFirstTerminal() {
        return firstTerminal;
    }

    public FixedTerminal getSecondTerminal() {
        return secondTerminal;
    }

    @Override
    public boolean matchesState(State s) {
        return firstTerminal.matchesState(s) ||
                secondTerminal.matchesState(s);
    }
}
