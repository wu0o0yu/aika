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

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.bindingsignal.LatentRelations.expandRelation;


/**
 * @author Lukas Molzberger
 */
public class LatentLinking {


    public static void latentLinking(PrimitiveTransition t, Synapse synA, BindingSignal fromBS) {
        Neuron<?, ?> toNeuron = synA.getOutput();

        boolean templateEnabled = fromBS.getConfig().isTemplatesEnabled();
        toNeuron.getTargetSynapses(INPUT, templateEnabled)
                .filter(synB -> synA != synB)
                .filter(synB -> synA.getTemplate() != synB && synB.getTemplate() != synA)
                .filter(synB -> synA.isLatentLinking() || synB.isLatentLinking())
          //      .filter(synB -> synB.hasOutputTerminal(t.getOutput().getState()))
                .forEach(synB ->
                        latentLinking(t, fromBS, synA, synB)
                );
    }

    private static void latentLinking(PrimitiveTransition tA, BindingSignal bsA, Synapse synA, Synapse synB) {
        if(synB.hasOutputTerminal(tA.getOutput().getState())) {
            Stream<PrimitiveTransition> relTrans = synB.getRelatedTransitions(tA);
            relTrans.forEach(tB -> {
                Stream<BindingSignal> bsStream = synB.getRelatedBindingSignals(bsA.getOriginActivation(), tB, INPUT);

                latentLinking(tA, bsA, synA, synB, tB, bsStream);
            });
        }

        expandRelation(bsA, synA, synB, tA);
    }

    public static void latentLinking(PrimitiveTransition tA, BindingSignal bsA, Synapse synA, Synapse synB, PrimitiveTransition tB, Stream<BindingSignal> bsStream) {
        bsStream.filter(bsB -> bsA != bsB)
                .filter(bsB ->
                        isTrue(bsB.getOnArrivedFired())
                )
                .forEach(bsB -> {
                    latentLinking(synA, bsA, tA, bsB, tB);
                    latentLinking(synB, bsB, tB, bsA, tA);
                });
    }

    private static void latentLinking(
            Synapse targetSyn,
            BindingSignal fromBS,
            PrimitiveTransition matchingTransition,
            BindingSignal relBS,
            PrimitiveTransition propagateTransition
    ) {
        if(!targetSyn.isLatentLinking())
            return;

        if(!propagateTransition.isPropagate())
            return;

        if(!matchingTransition.isMatching())
            return;

        BindingSignal toBS = relBS.next(propagateTransition.getInput());

        if (!targetSyn.checkLinkingEvent(relBS.getActivation(), INPUT))
            return;

        Link l = targetSyn.propagate(fromBS);

        if(l != null) {
            toBS.init(l.getOutput());
            l.getOutput().addBindingSignal(toBS);
        }
    }
}
