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
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Direction.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron {
    private static final Logger log = LoggerFactory.getLogger(PatternNeuron.class);

    public static byte type;

    private String tokenLabel;

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternNeuron(Model model, String tokenLabel, String descriptionLabel, Boolean isInputNeuron) {
        super(model, descriptionLabel, isInputNeuron);
        this.tokenLabel = tokenLabel;
    }

    @Override
    public Visitor transition(Visitor v) {
        if(!v.samePattern) {
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
        Reference or = nl.getOutput().getReference();
        Reference ir = nl.getInput().getReference();

        nl.getOutput().setReference(or == null ? ir : or.add(ir));
    }

    @Override
    public void induceNeuron(Activation act) {
        double s = getSurprisal(POS);

        if(s < 2.0) {
            System.out.println(act.getNeuron().getDescriptionLabel() + "  " + s + " below threshold");
            return;
        }

        if(hasOutputPatternPartConsumer(act)) {
            System.out.println(act.getNeuron().getDescriptionLabel() + "  " + s + " already exists");
            return;
        }

        Neuron n = new PatternPartNeuron(getModel(), "TP-" + getDescriptionLabel(), false);
        n.getInstances().update(getModel(), act.getReference());

        System.out.println(act.getNeuron().getDescriptionLabel() + "  " + s + "  --> " + n.getDescriptionLabel());

        Activation oAct = act.createActivation(n);

        n.induceSynapse(act, oAct, new Visitor(act, INPUT, false));
    }

    private boolean hasOutputPatternPartConsumer(Activation act) {
        return act.getOutputLinks()
                .filter(l -> l.getSynapse().isInputLinked())
                .anyMatch(l -> l.getOutput().getNeuron() instanceof PatternPartNeuron);
    }

    public String getTokenLabel() {
        return tokenLabel;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(tokenLabel != null);
        if(tokenLabel != null) {
            out.writeUTF(tokenLabel);
        }
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        if(in.readBoolean()) {
            tokenLabel = in.readUTF();
        }
    }
}
