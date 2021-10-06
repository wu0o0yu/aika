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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.steps.activation.Linking;
import network.aika.neuron.steps.activation.TemplateLinking;

import java.util.List;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends Activation<PatternNeuron> {

    public static PatternActivation MIN_PATTERN_ACT = new PatternActivation(0, null);
    public static PatternActivation MAX_PATTERN_ACT = new PatternActivation(Integer.MAX_VALUE, null);

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);

        addBindingSignal(this, (byte) 0);
    }

    @Override
    protected Activation newInstance() {
        return new PatternActivation(id, thought, neuron);
    }

    @Override
    public byte getType() {
        return 0;
    }

    protected void registerBindingSignal(Activation targetAct, Byte scope) {
        super.registerBindingSignal(targetAct, scope);
        List<Direction> dirs = List.of(INPUT, OUTPUT);
        Linking.add(targetAct, this, scope, dirs);
        TemplateLinking.add(targetAct, this, scope, dirs);
    }

    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }
}
