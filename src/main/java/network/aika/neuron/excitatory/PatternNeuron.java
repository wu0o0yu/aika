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
import network.aika.neuron.activation.direction.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.Scope.*;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse> {

    private static final Logger log = LoggerFactory.getLogger(PatternNeuron.class);

    public static byte type;

    private String tokenLabel;

    public PatternNeuron() {
        super();
    }

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternNeuron(Model model) {
        super(model);
    }

    @Override
    public Set<ScopeEntry> getInitialScopes(Direction dir) {
        Set<ScopeEntry> result = new TreeSet<>();
        result.add(new ScopeEntry(0, P_SAME));
        if(dir == Direction.OUTPUT) {
            result.add(new ScopeEntry(1, PP_SAME));
            result.add(new ScopeEntry(2, PP_INPUT));
            result.add(new ScopeEntry(3, I_INPUT));
        }
        return result;
    }

    @Override
    public boolean checkGradientThreshold(Activation act) {
        return getCandidateGradient(act) >= 1.4;
    }

    @Override
    public PatternNeuron instantiateTemplate() {
        PatternNeuron n = new PatternNeuron(getModel());
        n.getTemplates().add(this);
        n.getTemplates().addAll(getTemplates());
        return n;
    }

    @Override
    public void transition(Visitor v, Activation act) {
        if (v.downUpDir == OUTPUT)
            return;

        Visitor nv = v.prepareNextStep(act, null, v.getScopes(), ACT);

        if(nv == null)
            return;

        nv.downUpDir = OUTPUT;

        act.followLinks(nv);
    }

    @Override
    public byte getType() {
        return type;
    }

    public void setTokenLabel(String tokenLabel) {
        this.tokenLabel = tokenLabel;
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
