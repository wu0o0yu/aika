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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Direction.*;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron() {
        super();
    }

    public PatternPartNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternPartNeuron(Model model) {
        super(model);
    }


    @Override
    public boolean checkTemplate(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isInputNeuron()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean checkInduction(Activation act) {
        return true;
    }

    @Override
    public PatternPartNeuron instantiateTemplate() {
        PatternPartNeuron n = new PatternPartNeuron(getModel());
        n.getTemplates().add(this);
        n.getTemplates().addAll(getTemplates());
        return n;
    }

    @Override
    public void transition(Visitor v, Activation act, boolean create) {
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
        // TODO: find a better solution.
        Synapse ts = nl.getSynapse().getTemplate();
        Templates t = getModel().getTemplates();
        if(ts != t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE && ts != t.RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE) {
            nl.getOutput().propagateReference(nl.getInput().getReference());
        }
    }
}
