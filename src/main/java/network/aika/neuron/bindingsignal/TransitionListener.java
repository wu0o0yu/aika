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
import network.aika.steps.activation.InactiveLinks;

import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;

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
        FieldOutput e = targetSynapse.getLinkingEvent(bs, dir);
        if (e != null)
            e.addLinkingEventListener(this);

        if(dir == OUTPUT)
            bs.getEvent(true, true)
                    .addEventListener(() ->
                            InactiveLinks.add(bs)
                    );
    }

    public void link() {
        link(transition, bindingSignal);
        propagate(transition, bindingSignal);
    }

    protected void link(T t, BindingSignal<?> fromBS) {
        Stream<BindingSignal<?>> bsStream = targetSynapse.getRelatedBindingSignal(fromBS, dir);

        bsStream.filter(toBS -> fromBS != toBS)
                .forEach(toBS ->
                        link(t, fromBS, toBS)
                );
    }

    protected void propagate(T t, BindingSignal<?> fromBS) {
        if (dir == INPUT)
            return;

        targetSynapse.propagate(
                fromBS.getActivation()
        );
    }

    public void link(T t, BindingSignal fromBS, BindingSignal toBS) {
        if(!t.linkCheck(targetSynapse, fromBS, toBS, dir))
            return;

        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        if(!targetSynapse.linkCheck(inputBS, outputBS))
            return;

        targetSynapse.link(
                inputBS.getActivation(),
                outputBS.getActivation()
        );
    }

    public boolean check(BindingSignal bs) {
        return transition.check(bs, dir);
    }
}
