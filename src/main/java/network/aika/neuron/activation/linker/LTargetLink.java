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

import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

public class LTargetLink<S extends Synapse> extends LLink<S> {

    Boolean isRecurrent;
    Boolean isNegative;
    Boolean isPropagate;

    public LTargetLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, Boolean isRecurrent, Boolean isNegative, Boolean isPropagate) {
        super(input, output, patternScope, synapseClass, label);
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
        this.isPropagate = isPropagate;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Activation iAct = selectActivation(input, act, startAct);
        Activation oAct = selectActivation(output, act, startAct);
        LNode to = getTo(from);

        if(oAct != null) {
            followClosedLoop(m, iAct, oAct);
        } else if(to.isOpenEnd()) {
            followOpenEnd(m, iAct, to, startAct);
        }
    }

    private void followClosedLoop(Mode m, Activation iAct, Activation oAct) {
        Neuron in = iAct.getNeuron();
        if(linkExists(iAct, oAct)) {
            return;
        }

        Neuron on = oAct.getNeuron();
        Synapse s = in.getOutputSynapse(on, patternScope);

        if(s == null) {
            if(m != Mode.INDUCTION) return;
            s = createSynapse(in, on);
        }

        Link.link(s, iAct, oAct);
    }

    private void followOpenEnd(Mode m, Activation iAct, LNode to, Activation startAct) {
        Activation oAct;
        Neuron in = iAct.getNeuron();
        if(m == Mode.LINKING) {
            in.getActiveOutputSynapses().stream()
                    .filter(s -> checkSynapse(s))
                    .forEach(s -> {
                        Activation oa = to.follow(m, s.getOutput(), null, this, startAct);
                        Link.link(s, iAct, oa);
                    });
        } else if(m == Mode.INDUCTION) {
            if(!linkExists(iAct, to)) {
                oAct = to.follow(m, null, null, this, startAct);
                Synapse s = createSynapse(in, oAct.getNeuron());
                Link.link(s, iAct, oAct);
            }
        }
    }

    private boolean linkExists(Activation iAct, Activation oAct) {
        return iAct.outputLinks.get(oAct) != null;
    }

    private boolean linkExists(Activation iAct, LNode to) {
        return !iAct.outputLinks.values().stream()
                .filter(l -> checkSynapse(l.getSynapse()))
                .filter(l -> to.checkNeuron(l.getOutput().getINeuron()))
                .findAny()
                .isEmpty();
    }

    private Activation selectActivation(LNode n, Activation... acts) {
        for(Activation act : acts) {
            if(act.lNode == n) {
                return act;
            }
        }
        return null;
    }

    public Synapse createSynapse(Neuron in, Neuron on) {
        try {
            Synapse s = synapseClass.getConstructor().newInstance();
            s.init(patternScope, isRecurrent, isNegative, isPropagate);
            s.setInput(in);
            s.setOutput(on);

            s.link();
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean checkSynapse(Synapse s) {
        super.checkSynapse(s);

        if(isRecurrent != null && isRecurrent.booleanValue() != s.isRecurrent()) {
            return false;
        }

        if(isNegative != null && isNegative.booleanValue() != s.isNegative()) {
            return false;
        }

        if(isPropagate != null && isPropagate.booleanValue() != s.isPropagate()) {
            return false;
        }

        return true;
    }

    @Override
    public String getTypeStr() {
        return "T";
    }
}
