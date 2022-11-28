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
package network.aika.neuron.conjunctive;

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.QueueSumField;
import network.aika.fields.SumField;
import network.aika.neuron.activation.LatentRelationActivation;
import network.aika.neuron.activation.text.TokenActivation;


import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LatentRelationNeuron extends BindingNeuron {

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


    public abstract Stream<TokenActivation> evaluateLatentRelation(TokenActivation fromOriginAct, Direction dir);

    @Override
    protected SumField initBias() {
        return (SumField) new QueueSumField(this, "bias")
                .setInitialValue(10.0);
    }

    @Override
    public LatentRelationActivation createActivation(Thought t) {
        return new LatentRelationActivation(t.createActivationId(), t, this);
    }
}