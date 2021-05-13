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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

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
    public boolean allowTemplatePropagate(Activation act) {
        return true; //getCandidateGradient(act) >= 1.4;
    }

    @Override
    public PatternNeuron instantiateTemplate() {
        PatternNeuron n = new PatternNeuron(getModel());
        initFromTemplate(n);
        return n;
    }

    @Override
    public void transition(LinkVisitor v, Activation act) {
        if (v.getCurrentDir() == OUTPUT)
            return;

        ActVisitor nv = new ActVisitor(v, act);

        if(!nv.follow())
            return;

        nv.switchDirection();

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
