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

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.Objects;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.Synapse.isLatentLinking;
import static network.aika.neuron.linking.LatentRelations.expandRelation;

/**
 * @author Lukas Molzberger
 */
public class LatentLinking {

    public static void latentLinking(Activation bsA, Synapse synA, Synapse synB, Stream<Activation> bsStream) {
        Thought t = bsA.getThought();

        bsStream
                .filter(bsB ->
                        bsA != bsB &&
                                isTrue(bsB.getIsFired())
                )
                .filter(bsB ->
                        synB.checkLinkingEvent(bsB, INPUT)
                )
                .filter(bsB ->
                        !latentActivationExists(synA, synB, bsA, bsB)
                )
                .forEach(bsB -> {
                    Activation oAct = synA.getOutput().createActivation(t);
                    oAct.init(synA, bsA);

                    synA.createLink(bsA, oAct);
                    synB.createLink(bsB, oAct);
                });
    }

    private static boolean latentActivationExists(Synapse synA, Synapse synB, Activation iActA, Activation iActB) {
        Stream<Link> linksA = iActA.getOutputLinks(synA);
        return linksA.map(lA -> lA.getOutput())
                .map(oAct -> oAct.getInputLink(synB))
                .filter(Objects::nonNull)
                .map(lB -> lB.getInput())
                .anyMatch(iAct -> iAct == iActB);
    }
}
