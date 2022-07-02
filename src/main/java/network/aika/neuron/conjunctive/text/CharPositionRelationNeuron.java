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

import network.aika.Thought;
import network.aika.neuron.Range;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.text.Document;

import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.State.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class CharPositionRelationNeuron extends LatentRelationNeuron {

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

    public CharPositionRelationNeuron lookupRelation(int rangeBegin, int rangeEnd) {
        return getModel().lookupNeuron("CP-Rel.: " + rangeBegin + "," + rangeEnd, l -> {
            CharPositionRelationNeuron n = instantiateTemplate(true);
            n.setLabel(l);

            n.setRangeBegin(rangeBegin);
            n.setRangeEnd(rangeEnd);

            n.getBias().receiveUpdate(-4.0);
            n.setAllowTraining(false);
            n.updateSumOfLowerWeights();
            return n;
        });
    }

    @Override
    public CharPositionRelationNeuron instantiateTemplate(boolean addProvider) {
        CharPositionRelationNeuron n = new CharPositionRelationNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    protected Stream<BindingSignal> getRelatedBindingSignalsInternal(BindingSignal fromBS) {
        boolean dir = fromBS.getLink() == null;
        Document doc = (Document) fromBS.getThought();

        TokenActivation fromTokenAct = (TokenActivation) fromBS.getOriginActivation();
        Range r = fromTokenAct.getRange();

        Range fromRange = new Range(dir ? r.getBegin() + rangeBegin : r.getEnd() + rangeEnd, 0);
        Range toRange = new Range(dir ? r.getEnd() + rangeEnd : r.getBegin() + rangeBegin, Integer.MAX_VALUE);

        return doc.getRelatedTokensByCharPosition(fromRange, toRange)
                .filter(tokenAct -> tokenAct.getRange() != null) // TODO filter range
                .map(tokenAct -> tokenAct.getBindingSignal(SAME))
                .map(bs ->
                        createLatentActivation(fromBS, bs, dir)
                );
    }
}
