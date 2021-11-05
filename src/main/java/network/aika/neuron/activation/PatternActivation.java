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
import network.aika.neuron.excitatory.PatternNeuron;

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
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addSelfBindingSignal((byte) 0);
    }

    @Override
    public byte getType() {
        return 0;
    }

    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }

    @Override
    public Range getRange() {
        return getBranchBindingSignals().values().stream()
                .map(s -> s.getBindingSignalAct().getRange())
                .reduce(
                        new Range(0, 0),
                        (a, s) -> Range.join(a, s)
                );
    }
}
