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
import network.aika.fields.Fields;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.PatternNeuron;

import static network.aika.neuron.bindingsignal.State.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    protected void initFields() {
        super.initFields();

        isFinal.addEventListener(() ->
                outputLinks.values().forEach(l ->
                        l.setFinalMode()
                )
        );
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addBindingSignal(new BindingSignal(this, SAME));
    }

    @Override
    public void registerBindingSignal(BindingSignal bs) {
        super.registerBindingSignal(bs);

        if(bs.getOriginActivation() == this)
            thought.registerBindingSignalSource(this, bs);
    }

    @Override
    public Range getRange() {
        return getBindingSignals()
                .filter(s -> s.getState() == State.INPUT)
                .map(s -> s.getOriginActivation().getRange())
                .reduce(
                        new Range(0, 0),
                        (a, s) -> Range.join(a, s)
                );
    }
}
