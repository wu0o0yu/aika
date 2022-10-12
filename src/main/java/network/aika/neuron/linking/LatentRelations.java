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
package network.aika.neuron.linking;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.*;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.neuron.linking.LatentLinking.latentLinking;

/**
 * @author Lukas Molzberger
 */
public class LatentRelations {

    public static void expandRelation(Activation bsA, Synapse synA, Synapse synB) {
        Direction dir;
        if(synA instanceof PrimaryInputSynapse)
            dir = Direction.INPUT;
        else if(synA instanceof SamePatternSynapse)
            dir = Direction.OUTPUT;
        else return;

        Stream<Activation> relatedBSs = findLatentRelationNeurons((BindingNeuron) synA.getOutput())
                .flatMap(n -> evaluateLatentRelation(n, bsA, dir))
                .flatMap(bs -> synB.getRelatedBindingSignals(bs, INPUT));

        latentLinking(bsA, synA, synB, relatedBSs);
    }

    private static Stream<TokenActivation> evaluateLatentRelation(LatentRelationNeuron n, Activation bs, Direction dir) {
        List<TokenActivation> origins = bs.findBindingSignalOrigins(TokenActivation.class);

        return origins.stream().flatMap(o ->
                n.evaluateLatentRelation(o, dir)
        );
    }

    private static Stream<LatentRelationNeuron> findLatentRelationNeurons(BindingNeuron n) {
        return n.getInputSynapses()
                .filter(s -> s instanceof RelatedInputSynapse)
                .map(s -> s.getInput())
                .filter(lrn -> lrn instanceof LatentRelationNeuron)
                .map(lrn -> (LatentRelationNeuron)lrn);
    }
}
