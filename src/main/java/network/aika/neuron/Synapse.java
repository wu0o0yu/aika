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
import network.aika.callbacks.VisitorEvent;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.activation.scopes.Transition;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.steps.activation.SumUpBias;
import network.aika.neuron.steps.link.SumUpLink;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.link.PropagateGradient;
import network.aika.neuron.sign.Sign;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.stream.Stream;

import static network.aika.callbacks.VisitorEvent.AFTER;
import static network.aika.callbacks.VisitorEvent.BEFORE;
import static network.aika.neuron.sign.Sign.NEG;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.activation.Link.linkExists;
import static network.aika.neuron.steps.link.LinkStep.INFORMATION_GAIN_GRADIENT;

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
    private TemplateSynapseInfo templateInfo;

    protected double weight;

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
        if(isTemplate())
            return this;
        return template;
    }

    public TemplateSynapseInfo getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(TemplateSynapseInfo templateInfo) {
        this.templateInfo = templateInfo;
    }

    public abstract Synapse instantiateTemplate(I input, O output);

    protected void initFromTemplate(Synapse s) {
        s.weight = weight;
    }

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

    public LinkVisitor transition(ActVisitor v, Link l) {
        LinkVisitor nv = v.prepareNextStep(this, l);

        if(nv != null)
            nv.incrementPathLength();

        return nv;
    }

    public Stream<Transition> transition(Scope s, Direction dir, boolean checkFinalRequirement) {
        return dir.getTransitions(s.getTemplate())
                .stream()
                .filter(t ->
                        t.check(this, checkFinalRequirement)
                )
                .map(t -> t.getInstance(dir, s));
    }

    public void follow(Activation toAct, LinkVisitor v) {
        v.onEvent(BEFORE);

        toAct.getNeuron()
                .transition(v, toAct);

        v.onEvent(AFTER);
    }

    public void propagate(Activation fromAct, ActVisitor v) {
        LinkVisitor nv = transition(v, null);
        if(nv == null)
            return;

        Direction dir = nv.startDir;

        Activation toAct = fromAct.getThought()
                .createActivation(
                        dir.getNeuron(this),
                        fromAct,
                        v
                );

        nv.getPhase().getNextSteps(toAct);

        createLink(
                dir.getPropagateInput(fromAct, toAct),
                dir.getPropagateOutput(fromAct, toAct),
                nv
        );
    }

    public void closeCycle(ActVisitor v, Activation iAct, Activation oAct) {
        LinkVisitor nv = transition(v, null);
        if(nv == null)
            return;

        if(!nv.isClosedCycle())
            return;

        if (linkExists(this, iAct, oAct))
            return;

        if (!checkCausality(iAct, oAct, v))
            return;

        createLink(iAct, oAct, nv);
    }

    public void createLink(Activation iAct, Activation oAct, LinkVisitor v) {
        Link nl = oAct.addLink(
                this,
                iAct,
                v.getSelfRef(),
                v
        );

        Synapse s = nl.getSynapse();
        if (s.getWeight() <= 0.0 && !s.isTemplate())
            return;

        QueueEntry.add(nl, INFORMATION_GAIN_GRADIENT);

        if(!Utils.belowTolerance(oAct.getOutputGradientSum())) {
            QueueEntry.add(nl, new PropagateGradient(oAct.getOutputGradientSum()));
        }

        v.getPhase().getNextSteps(nl);
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

    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true)) {
            addWeight(delta);

            QueueEntry.add(
                    l,
                    new SumUpLink(l.getInputValue(POS) * delta)
            );
        } else {
            addWeight(-delta);
            getOutput().addConjunctiveBias(delta, !l.isCausal());

            QueueEntry.add(
                    l.getOutput(),
                    new SumUpBias(delta)
            );
            QueueEntry.add(
                    l,
                    new SumUpLink((l.getInputValue(POS) * -delta) + delta)
            );
        }
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
