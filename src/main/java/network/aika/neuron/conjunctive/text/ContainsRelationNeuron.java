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
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.text.Document;

import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.State.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class ContainsRelationNeuron extends LatentRelationNeuron {

    public enum Direction {
        CONTAINS,
        CONTAINED_IN
    }

    private Direction direction;

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public ContainsRelationNeuron lookupRelation(Direction direction) {
        return getModel().lookupNeuron("Contains-Rel.: " + direction, l -> {
            ContainsRelationNeuron n = instantiateTemplate(true);
            n.setLabel(l);

            n.setDirection(direction);

            n.getBias().receiveUpdate(-4.0);
            n.setAllowTraining(false);
            n.updateSumOfLowerWeights();
            return n;
        });
    }

    @Override
    public ContainsRelationNeuron instantiateTemplate(boolean addProvider) {
        ContainsRelationNeuron n = new ContainsRelationNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    protected Stream<BindingSignal> getRelatedBindingSignalsInternal(BindingSignal fromBS) {
        boolean dir = fromBS.getLink() == null;
        Document doc = (Document) fromBS.getThought();
        return doc.getRelatedTokensByTokenPosition(fromBS, (dir ? 1 : -1) * rangeBegin, this)
                .map(tokenAct -> tokenAct.getBindingSignal(SAME))
                .map(bs ->
                        createLatentActivation(fromBS, bs, dir)
                );
    }
}
