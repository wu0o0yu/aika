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

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Lukas Molzberger
 */
public class Linker {

    public static void link(Activation bsA, Synapse synA, Link linkA, Synapse synB, Stream<Activation> bsStream) {
        bsStream
                .filter(bsB ->
                        synB.checkLinkingEvent(bsB)
                ).forEach(bsB ->
                        link(bsA, synA, linkA, bsB, synB)
                );
    }

    public static Link link(Activation bsA, Synapse synA, Link linkA, Activation bsB, Synapse synB) {
        Activation oAct;
        if (linkA == null) {
            if(latentActivationExists(synA, synB, bsA, bsB))
                return null;

            oAct = synA.getOutput().createActivation(bsA.getThought());
            oAct.init(synA, bsA);

            synA.createLink(bsA, oAct);
        } else {
            oAct = linkA.getOutput();
            if(synB.linkExists(bsB, oAct))
                return null;
        }

        return synB.createLink(bsB, oAct);
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
