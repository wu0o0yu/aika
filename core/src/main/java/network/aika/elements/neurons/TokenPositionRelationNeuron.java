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

import network.aika.Model;
import network.aika.enums.direction.Direction;
import network.aika.elements.activations.TokenActivation;
import network.aika.text.Document;

import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class TokenPositionRelationNeuron extends LatentRelationNeuron {


    public static TokenPositionRelationNeuron lookupRelation(Model m, int rangeBegin, int rangeEnd) {
        return m.lookupNeuronByLabel("Prev. Token Rel.: " + rangeBegin + "," + rangeEnd, l -> {
                    TokenPositionRelationNeuron n = new TokenPositionRelationNeuron();
                    n.addProvider(m);
                    n.initTokenPositionRelationNeuron(rangeBegin, rangeEnd, l);
                    return n;
                }
        );
    }

    private TokenPositionRelationNeuron initTokenPositionRelationNeuron(int rangeBegin, int rangeEnd, String l) {
        setLabel(l);

        setRangeBegin(rangeBegin);
        setRangeEnd(rangeEnd);

        setAllowTraining(false);
        return this;
    }

    @Override
    public Stream<TokenActivation> evaluateLatentRelation(TokenActivation fromOriginAct, Direction dir) {
        Document doc = (Document) fromOriginAct.getThought();

        return doc.getRelatedTokensByTokenPosition(fromOriginAct, getRelFrom(dir), getRelTo(dir));
    }

    private int getRelFrom(Direction dir) {
        return dir == Direction.INPUT ? getRangeBegin() : -getRangeEnd();
    }

    private int getRelTo(Direction dir) {
        return dir == Direction.INPUT ? getRangeEnd() : -getRangeBegin();
    }
}
