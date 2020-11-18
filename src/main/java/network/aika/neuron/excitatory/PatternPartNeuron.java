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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.Direction.*;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternPartNeuron(Model model) {
        super(model);
    }

    @Override
    public void prepareInitialSynapseInduction(Activation iAct, Activation newAct) {
        newAct.getNeuron().induceSynapse(iAct, newAct, new Visitor(iAct, newAct, INPUT, null, INPUT, false));
    }

    @Override
    public void initOutgoingPPSynapse(PatternPartSynapse s, Visitor v) {
        s.setInputScope(v.scope == v.downUpDir);
        s.setSamePattern(v.related);
    }

    @Override
    public InhibitorySynapse induceOutgoingInhibitorySynapse(InhibitoryNeuron outN) {
        return new InhibitorySynapse(this, outN);
    }

    @Override
    public void induceNeuron(Activation act) {
        PatternNeuron.induce(act);
    }

    public static Activation induce(Activation iAct) {
        if(!iAct.getConfig().checkPatternPartNeuronInduction(iAct.getNeuron())) {
            return null;
        }

        Activation act = iAct.getOutputLinks()
                .filter(l -> l.getSynapse().inductionRequired(PatternPartNeuron.class))
                .map(l -> l.getOutput())
                .findAny()
                .orElse(null);

        if (act == null) {
            Neuron n = new PatternPartNeuron(iAct.getModel());
            n.initInstance(iAct.getReference(), iAct);
            act = n.initInducedNeuron(iAct);
        }

        return act;
    }

    public Link induceSynapse(Activation iAct, Activation oAct, Visitor v) {
        PatternPartSynapse s = new PatternPartSynapse(iAct.getNeuron(), this);
        iAct.getNeuron().initOutgoingPPSynapse(s, v);
        s.initInstance(iAct.getReference(), iAct);

        return s.initInducedSynapse(iAct, oAct, v);
    }

    @Override
    public void transition(Visitor v, Activation act) {
        if(v.samePattern) {
            if(v.downUpDir == OUTPUT) {
                return;
            }

            Visitor nv = v.prepareNextStep();
            nv.downUpDir = OUTPUT;
            nv.followLinks(act);
        } else {
            v.followLinks(act);
        }
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public void updateReference(Link nl) {
        nl.getOutput().propagateReference(nl.getInput().getReference());
    }
}
