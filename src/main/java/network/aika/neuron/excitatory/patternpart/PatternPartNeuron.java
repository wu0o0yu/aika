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
package network.aika.neuron.excitatory.patternpart;

import network.aika.Config;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.linker.*;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;
import static network.aika.neuron.activation.linker.Mode.LINKING;
import static network.aika.neuron.activation.linker.Mode.INDUCTION;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron(Neuron p) {
        super(p);
    }

    public PatternPartNeuron(Model model, String label, boolean isInputNeuron) {
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

    public boolean isMature() {
        return binaryFrequency >= getModel().getTrainingConfig().getMaturityThreshold();  // Sign.NEG, Sign.POS
    }

    public double propagateRangeCoverage(Activation iAct) {
        return getPrimaryInput() == iAct.getINeuron() ? iAct.rangeCoverage : 0.0;
    }

    @Override
    public void linkForwards(Activation act) {
        super.linkForwards(act);

        LinkGraphs.sameInputLinkT.followForwards(LINKING, act);
        LinkGraphs.relatedInputLinkT.followForwards(LINKING, act);
        LinkGraphs.patternInputLinkT.followForwards(LINKING, act);
    }

    @Override
    public void linkBackwards(Link l) {
        LinkGraphs.sameInputLinkI.followBackwards(LINKING, l);
        LinkGraphs.relatedInputLinkI.followBackwards(LINKING, l);
        LinkGraphs.inhibitoryLinkI.followBackwards(LINKING, l);
    }

    @Override
    public void linkPosRecSynapses(Activation act) {
        LinkGraphs.posRecLinkT.output.follow(LINKING, act.getINeuron(), act, LinkGraphs.posRecLinkT, act);
    }

    @Override
    public void induceStructure(Activation act) {

    }

    public double getCost(Sign s) {
        INeuron primaryInput = getPrimaryInput();
        INeuron patternInput = getPatternInput();

        double fz = primaryInput.frequency;
        double Nz = primaryInput.getN();
        double fy = patternInput.frequency;
        double Ny = patternInput.getN();

        double pXi = fz / Nz;
        double pXo = fy / Ny;

        double pXio = frequency / getN();
        double pXioIndep = pXi * pXo;

        if (s == Sign.POS) {
            return Math.log(pXio) - Math.log(pXioIndep);
        } else {
            return Math.log(1.0 - pXio) - Math.log(1.0 - pXioIndep);
        }
    }

    private Synapse<INeuron, INeuron> getPatternSynapse(PatternScope ps) {
        return inputSynapses
                .values()
                .stream()
                .filter(s -> ps == s.getPatternScope())
                .filter(s -> s.getInput().getOuterType() == PatternNeuron.type)
                .findFirst()
                .orElse(null);
    }

    public INeuron getPrimaryInput() {
        return getPatternSynapse(INPUT_PATTERN).getInput();
    }

    public INeuron getPatternInput() {
        return getPatternSynapse(SAME_PATTERN).getInput();
    }

    public void collectNewSynapseCandidates(Activation act) {
        LinkGraphs.sameInputLinkT.output.follow(INDUCTION, act.getINeuron(), act, LinkGraphs.sameInputLinkT, act);
        LinkGraphs.relatedInputLinkT.output.follow(INDUCTION, act.getINeuron(), act, LinkGraphs.relatedInputLinkT, act);
    }
}
