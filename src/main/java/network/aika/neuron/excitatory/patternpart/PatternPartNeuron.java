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
import network.aika.Document;
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
import static network.aika.neuron.activation.linker.Mode.SYNAPSE_INDUCTION;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron(Neuron p) {
        super(p);
    }

    public PatternPartNeuron(Model model, String label) {
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


    public boolean isMature(Config c) {
        return binaryFrequency >= c.getMaturityThreshold();  // Sign.NEG, Sign.POS
    }

    /*
    public double getInformationGain(Sign si, Sign so) {
        INeuron primaryInput = getPrimaryInput().getInput();
        INeuron patternInput = getPatternInput().getInput();

        double fz = primaryInput.frequency;
        double Nz = primaryInput.N;
        double fy = patternInput.frequency;
        double Ny = patternInput.N;

        double pXi = fz / Nz;
        pXi = si == Sign.POS ? pXi : 1.0 - pXi;

        double pXo = fy / Ny;
        pXo = so == Sign.POS ? pXo : 1.0 - pXo;

        double f;
        if(si == Sign.POS) {
            if(so == Sign.POS) {
                f = frequency;
            } else {
                f = fz - frequency;
            }
        } else {
            if(so == Sign.POS) {
                f = fy - frequency;
            } else {
                f = N + frequency - (fz + fy);
            }
        }
        double pXio = f / N;

        return Math.log(pXio) - (Math.log(pXi) + Math.log(pXo));
    }
    */

    public double propagateRangeCoverage(Activation iAct) {
        return getPrimaryInput() == iAct.getINeuron() ? iAct.rangeCoverage : 0.0;
    }

    @Override
    public void collectLinkingCandidatesForwards(Activation act, Linker.CollectResults c) {
        Linker.sameInputLinkT.input.follow(LINKING, act, Linker.sameInputLinkT, act, c);
        Linker.relatedInputLinkT.input.follow(LINKING, act, Linker.relatedInputLinkT, act, c);
        Linker.patternInputLinkT.input.follow(LINKING, act, Linker.patternInputLinkT, act, c);
    }

    @Override
    public void collectLinkingCandidatesBackwards(Link l, Linker.CollectResults c) {
        Linker.sameInputLinkI.follow(LINKING, l, Linker.sameInputLinkI.output, l.getOutput(), c);
        Linker.relatedInputLinkI.follow(LINKING, l, Linker.relatedInputLinkI.output, l.getOutput(), c);
        Linker.inhibitoryLinkI.follow(LINKING, l, Linker.inhibitoryLinkI.output, l.getOutput(), c);
    }

    @Override
    public void collectPosRecLinkingCandidates(Activation act, Linker.CollectResults c) {
        Linker.posRecLinkT.output.follow(LINKING, act, Linker.posRecLinkT, act, c);
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

    public Activation init(Activation iAct) {
        Document doc = iAct.getDocument();

        setBias(2.0);

        int actBegin = 0; // iAct.getSlot(BEGIN).getFinalPosition();

        PatternPartSynapse s = null; //new PatternPartSynapse(iAct.getNeuron(), getProvider());

        s.updateDelta(doc, 2.0);

        s.link();

        if (log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Activation targetAct = new Activation(doc.getNewActivationId(), doc, this);
        Link l = new Link(s, iAct, targetAct);
        targetAct.addLink(l);

        return targetAct;
    }

    public void collectNewSynapseCandidates(Activation act, Linker.CollectResults c) {
        Linker.sameInputLinkT.output.follow(SYNAPSE_INDUCTION, act, Linker.sameInputLinkT, act, c);
        Linker.relatedInputLinkT.output.follow(SYNAPSE_INDUCTION, act, Linker.relatedInputLinkT, act, c);
    }

    @Override
    public Synapse createSynapse(Neuron input, PatternScope patternScope, Boolean isRecurrent, Boolean isNegative) {
        return new PatternPartSynapse(input, getProvider(), patternScope, isRecurrent, isNegative);
    }

    @Override
    public void createSynapses(Config c, Activation act) {

    }
}
