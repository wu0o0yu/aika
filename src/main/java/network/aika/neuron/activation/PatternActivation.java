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
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.conjunctive.PatternNeuron;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    protected Map<Activation<?>, PatternBindingSignal> reverseBindingSignals = new TreeMap<>();

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    public void onFinal() {
        super.onFinal();

        outputLinks.values().forEach(l ->
                l.setFinalMode()
        );
    }

    @Override
    public boolean checkAllowPropagate() {
        return super.checkAllowPropagate();
    }

    @Override
    public void registerPatternBindingSignal(PatternBindingSignal pbs) {
        super.registerPatternBindingSignal(pbs);

        if(pbs.getOriginActivation() == this)
            thought.registerPatternBindingSignalSource(this, pbs);
    }

    public void registerReverseBindingSignal(Activation targetAct, PatternBindingSignal bindingSignal) {
        reverseBindingSignals.put(targetAct, bindingSignal);
    }

    @Override
    public Stream<PatternBindingSignal> getReverseBindingSignals() {
        return reverseBindingSignals.values().stream();
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addBindingSignal(new PatternBindingSignal(this));
    }

    public void addFeedbackSteps() {
    }

    public boolean checkPropagatePatternBindingSignal(PatternBindingSignal bs) {
        return bs.getOriginActivation() == this;
    }

    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }

    @Override
    public Range getRange() {
        return getBranchBindingSignals().values().stream()
                .map(s -> s.getOriginActivation().getRange())
                .reduce(
                        new Range(0, 0),
                        (a, s) -> Range.join(a, s)
                );
    }
}
