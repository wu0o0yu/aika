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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.conjunctive.*;

import java.util.stream.Stream;

import static network.aika.neuron.bindingsignal.LatentLinking.latentLinking;
import static network.aika.neuron.conjunctive.PrimaryInputSynapse.SAME_RELATED_SAME_TRANSITION;
import static network.aika.neuron.conjunctive.SamePatternSynapse.INPUT_TRANSITION;

/**
 * @author Lukas Molzberger
 */
public class LatentRelations {

    public static void expandRelation(BindingSignal bsA, Synapse synA, Synapse synB, PrimitiveTransition tA) {
        Direction dir;

        PrimitiveTransition tB;
        if(samePatternToPrimaryInput(synA, synB) && tA == INPUT_TRANSITION) {
            dir = Direction.OUTPUT;
            tB = SAME_RELATED_SAME_TRANSITION;
        } else if(samePatternToPrimaryInput(synB, synA) && tA == SAME_RELATED_SAME_TRANSITION) {
            dir = Direction.INPUT;
            tB = INPUT_TRANSITION;
        } else
            return;

        Stream<BindingSignal> relatedBSs = findLatentRelationNeurons((BindingNeuron) synA.getOutput())
                .flatMap(n -> n.evaluateLatentRelation(bsA.getOriginActivation(), dir))
                .flatMap(bs -> bs.getOriginActivation().getReverseBindingSignals(
                        synB.getInput(),
                        tB.getInput().getState()
                ));

        latentLinking(tA, bsA, synA, synB, tB, relatedBSs);
    }

    private static boolean samePatternToPrimaryInput(Synapse synA, Synapse synB) {
        return synA instanceof SamePatternSynapse &&
                synB instanceof PrimaryInputSynapse;
    }

    private static Stream<LatentRelationNeuron> findLatentRelationNeurons(BindingNeuron n) {
        return n.getInputSynapses()
                .filter(s -> s instanceof RelatedInputSynapse)
                .map(s -> s.getInput())
                .filter(lrn -> lrn instanceof LatentRelationNeuron)
                .map(lrn -> (LatentRelationNeuron)lrn);
    }
}
