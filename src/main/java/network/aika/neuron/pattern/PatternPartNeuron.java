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
package network.aika.neuron.pattern;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron {
    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    public static byte type;

    public PatternPartNeuron(Neuron p) {
        super(p);
    }

    public PatternPartNeuron(Model model, String label) {
        super(model, label);
    }

    @Override
    public byte getType() {
        return type;
    }


    public Activation init(Activation iAct) {
        Document doc = iAct.getDocument();

        setBias(2.0);

        int actBegin = 0; // iAct.getSlot(BEGIN).getFinalPosition();
        lastCount += actBegin;

        PatternPartSynapse s = new PatternPartSynapse(iAct.getNeuron(), getProvider(), true, actBegin);

        s.updateDelta(doc, 2.0);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Activation targetAct = new Activation(doc, this, null, 0);

        Link l = new Link(s, iAct, targetAct);
        targetAct.addLink(l, false);

        return targetAct;
    }

    protected void createCandidateSynapse(Config c, Activation iAct, Activation targetAct) {
        Neuron targetNeuron = targetAct.getNeuron();
        Neuron inputNeuron = iAct.getNeuron();

        if(!((TNeuron) inputNeuron.get()).isMature(c)) {
            return;
        }

        int lastCount = 0; //iAct.getSlot(BEGIN).getFinalPosition();

        PatternPartSynapse s = new PatternPartSynapse(inputNeuron, targetNeuron, false, lastCount);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Link l = new Link(s, iAct, targetAct);

        targetAct.addLink(l, false);
    }
}
