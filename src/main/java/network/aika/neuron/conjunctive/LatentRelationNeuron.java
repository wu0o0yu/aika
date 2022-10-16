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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.LatentRelationActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.text.TokenActivation;


import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LatentRelationNeuron extends BindingNeuron {

    public abstract Stream<TokenActivation> evaluateLatentRelation(TokenActivation fromOriginAct, Direction dir);

    @Override
    public LatentRelationActivation createActivation(Thought t) {
        return new LatentRelationActivation(t.createActivationId(), t, this);
    }

    public LatentRelationActivation createOrLookupLatentActivation(TokenActivation fromOriginAct, TokenActivation toOriginAct) {
        return fromOriginAct.getToRelations().computeIfAbsent(toOriginAct, toAct -> {
            LatentRelationActivation relAct = createActivation(fromOriginAct.getThought());
            relAct.init(null, null);
            return relAct;
        });
    }
}