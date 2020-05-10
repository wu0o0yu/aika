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
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class LMatchingLink<S extends Synapse> extends LLink<S> {

    boolean dir;

    public LMatchingLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, boolean dir) {
        super(input, output, patternScope, synapseClass, label);

        this.dir = dir;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Stream<Link> s = null;
        if(from == input) {
            if(!act.isFinal && act.lastRound != null) {
                act = act.lastRound;
            }
            s = act.outputLinks.values().stream();
        } else if(from == output) {
            s = act.inputLinks.values().stream();
        }

        s.forEach(l -> follow(m, l, from, startAct));
    }

    public void followBackwards(Mode m, Link l) {
        Activation startAct = l.getOutput();
        startAct.lNode = output;
        follow(m, l, output, startAct);
        startAct.lNode = null;
    }

    public void follow(Mode m, Link l, LNode from, Activation startAct) {
        LNode to = getTo(from);
        if(!checkSynapse(l.getSynapse())) {
            return;
        }

        Activation act = getToActivation(l, to);
        INeuron n = getToNeuron(l.getSynapse(), to);
        to.follow(m, n, act, this, startAct);
    }

    @Override
    public String getTypeStr() {
        return "M";
    }
}
