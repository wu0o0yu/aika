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
import network.aika.neuron.activation.Link;
import network.aika.steps.activation.InactiveLinks;
import network.aika.steps.link.PropagateBindingSignal;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.Fields.isTrue;

/**
 * @author Lukas Molzberger
 */
public class TransitionListener<T extends Transition> {

    protected T transition;
    protected Direction dir;
    protected Synapse targetSynapse;
    protected BindingSignal bindingSignal;

    public TransitionListener(T transition, BindingSignal bs, Direction dir, Synapse targetSynapse) {
        this.transition = transition;
        this.bindingSignal = bs;
        this.dir = dir;
        this.targetSynapse = targetSynapse;
    }

    public void notify(BindingSignal bs) {
        FieldOutput e = targetSynapse.getLinkingEvent(bs, transition, dir);
        if (e != null)
            e.addLinkingEventListener(this);

        if(dir == OUTPUT)
            bs.getOnArrivedFiredFinal().addEventListener(() ->
                    InactiveLinks.add(bs)
            );
    }

    public void link() {
        link(bindingSignal);
    }

    protected void link(BindingSignal<?> fromBS) {
        fromBS.getRelatedBindingSignal(
                        targetSynapse,
                        dir.getNeuron(targetSynapse)
                )
                .filter(toBS -> fromBS != toBS)
                .forEach(toBS ->
                        link(fromBS, toBS)
                );

        if(dir == OUTPUT) {
            link(fromBS, null);
        }
    }

    public void link(BindingSignal fromBS, BindingSignal toBS) {
        if (toBS != null && !linkCheck(fromBS, toBS))
            return;

        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        Link nl = targetSynapse.link(
                inputBS.getActivation(),
                (outputBS != null ? outputBS.getActivation() : null)
        );

        if(nl != null)
            PropagateBindingSignal.add(nl, inputBS);
    }

    protected boolean linkCheck(BindingSignal fromBS, BindingSignal toBS) {
        if (transition.eventCheck(targetSynapse, toBS, dir.invert()))
            return true;

        if (isTrue(targetSynapse.getLinkingEvent(toBS, transition, dir)))
            return true;

        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        return transition.linkCheck(targetSynapse, inputBS, outputBS);
    }
}
