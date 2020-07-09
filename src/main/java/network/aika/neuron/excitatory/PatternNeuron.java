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
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static network.aika.neuron.activation.Direction.OUTPUT;

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

    public String getTokenLabel() {
        return tokenLabel;
    }

    @Override
    public byte getType() {
        return type;
    }

    public double getCost(Sign s) {
        return Math.log(s.getP(this));
    }

    public double propagateRangeCoverage(Link l) {
        return l.getInput().getRangeCoverage();
    }

    public List<Neuron> induceNeuron(Activation act) {
        List<Neuron> results = new ArrayList<>();

        if(getStandardDeviation() > 0.08) {
            return results;
        }

        if(!act.getLinks(OUTPUT)
                .anyMatch(l -> l.getSynapse() instanceof InhibitorySynapse)) {
            results.add(new InhibitoryNeuron(getModel(), "PP-" + act.getDescriptionLabel(), false));
        }

        return results;
    }

    public Synapse induceSynapse(Activation iAct, Activation oAct) {
        return new ExcitatorySynapse(iAct.getNeuron(), (ExcitatoryNeuron) oAct.getNeuron());
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
