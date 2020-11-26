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
import java.util.Arrays;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternNeuron.class);

    public static byte type;

    public static PatternNeuron THIS_TEMPLATE = new PatternNeuron();

    public static PatternSynapse PATTERN_SYNAPSE_TEMPLATE = new PatternSynapse(PatternPartNeuron.THIS_TEMPLATE, THIS_TEMPLATE);


    private String tokenLabel;

    private PatternNeuron() {
        super();
    }

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternNeuron(Model model, String tokenLabel) {
        super(model);
        this.tokenLabel = tokenLabel;
    }

    @Override
    public Neuron<?> getTemplate() {
        return THIS_TEMPLATE;
    }

    @Override
    public Stream<PatternSynapse> getTemplateSynapses() {
        return Arrays.asList(PATTERN_SYNAPSE_TEMPLATE).stream();
    }

    @Override
    public void transition(Visitor v, Activation act, boolean create) {
        if(!v.samePattern) {
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
        Reference or = nl.getOutput().getReference();
        Reference ir = nl.getInput().getReference();

        nl.getOutput().propagateReference(or == null ? ir : or.add(ir));
    }

    public Link induceSynapse(Activation iAct, Activation oAct, Visitor v) {
        PatternSynapse s = new PatternSynapse(iAct.getNeuron(), this);
        return s.initInducedSynapse(iAct, oAct, v);
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
