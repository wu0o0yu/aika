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
import network.aika.neuron.activation.LatentRelationActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.bindingsignal.BSKey;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.State;


import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.State.SAME;
import static network.aika.neuron.conjunctive.PrimaryInputSynapse.SAME_INPUT_TRANSITION;
import static network.aika.neuron.conjunctive.ReversePatternSynapse.SAME_SAME_TRANSITION;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class LatentRelationNeuron extends BindingNeuron {

    public abstract Stream<BindingSignal> evaluateLatentRelation(PatternActivation fromOriginAct, Direction dir);

    @Override
    public LatentRelationActivation createActivation(Thought t) {
        return new LatentRelationActivation(t.createActivationId(), t, this);
    }
/*
    protected BindingSignal createOrLookupLatentActivation(PatternActivation fromOriginAct, BindingSignal toBS, State s) {
        PrimitiveTransition fromTransition = getTransitionByDirection(s == SAME);
        State fromState = fromTransition.next(Direction.OUTPUT);
        PrimitiveTransition toTransition = getTransitionByDirection(s != SAME);
        State toState = toTransition.next(Direction.OUTPUT);

        LatentRelationActivation latentRelAct = lookupLatentRelAct(fromOriginAct, fromState, toBS, toState);
        if(latentRelAct != null)
            return latentRelAct.getBindingSignal(fromState);

        latentRelAct = createActivation(fromOriginAct.getThought());
        latentRelAct.init(null, null);

        BindingSignal latentFromBS = latentRelAct.addLatentBindingSignal(fromOriginAct, fromTransition);
        latentRelAct.addLatentBindingSignal(toBS.getOriginActivation(), toTransition);
        return latentFromBS;
    }
*/
    private LatentRelationActivation lookupLatentRelAct(PatternActivation fromOriginAct, State fromState, BindingSignal toBS, State toState) {
        return (LatentRelationActivation) fromOriginAct.getReverseBindingSignals(this, fromState)
                .map(bs -> bs.getActivation())
                .filter(act ->
                        act.getBindingSignal(BSKey.createKey(toBS.getOriginActivation(), toState)) != null
                ).findAny()
                .orElse(null);
    }

    private PrimitiveTransition getTransitionByDirection(boolean direction) {
        return direction ?
                SAME_SAME_TRANSITION :
                SAME_INPUT_TRANSITION;
    }
}