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
package network.aika.neuron.visitor.linking;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.conjunctive.ConjunctiveSynapse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public abstract class LinkingOperator implements Consumer<Link> {

    protected Activation fromBS;

    protected ConjunctiveSynapse syn;

    private List<Link> newLinks = new ArrayList<>();

    public LinkingOperator(Activation fromBS, ConjunctiveSynapse syn) {
        this.fromBS = fromBS;
        this.syn = syn;
    }

    @Override
    public void accept(Link l) {
        if(l != null)
            newLinks.add(l);
    }

    public void finalizeLinks() {
        newLinks.forEach(l ->
                l.link()
        );
    }

    public abstract void check(LinkingCallback v, Link lastLink, Activation act);

    public void link(Activation bsA, Synapse synA, Link linkA, Synapse synB, Stream<Activation> bsStream) {
        bsStream
                .filter(bsB ->
                        synB.checkLinkingEvent(bsB)
                ).forEach(bsB ->
                        link(bsA, synA, linkA, bsB, synB)
                );
    }

    public Link link(Activation bsA, Synapse synA, Link linkA, Activation bsB, Synapse synB) {
        Activation oAct;
        if (linkA == null) {
            if(latentActivationExists(synA, synB, bsA, bsB))
                return null;

            oAct = synA.getOutput().createActivation(bsA.getThought());
            oAct.init(synA, bsA);

            synA.createAndCollectLink(bsA, oAct, this);
        } else {
            oAct = linkA.getOutput();
            if(synB.linkExists(bsB, oAct))
                return null;
        }

        return synB.createAndCollectLink(bsB, oAct, this);
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
