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
package network.aika.visitor.linking;

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.elements.synapses.Synapse;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.elements.synapses.Scope;

import java.util.stream.Stream;

import static network.aika.elements.synapses.Synapse.latentActivationExists;

/**
 * @author Lukas Molzberger
 */
public abstract class LinkingOperator {

    protected Activation fromBS;

    protected Synapse syn;

    public LinkingOperator(Activation fromBS, Synapse syn) {
        this.fromBS = fromBS;
        this.syn = syn;
    }

    public abstract Direction getRelationDir(Scope fromScope);

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

            Thought t = bsA.getThought();
            oAct = synA.getOutput().createAndInitActivation(t);

            synA.createAndInitLink(bsA, oAct);
        } else {
            oAct = linkA.getOutput();
            if(synB.linkExists(bsB, oAct))
                return null;
        }

        return synB.createAndInitLink(bsB, oAct);
    }
}
