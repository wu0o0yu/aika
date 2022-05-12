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
import network.aika.fields.FieldOutput;
import network.aika.neuron.Synapse;

import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public class BiTransitionListener extends TransitionListener<BiTransition> {
    private BindingSignal relatedBindingSignal;

    public BiTransitionListener(BiTransition transition, BindingSignal bs, Direction dir, Synapse targetSynapse) {
        super(transition, bs, dir, targetSynapse);
    }

    public void notify(BindingSignal bs) {
        this.relatedBindingSignal = bs;

        FieldOutput eA = targetSynapse.getLinkingEvent(relatedBindingSignal, transition.getRelatedTransition(), dir);
        FieldOutput eB = targetSynapse.getLinkingEvent(bindingSignal, transition, dir);

        if(eA != null && eB != null) {
            FieldOutput e = mul(
                    "bi transition linking event",
                    eA,
                    eB
            );

            e.addLinkingEventListener(this);
        }
    }

    public void link() {
        link(relatedBindingSignal);
        link(bindingSignal);
    }

    public void link(BindingSignal fromBS, BindingSignal toBS) {
        if(fromBS == relatedBindingSignal) {
            super.link(fromBS, toBS);
        } else {
            super.link(fromBS, toBS);
        }
    }
}
