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
import network.aika.fields.Field;
import network.aika.fields.FieldOutput;
import network.aika.neuron.Neuron;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.BSKey;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.PatternNeuron;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.BSKey.COMPARATOR;
import static network.aika.neuron.bindingsignal.State.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    protected NavigableMap<BSKey, BindingSignal> reverseBindingSignals = new TreeMap<>(COMPARATOR);

    protected Range range;

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    public BindingSignal getAbstractBindingSignal() {
        return getBindingSignal(ABSTRACT_SAME);
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);

        new BindingSignal(this, SAME);
    }

    @Override
    public void registerBindingSignal(BindingSignal bs) {
        super.registerBindingSignal(bs);

        if(bs.getState() == INPUT) 
            range = Range.join(range, bs.getOriginActivation().getRange());
    }

    public void registerReverseBindingSignal(BindingSignal bindingSignal) {
        reverseBindingSignals.put(BSKey.createReverseKey(bindingSignal), bindingSignal);
    }

    public Stream<BindingSignal> getReverseBindingSignals(Neuron toNeuron, State s) {
        return reverseBindingSignals.subMap(
                new BSKey(toNeuron, s.ordinal(), 0),
                new BSKey(toNeuron, s.ordinal(), Integer.MAX_VALUE)
        ).values().stream();
    }

    @Override
    public Range getRange() {
        return range;
    }
}
