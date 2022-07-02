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
package network.aika.neuron.conjunctive.text;

import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.text.Document;

import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.State.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class TokenPositionRelationNeuron extends LatentRelationNeuron {

    private int rangeBegin = -1;
    private int rangeEnd = -1;


    public int getRangeBegin() {
        return rangeBegin;
    }

    public void setRangeBegin(int rangeBegin) {
        this.rangeBegin = rangeBegin;
    }

    public int getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(int rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public TokenPositionRelationNeuron lookupRelation(int rangeBegin, int rangeEnd) {
        return getModel().lookupNeuron("TP-Rel.: " + rangeBegin + "," + rangeEnd, l ->
                instantiateTemplate(true)
                        .initTokenPositionRelationNeuron(rangeBegin, rangeEnd, l)
        );
    }

    private TokenPositionRelationNeuron initTokenPositionRelationNeuron(int rangeBegin, int rangeEnd, String l) {
        setLabel(l);

        setRangeBegin(rangeBegin);
        setRangeEnd(rangeEnd);

        getBias().receiveUpdate(-4.0);
        setAllowTraining(false);
        updateSumOfLowerWeights();
        return this;
    }

    @Override
    public TokenPositionRelationNeuron instantiateTemplate(boolean addProvider) {
        TokenPositionRelationNeuron n = new TokenPositionRelationNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    protected Stream<BindingSignal> getRelatedBindingSignalsInternal(PatternActivation fromOriginAct, State state) {
        Document doc = (Document) fromOriginAct.getThought();

        return doc.getRelatedTokensByTokenPosition((TokenActivation) fromOriginAct, getRelFrom(state), getRelTo(state))
                .map(tokenAct -> tokenAct.getBindingSignal(SAME))
                .map(bs ->
                        createOrLookupLatentActivation(fromOriginAct, bs, state)
                );
    }

    private int getRelFrom(State s) {
        return (s == SAME ? 1 : -1) * rangeBegin;
    }

    private int getRelTo(State s) {
        return (s == SAME ? 1 : -1) * rangeEnd;
    }
}
