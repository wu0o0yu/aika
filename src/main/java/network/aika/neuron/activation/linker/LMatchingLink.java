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
package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;


/**
 *
 * @author Lukas Molzberger
 */
public class LMatchingLink<S extends Synapse> extends LLink<S> {

    private boolean dir;

    public LMatchingLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, boolean dir) {
        super(input, output, patternScope, synapseClass, label);

        this.dir = dir;
    }

    public void follow(Activation act, LNode from, Activation startAct) {
        act.getLinks(getDirection(from))
                .forEach(l -> follow(l, from, startAct));
    }

    public void followBackwards(Link l) {
        Activation startAct = l.getOutput();
        startAct.setLNode(output);
        follow(l, output, startAct);
        startAct.setLNode(null);
    }

    public void follow(Link l, LNode from, Activation startAct) {
        if(!checkSynapse(l.getSynapse())) {
            return;
        }

        LNode to = getTo(from);
        Direction dir = getDirection(from);

        INeuron n = l.getSynapse().getNeuron(dir);
        Activation act = l.getActivation(dir);
        to.follow(n, act, this, startAct);
    }

    @Override
    public String getTypeStr() {
        return "M";
    }
}
