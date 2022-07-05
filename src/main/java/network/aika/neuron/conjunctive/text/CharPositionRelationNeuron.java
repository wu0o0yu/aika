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

import network.aika.direction.Direction;
import network.aika.neuron.Range;
import network.aika.neuron.activation.PatternActivation;
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
        return getModel().lookupNeuron("CP-Rel.: " + rangeBegin + "," + rangeEnd, l ->
                instantiateTemplate(true)
                        .initCharPositionRelationNeuron(rangeBegin, rangeEnd, l)
        );
    }

    private CharPositionRelationNeuron initCharPositionRelationNeuron(int rangeBegin, int rangeEnd, String l) {
        setLabel(l);

        setRangeBegin(rangeBegin);
        setRangeEnd(rangeEnd);

        getBias().receiveUpdate(-4.0);
        setAllowTraining(false);
        updateSumOfLowerWeights();
        return this;
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
    public Stream<BindingSignal> evaluateLatentRelation(PatternActivation fromOriginAct, Direction dir) {
        Document doc = (Document) fromOriginAct.getThought();
/*
        Range r = fromOriginAct.getRange();

        Range fromRange = new Range(dir == Direction.OUTPUT ? r.getBegin() + rangeBegin : r.getEnd() + rangeEnd, 0);
        Range toRange = new Range(dir == Direction.OUTPUT ? r.getEnd() + rangeEnd : r.getBegin() + rangeBegin, Integer.MAX_VALUE);

        return doc.getRelatedTokensByCharPosition(fromRange, toRange)
                .filter(tokenAct -> tokenAct.getRange() != null) // TODO filter range
                .map(tokenAct -> tokenAct.getBindingSignal(SAME))
                .map(bs ->
                        createOrLookupLatentActivation(fromOriginAct, bs, s)
                );*/
        return null;
    }
}
