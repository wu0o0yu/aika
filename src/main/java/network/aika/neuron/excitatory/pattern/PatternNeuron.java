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

import network.aika.Config;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.linker.*;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse> {

    public static byte type;

    public PatternNeuron(Neuron p) {
        super(p);
    }

    public PatternNeuron(Model model, String label) {
        super(model, label);
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

    public boolean isMature(Config c) {
        return binaryFrequency >= c.getMaturityThreshold();  // Sign.NEG, Sign.POS
    }

    @Override
    protected void createCandidateSynapse(Config c, Activation iAct, Activation targetAct) {

    }

    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        super.commit(modifiedSynapses);

        TreeSet<PatternSynapse> sortedSynapses = new TreeSet<>(
                Comparator.<Synapse>comparingDouble(s -> s.getWeight()).reversed()
                        .thenComparing(Direction.INPUT.getSynapseComparator())
        );

        sortedSynapses.addAll(inputSynapses.values());

        double sum = getBias(CURRENT);
        for(PatternSynapse s: sortedSynapses) {
            if(!s.isRecurrent() && !s.isNegative()) {
                s.setPropagate(sum > 0.0);

                sum -= s.getWeight();
            }
        }
    }

    @Override
    public void collectLinkingCandidatesForwards(Activation act, Linker.CollectResults c) {
    }

    @Override
    public void collectLinkingCandidatesBackwards(Link l, Linker.CollectResults c) {
        Linker.patternInputLinkI.follow(l, Linker.patternInputLinkI.output, l.getOutput(), c);
    }

    @Override
    public void collectPosRecLinkingCandidates(Activation act, Linker.CollectResults c) {

    }
}
