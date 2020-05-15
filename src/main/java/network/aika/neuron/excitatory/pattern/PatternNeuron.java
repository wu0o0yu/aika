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
package network.aika.neuron.excitatory.pattern;

import network.aika.Model;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.linker.*;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static network.aika.neuron.activation.Direction.OUTPUT;
import static network.aika.neuron.activation.linker.LinkGraphs.inducePPInhibitoryNeuron;
import static network.aika.neuron.activation.linker.LinkGraphs.inducePatternPart;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternNeuron.class);

    public static byte type;

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public byte getOuterType() {
        return type;
    }

    public double getCost(Sign s) {
        return Math.log(s.getP(this));
    }

    public double propagateRangeCoverage(Activation iAct) {
        return iAct.rangeCoverage;
    }

    public boolean isMature() {
        return binaryFrequency >= getModel().getTrainingConfig().getMaturityThreshold();
    }

    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        super.commit(modifiedSynapses);
    }

    @Override
    public void linkForwards(Activation act) {
        super.linkForwards(act);
    }

    @Override
    public void linkBackwards(Link l) {
        LinkGraphs.patternInputLinkI.followBackwards(l);
    }

    @Override
    public void linkPosRecSynapses(Activation act) {
    }

    public void induceStructure(Activation act) {
        inducePatternPart.follow(act, OUTPUT, false);
        inducePPInhibitoryNeuron.follow(act, OUTPUT, false);
    }
}
