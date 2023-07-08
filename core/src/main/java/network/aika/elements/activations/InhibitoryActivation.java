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
package network.aika.elements.activations;

import network.aika.Thought;
import network.aika.elements.links.AbstractInhibitoryLink;
import network.aika.elements.links.InhibitoryCategoryLink;
import network.aika.elements.links.InhibitoryLink;
import network.aika.elements.links.NegativeFeedbackLink;
import network.aika.elements.neurons.InhibitoryNeuron;

import java.util.List;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryActivation extends DisjunctiveActivation<InhibitoryNeuron> {


    public InhibitoryActivation(int id, Thought t, InhibitoryNeuron neuron) {
        super(id, t, neuron);
    }


    @Override
    public boolean isActiveTemplateInstance() {
        return true;
    }

    public Stream<InhibitoryLink> getInhibitoryLinks() {
        return getInputLinksByType(AbstractInhibitoryLink.class)
                .flatMap(AbstractInhibitoryLink::getInhibitoryLinks);
    }

    public Stream<NegativeFeedbackLink> getNegativeFeedbackLinks() {
        return Stream.concat(
                getOutputLinksByType(NegativeFeedbackLink.class),
                getOutputLinksByType(InhibitoryCategoryLink.class)
                        .flatMap(InhibitoryCategoryLink::getNegativeFeedbackLinks)
        );
    }

    public static void connectFields(Stream<InhibitoryLink> in, Stream<NegativeFeedbackLink> out) {
        List<NegativeFeedbackLink> nfls = out.toList();

        in.forEach(il ->
                nfls.forEach(nfl ->
                        il.connectFields(nfl)
                )
        );
    }
}
