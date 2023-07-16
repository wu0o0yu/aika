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
package network.aika.visitor.operator;

import network.aika.Thought;
import network.aika.elements.synapses.Synapse;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.visitor.operator.Operator;

import java.util.stream.Stream;

import static network.aika.elements.synapses.Synapse.latentActivationExists;

/**
 * @author Lukas Molzberger
 */
public abstract class LinkingOperator implements Operator {

    protected Activation fromAct;

    protected Synapse syn;

    public LinkingOperator(Activation fromAct, Synapse syn) {
        this.fromAct = fromAct;
        this.syn = syn;
    }

    public void link(Activation bsA, Synapse synA, Link linkA, Synapse synB, Stream<Activation> bsStream) {
        bsStream
                .filter(bsB ->
                        synB.checkLinkingEvent(bsB)
                ).forEach(bsB ->
                        link(bsA, synA, linkA, bsB, synB)
                );
    }

    public Link link(Activation actA, Synapse synA, Link linkA, Activation actB, Synapse synB) {
        Activation oAct;
        if (linkA == null) {
            if(latentActivationExists(synA, synB, actA, actB))
                return null;

            Thought t = actA.getThought();
            oAct = synA.getOutput().createActivation(t);

            synA.createAndInitLink(actA, oAct);
        } else {
            oAct = linkA.getOutput();

            Link l = synB.checkExistingLink(actB, oAct);
            if(l != null)
                return l;
        }

        return synB.createAndInitLink(actB, oAct);
    }
}
