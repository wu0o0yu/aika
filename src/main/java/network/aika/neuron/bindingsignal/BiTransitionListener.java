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
import network.aika.neuron.activation.Activation;

import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public class BiTransitionListener extends TransitionListener<BiTransition> {
    private BindingSignal relatedBindingSignal;

    public BiTransitionListener(BiTransition transition, BindingSignal bs, Direction dir, Synapse targetSynapse) {
        super(transition, null, dir, targetSynapse);

        relatedBindingSignal = bs;
    }

    public void notify(BindingSignal bs) {
        this.bindingSignal = bs;

        FieldOutput eA = targetSynapse.getLinkingEvent(relatedBindingSignal, dir);
        FieldOutput eB = targetSynapse.getLinkingEvent(bindingSignal, dir);

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
        if(relatedBindingSignal == null)
            return;

        link(transition.getRelatedTransition(), relatedBindingSignal);
        link(transition, bindingSignal);
    }

    public void link(Transition t, BindingSignal fromBS, BindingSignal toBS) {
        Activation toAct = toBS.getActivation();
        if(!(fromBS == relatedBindingSignal ?
                checkRelated(transition, bindingSignal, toAct) :
                checkRelated(transition.getRelatedTransition(), relatedBindingSignal, toAct)
        )) return;

        super.link(t, fromBS, toBS);
    }

    private boolean checkRelated(Transition relTransition, BindingSignal relFromBS, Activation toAct) {
        BindingSignal relToBS = toAct.getBindingSignal(relTransition.getOutput());

        return relToBS == null || relFromBS.getOriginActivation() == relToBS.getOriginActivation();
    }
}
