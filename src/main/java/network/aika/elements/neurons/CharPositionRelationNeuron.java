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
package network.aika.elements.neurons;

import network.aika.direction.Direction;
import network.aika.elements.activations.TokenActivation;
import network.aika.text.Document;

import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class CharPositionRelationNeuron extends LatentRelationNeuron {

    public CharPositionRelationNeuron lookupRelation(int rangeBegin, int rangeEnd) {
        return getModel().lookupNeuronByLabel("CP-Rel.: " + rangeBegin + "," + rangeEnd, l ->
                ((CharPositionRelationNeuron) instantiateTemplate())
                        .initCharPositionRelationNeuron(rangeBegin, rangeEnd, l)
        );
    }

    private CharPositionRelationNeuron initCharPositionRelationNeuron(int rangeBegin, int rangeEnd, String l) {
        setLabel(l);

        setRangeBegin(rangeBegin);
        setRangeEnd(rangeEnd);

        setAllowTraining(false);
        return this;
    }

    @Override
    public Stream<TokenActivation> evaluateLatentRelation(TokenActivation fromOriginAct, Direction dir) {
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
