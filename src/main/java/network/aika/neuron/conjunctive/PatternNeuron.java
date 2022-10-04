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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.Thought;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.bindingsignal.PrimitiveTerminal;
import network.aika.neuron.bindingsignal.State;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.FixedTerminal.fixed;
import static network.aika.neuron.bindingsignal.State.ABSTRACT_SAME;
import static network.aika.neuron.bindingsignal.VariableTerminal.variable;
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.PATTERN;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ConjunctiveNeuron<ConjunctiveSynapse, PatternActivation> {

    public static PrimitiveTerminal INPUT_IN = variable(State.INPUT, INPUT, PatternNeuron.class);
    public static PrimitiveTerminal ABSTRACT_SAME_IN = fixed(ABSTRACT_SAME, INPUT, PatternNeuron.class);

    public static PrimitiveTerminal INPUT_OUT = variable(State.INPUT, OUTPUT, PatternNeuron.class);
    public static PrimitiveTerminal ABSTRACT_SAME_OUT = fixed(ABSTRACT_SAME, OUTPUT, PatternNeuron.class);



    private String tokenLabel;

    public PatternNeuron() {
        super(PATTERN);
    }

    @Override
    public PatternActivation createActivation(Thought t) {
        return new PatternActivation(t.createActivationId(), t, this);
    }

    @Override
    public PatternNeuron instantiateTemplate(boolean addProvider) {
        PatternNeuron n = new PatternNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    protected void updateSumOfLowerWeights() {
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
