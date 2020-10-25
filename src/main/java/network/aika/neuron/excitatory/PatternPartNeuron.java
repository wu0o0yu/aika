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

import static network.aika.neuron.activation.Direction.*;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternPartNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    @Override
    public void induceNeuron(Activation act) {
        PatternNeuron.induce(act);
    }

    public static void induce(Activation iAct) {
        if(iAct.checkInductionThreshold()) {
//            System.out.println("N  " + "dbg:" + (Neuron.debugId++) + " " + act.getNeuron().getDescriptionLabel() + "  " + Utils.round(s) + " below threshold");
            return;
        }

        if (!iAct.checkIfOutputLinkExists(syn -> syn.isInputScope() && syn.isInputLinked())) {
            Neuron n = new PatternPartNeuron(iAct.getModel(), "TP-" + iAct.getDescriptionLabel(), false);
            n.initInstance(iAct.getReference());
            n.initInducedNeuron(iAct);
        }
    }

    @Override
    public Visitor transition(Visitor v) {
        if(v.samePattern) {
            if(v.downUpDir == OUTPUT) {
                return null;
            }

            Visitor nv = v.copy();
            nv.downUpDir = OUTPUT;
            return nv;
        }
        return v;
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public void updateReference(Link nl) {
        if(nl.getInput().getNeuron() instanceof PatternNeuron) {
            nl.getOutput().setReference(nl.getInput().getReference());
        }
    }
}
