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
package network.aika.neuron;

import network.aika.*;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.Scope;
import network.aika.neuron.phase.link.PropagateGradient;
import network.aika.neuron.sign.Sign;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Element.RoundType.*;
import static network.aika.neuron.sign.Sign.NEG;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.activation.Link.linkExists;
import static network.aika.neuron.activation.Visitor.Transition.LINK;
import static network.aika.neuron.phase.link.LinkPhase.INFORMATION_GAIN_GRADIENT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends Neuron<?>, O extends Neuron<?>> implements Writable {

    private static final Logger log = LoggerFactory.getLogger(Synapse.class);

    public static double TOLERANCE = 0.0000001;

    protected NeuronProvider input;
    protected NeuronProvider output;

    private Synapse template;

    private double weight;

    protected SampleSpace sampleSpace;

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;
    private volatile boolean modified;

    protected boolean allowTraining = true;


    public Synapse() {
    }

    public Synapse(I input, O output, Synapse template) {
        this.input = input.getProvider();
        this.output = output.getProvider();
        this.template = template;

        sampleSpace = new SampleSpace(getModel());

        assert input.getId() < 0 || input.getId() != output.getId();
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public static boolean synapseExists(Neuron iN, Neuron oN) {
        return oN.getInputSynapse(iN.getProvider()) != null;
    }

    public boolean isTemplate() {
        return template == null;
    }

    public Synapse getTemplate() {
        return template;
    }

    public abstract Synapse instantiateTemplate(I input, O output);

    public abstract Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement);

    protected abstract boolean checkCausality(Activation iAct, Activation oAct, Visitor v);

    public abstract boolean checkTemplatePropagate(Visitor v, Activation act);

    public abstract byte getType();

    public abstract void updateReference(Link l);

    public Reference getReference(Link l) {
        return l.getInput().getReference();
    }

    public boolean isInputLinked() {
        return getInput().containsOutputSynapse(this);
    }

    public boolean isRecurrent() {
        return false;
    }

    public abstract Activation branchIfNecessary(Activation oAct, Visitor v);

    public Visitor transition(Visitor v, Link l) {
        Visitor nv = v.prepareNextStep(
                null,
                l,
                v.getScopes()
                        .stream()
                        .flatMap(s ->
                                transition(s, v.downUpDir, v.startDir, l == null)
                                        .stream()
                        ).collect(Collectors.toCollection(TreeSet::new)),
                LINK
        );

        if(nv != null)
            nv.incrementPathLength();

        return nv;
    }

    public void follow(Activation toAct, Visitor v) {
        v.onEvent(false);

        toAct.getNeuron()
                .transition(v, toAct);

        v.onEvent(true);
    }

    public void propagate(Activation fromAct, Visitor v) {
        Visitor nv = transition(v, null);
        if(nv == null)
            return;

        Thought t = fromAct.getThought();
        Direction dir = nv.startDir;

        Activation toAct = t
                .createActivation(
                        nv.startDir.getNeuron(this),
                        fromAct
                );

        int round = fromAct.getRound(ACT);

        t.addToQueue(
                toAct,
                round,
                nv.getPhase().getNextActivationPhases()
        );

        createLink(
                dir.getPropagateInput(fromAct, toAct),
                dir.getPropagateOutput(fromAct, toAct),
                nv,
                round
        );
    }

    public void closeCycle(Visitor v, Activation iAct, Activation oAct) {
        Visitor nv = transition(v, null);
        if(nv == null)
            return;

        if(!nv.isClosedCycle())
            return;

        if (linkExists(this, iAct, oAct))
            return;

        if (!checkCausality(iAct, oAct, v))
            return;

        int round = Math.max(
                iAct.getRound(ACT),
                oAct.getRound(ACT)
        );

        createLink(iAct, oAct, nv, round);
    }

    public void createLink(Activation iAct, Activation oAct, Visitor v, int round) {
        Thought t = oAct.getThought();

        Link nl = oAct.addLink(
                this,
                iAct,
                v.getSelfRef()
        );

        t.onLinkCreationEvent(nl);

        v.link = nl;

        Synapse s = nl.getSynapse();
        if (s.getWeight() <= 0.0 && !s.isTemplate())
            return;

        t.addToQueue(
                nl,
                round,
                v.getPhase().getNextLinkPhases()
        );

        t.addToQueue(
                nl,
                round,
                INFORMATION_GAIN_GRADIENT,
                !oAct.gradientSumIsZero() ? new PropagateGradient(oAct.getOutputGradientSum()) : null
        );
    }

    public void linkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        in.addOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public void unlinkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        in.removeOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public void updateInputLink(boolean link) {
        if(link) {
            linkInput();
        } else {
            unlinkInput();
        }
    }

    public boolean isOutputLinked() {
        return getOutput().containsInputSynapse(this);
    }

    public void linkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        out.addInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public void unlinkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        out.removeInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public NeuronProvider getPInput() {
        return input;
    }

    public NeuronProvider getPOutput() {
        return output;
    }

    public I getInput() {
        return (I) input.getNeuron();
    }

    public O getOutput() {
        return (O) output.getNeuron();
    }

    public SampleSpace getSampleSpace() {
        return sampleSpace;
    }

    public double updateSynapse(Link l) {
        double gradient = l.getAndResetGradient();

        Neuron on = getOutput();
        double oldWeight = weight;

        double learnRate = on.getConfig().getLearnRate();

        if(l.getInput().isActive(true)) {
            double wDelta = learnRate * gradient;
            addWeight(wDelta);
        } else {
            double wDelta = learnRate * gradient;

            addWeight(-wDelta);
            on.addConjunctiveBias(wDelta, !l.isCausal());
        }
        l.updateRound(WEIGHT, l.getRound(GRADIENT), true);

        return weight - oldWeight;
    }

    public double getFrequency(Sign is, Sign os, double n) {
        if(is == POS && os == POS) {
            return frequencyIPosOPos;
        } else if(is == POS && os == NEG) {
            return frequencyIPosONeg;
        } else if(is == NEG && os == POS) {
            return frequencyINegOPos;
        }

        return n - (frequencyIPosOPos + frequencyIPosONeg + frequencyINegOPos);
    }

    public void setFrequency(Sign is, Sign os, double f) {
        if(is == POS && os == POS) {
            frequencyIPosOPos = f;
        } else if(is == POS && os == NEG) {
            frequencyIPosONeg = f;
        } else if(is == NEG && os == POS) {
            frequencyINegOPos = f;
        } else {
            throw new UnsupportedOperationException();
        }
        modified = true;
    }

    public void count(Link l) {
        sampleSpace.update(l.getInput().getReference());

        if(l.getInput().isActive(false) && l.getOutput().isActive(false)) {
            frequencyIPosOPos += 1.0;
            modified = true;
        } else if(l.getInput().isActive(false) && !l.getOutput().isActive(false)) {
            frequencyIPosONeg += 1.0;
            modified = true;
        } else if(!l.getInput().isActive(false) && l.getOutput().isActive(false)) {
            frequencyINegOPos += 1.0;
            modified = true;
        }
    }

    public Model getModel() {
        return getPOutput().getModel();
    }

    public double getSurprisal(Sign si, Sign so, Reference ref) {
        double N = sampleSpace.getN(ref);
        if(isTemplate() || N == 0.0)
            return 0.0;

        double p = getP(si, so, N);
        return -Math.log(p);
    }

    public double getP(Sign si, Sign so, double n) {
        BetaDistribution dist = new BetaDistribution(
                getFrequency(si, so, n) + 1,
                n + 1
        );

        return dist.inverseCumulativeProbability(
                getModel().getConfig().getBetaThreshold()
        );
    }

    public double getWeight() {
        return weight;
    }

    public boolean isZero() {
        return Math.abs(weight) < TOLERANCE;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        modified = true;
    }

    public void addWeight(double weightDelta) {
        this.weight += weightDelta;
        modified = true;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getType());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeDouble(weight);

        out.writeDouble(frequencyIPosOPos);
        out.writeDouble(frequencyIPosONeg);
        out.writeDouble(frequencyINegOPos);

        sampleSpace.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readLong());
        output = m.lookupNeuron(in.readLong());

        weight = in.readDouble();

        frequencyIPosOPos = in.readDouble();
        frequencyIPosONeg = in.readDouble();
        frequencyINegOPos = in.readDouble();

        sampleSpace = SampleSpace.read(in, m);
    }

    public String toString() {
        return "S " +
                getClass().getSimpleName() +
                "  w:" + Utils.round(getWeight()) +
                " in:[" + input.getNeuron() + "](" + (isInputLinked() ? "+" : "-") + ")" +
                " --> out:[" + output.getNeuron() + "](" + (isOutputLinked() ? "+" : "-") + ")";
    }

    public String statToString() {
        int n = getModel().getN();
        return "f:" + Utils.round(getInput().getFrequency()) + " " +
                "N:" + Utils.round(n) + " " +
                "s(p,p):" + Utils.round(getSurprisal(POS, POS, null)) + " " +
                "s(n,p):" + Utils.round(getSurprisal(NEG, POS, null)) + " " +
                "s(p,n):" + Utils.round(getSurprisal(POS, NEG, null)) + " " +
                "s(n,n):" + Utils.round(getSurprisal(NEG, NEG, null)) + " \n";
    }
}
