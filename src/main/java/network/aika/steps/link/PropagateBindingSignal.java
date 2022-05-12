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
package network.aika.steps.link;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.Transition;
import network.aika.steps.Phase;
import network.aika.steps.Step;


/**
 *
 * @author Lukas Molzberger
 */
public class PropagateBindingSignal extends Step<Link> {

    private BindingSignal inputBS;
    private Transition transition;

    public static void add(Link l, BindingSignal bs) {
        if(!bs.isPropagateAllowed())
            return;

        Transition t = bs.transition(l.getSynapse());
        if(t == null)
            return;

        Step.add(new PropagateBindingSignal(l, bs, t));
    }

    private PropagateBindingSignal(Link l, BindingSignal bs, Transition t) {
        super(l);
        inputBS = bs;
        transition = t;
    }

    @Override
    public void process() {
        Link l = getElement();
        BindingSignal toBS = inputBS.next(transition);
        toBS.setLink(l);

        Activation oAct = l.getOutput();
        toBS.init(oAct);
        oAct.addBindingSignal(toBS);
    }

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    public String toString() {
        return "" + getElement() + " inputBS:" + inputBS + " transition:<" + transition + ">";
    }
}
