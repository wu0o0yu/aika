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

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.Synapse.isLatentLinking;
import static network.aika.neuron.bindingsignal.LatentRelations.expandRelation;

/**
 * @author Lukas Molzberger
 */
public class LatentLinking {

    public static void latentLinking(PrimitiveTransition t, Synapse synA, BindingSignal fromBS) {
        if(t.getMode() != TransitionMode.MATCH_AND_PROPAGATE)
            return;

        Neuron<?, ?> toNeuron = synA.getOutput();

        boolean templateEnabled = fromBS.getConfig().isTemplatesEnabled();
        toNeuron.getTargetSynapses(INPUT, templateEnabled)
                .filter(synB -> synA != synB)
                .filter(synB -> synA.getTemplate() != synB && synB.getTemplate() != synA)
                .filter(synB -> isLatentLinking(synA, synB))
                .forEach(synB ->
                        latentLinking(t, fromBS, synA, synB)
                );
    }

    private static void latentLinking(PrimitiveTransition tA, BindingSignal bsA, Synapse synA, Synapse synB) {
        Stream<PrimitiveTransition> relTrans = synB.getRelatedTransitions(tA);
        relTrans.filter(tB ->
                        tB.getMode() == TransitionMode.MATCH_AND_PROPAGATE
                )
                .forEach(tB ->
                        latentLinking(
                                tA,
                                bsA,
                                synA,
                                synB,
                                tB,
                                synB.getRelatedBindingSignals(bsA.getOriginActivation(), tB, INPUT)
                        )
                );

        expandRelation(bsA, synA, synB, tA);
    }

    public static void latentLinking(PrimitiveTransition tA, BindingSignal bsA, Synapse synA, Synapse synB, PrimitiveTransition tB, Stream<BindingSignal> bsStream) {
        Activation iActA = bsA.getActivation();
        Thought t = iActA.getThought();

        bsStream.filter(bsB ->
                        bsA != bsB &&
                                isTrue(bsB.getOnArrivedFired())
                )
                .filter(bsB ->
                        synB.checkLinkingEvent(bsB.getActivation(), INPUT)
                )
                .filter(bsB ->
                        !latentActivationExists(synA, synB, iActA, bsB.getActivation())
                )
                .forEach(bsB -> {
                    Activation oAct = synA.getOutput().createActivation(t);
                    oAct.init(synA, iActA);

                    createLink(synA, bsB, tB, iActA, oAct);
                    createLink(synB, bsA, tA, bsB.getActivation(), oAct);
                });
    }

    private static void createLink(Synapse ts, BindingSignal bs, PrimitiveTransition t, Activation iAct, Activation oAct) {
        Link l = ts.createLink(iAct, oAct);
        if(l == null)
            return;

        BindingSignal toBS = bs.next(t.getInput());
        toBS.init(l.getOutput());
        l.getOutput().addBindingSignal(toBS);
    }

    private static boolean latentActivationExists(Synapse synA, Synapse synB, Activation iActA, Activation iActB) {
        Stream<Link> linksA = iActA.getOutputLinks(synA);
        return linksA.map(lA -> lA.getOutput())
                .map(oAct -> oAct.getInputLink(synB))
                .map(lB -> lB.getInput())
                .anyMatch(iAct -> iAct == iActB);
    }
}
