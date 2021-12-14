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
import network.aika.Thought;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.PatternActivation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse, PatternActivation> {

    private String tokenLabel;

    public PatternNeuron() {
        super();
    }

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    @Override
    public PatternActivation createActivation(Thought t) {
        return new PatternActivation(t.createActivationId(), t, this);
    }

    @Override
    public boolean allowTemplatePropagate(Activation act) {
        return true; //getCandidateGradient(act) >= 1.4;
    }

    @Override
    public Class<? extends Activation> getBindingSignalType() {
        return BindingActivation.class;
    }

    @Override
    public PatternNeuron instantiateTemplate(boolean addProvider) {
        PatternNeuron n = new PatternNeuron(getModel(), addProvider);
        initFromTemplate(n);
        return n;
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
        if(tokenLabel != null)
            out.writeUTF(tokenLabel);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        if(in.readBoolean())
            tokenLabel = in.readUTF();
    }
}
