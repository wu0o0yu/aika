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

import java.util.Set;
import java.util.stream.Collectors;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

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
        TNeuron primaryInput = getPrimaryInput().getInput();
        TNeuron patternInput = getPatternInput().getInput();

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
        Linker.sameInputLinkT.input.follow(act, Linker.sameInputLinkT, act, c);
        Linker.relatedInputLinkT.input.follow(act, Linker.relatedInputLinkT, act, c);
        Linker.patternInputLinkT.input.follow(act, Linker.patternInputLinkT, act, c);
    }

    @Override
    public void collectLinkingCandidatesBackwards(Link l, Linker.CollectResults c) {
        Linker.sameInputLinkI.follow(l, Linker.sameInputLinkI.output, l.getOutput(), c);
        Linker.relatedInputLinkI.follow(l, Linker.relatedInputLinkI.output, l.getOutput(), c);
        Linker.inhibitoryLinkI.follow(l, Linker.inhibitoryLinkI.output, l.getOutput(), c);
    }

    @Override
    public void collectPosRecLinkingCandidates(Activation act, Linker.CollectResults c) {
        Linker.posRecLinkT.output.follow(act, Linker.posRecLinkT, act, c);
    }

    public double getCost(Sign s) {
        TNeuron primaryInput = getPrimaryInput();
        TNeuron patternInput = getPatternInput();

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

    private Synapse<TNeuron, TNeuron> getPatternSynapse(PatternScope ps) {
        return inputSynapses
                .values()
                .stream()
                .filter(s -> ps == s.getPatternScope())
                .filter(s -> s.getInput().getOuterType() == PatternNeuron.type)
                .findFirst()
                .orElse(null);
    }

    public TNeuron getPrimaryInput() {
        return getPatternSynapse(INPUT_PATTERN).getInput();
    }

    public TNeuron getPatternInput() {
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

    protected void createCandidateSynapse(Config c, Activation iAct, Activation targetAct) {
        Neuron targetNeuron = targetAct.getNeuron();
        Neuron inputNeuron = iAct.getNeuron();

        if (!((TNeuron) inputNeuron.get()).isMature(c)) {
            return;
        }

        PatternPartSynapse s = null; //new PatternPartSynapse(inputNeuron, targetNeuron);

        s.link();

        if (log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Link l = new Link(s, iAct, targetAct);

        targetAct.addLink(l);
    }


    public static void computeP(Activation act) {
        if(!act.isActive()) return;

        Set<Activation> conflictingActs = act.branches
                .stream()
                .flatMap(bAct -> bAct.inputLinks.values().stream())
                .filter(l -> l.isConflict())
                .flatMap(l -> l.getInput().inputLinks.values().stream())  // Hangle dich durch die inhib. Activation.
                .map(l -> l.getInput())
                .collect(Collectors.toSet());

        final double[] offset = new double[] {act.net};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> offset[0] = Math.min(offset[0], cAct.net)
                );

        final double[] norm = new double[] {Math.exp(act.net - offset[0])};
        conflictingActs
                .stream()
                .forEach(
                        cAct -> norm[0] += Math.exp(cAct.net - offset[0])
                );

        act.p = Math.exp(act.net - offset[0]) / norm[0];
    }
}
