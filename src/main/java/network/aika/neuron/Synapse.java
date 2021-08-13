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

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.VisitorStep;
import network.aika.neuron.steps.link.PropagateGradientAndUpdateWeight;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Neuron.BETA_THRESHOLD;
import static network.aika.neuron.sign.Sign.NEG;
import static network.aika.neuron.sign.Sign.POS;
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

    public abstract void transition(ActVisitor v, Synapse s, Link l);

    public void alternateBranchTransition(ActVisitor v, Synapse s, Link l) {

    }

    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        return false;
    }

    public void samePatternTransitionLoop(ActVisitor v, Link l) {
    }

    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
    }

    public void patternTransitionLoop(ActVisitor v, Link l) {
    }

    public void inhibitoryTransitionLoop(ActVisitor v, Link l) {
    }

    public void negativeSynapseTransitionLoop(ActVisitor v, Link l) {
    }

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    private void init(Model m) {
        sampleSpace = new SampleSpace(m);
    }

    public Synapse<I, O> instantiateTemplate(I input, O output) {
        Synapse<I, O> s = instantiateTemplate();

        s.input = input.getProvider();
        s.output = output.getProvider();
        return s;
    }

    public Synapse<I, O> instantiateTemplate() {
        Synapse<I, O> s;
        try {
            s = getClass().getConstructor().newInstance();
            s.init(getModel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        initFromTemplate(s);
        return s;
    }

    public Synapse getConcreteSynapse(Neuron<?> in, Neuron<?> on) {
        if(on.getTemplate().getId() != output.getId())
            return null;

        Synapse cs = on.getInputSynapse(in.getProvider());
        if(cs == null || cs.getTemplateSynapseId() != getTemplateSynapseId())
            return null;

        return cs;
    }

    public abstract void updateSynapse(Link l, double delta);

    protected abstract boolean checkCausality(Activation iAct, Activation oAct, Visitor v);

    public abstract void updateReference(Link l);

    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        return oAct;
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    /*
    public static boolean synapseExists(Neuron iN, Neuron oN) {
        return oN.getInputSynapse(iN.getProvider()) != null;
    }
*/
    public boolean isTemplate() {
        return template == null;
    }

    public Synapse getTemplate() {
        if(isTemplate())
            return this;
        return template;
    }

    public TemplateSynapseInfo getTemplateInfo() {
        assert isTemplate();
        if (templateInfo == null) {
            templateInfo = new TemplateSynapseInfo();
        }

        return templateInfo;
    }

    public boolean isOfTemplate(Synapse templateSynapse) {
        return getTemplateSynapseId() == templateSynapse.getTemplateSynapseId();
    }

    public byte getTemplateSynapseId() {
        return getTemplate().getTemplateInfo().getTemplateSynapseId();
    }

    protected void initFromTemplate(Synapse s) {
        s.weight = weight;
        s.template = this;
    }

    public boolean checkLoopClosure(ActVisitor v) {
        return true;
    }

    public Reference getReference(Link l) {
        return l.getInput().getReference();
    }

    public boolean isInputLinked() {
        return getInput().containsOutputSynapse(this);
    }

    public boolean isRecurrent() {
        return false;
    }

    public void propagate(Activation fromAct, Direction dir, VisitorStep vs, boolean isSelfRef) {
        Activation toAct = fromAct.getThought()
                .createActivation(
                        dir.getNeuron(this),
                        fromAct
                );

        vs.getNextSteps(toAct);

        createLink(
                dir.getInput(fromAct, toAct),
                dir.getOutput(fromAct, toAct),
                vs,
                isSelfRef
        );
    }

    public void closeLoop(VisitorStep vs, ActVisitor v, Activation iAct, Activation oAct) {
        if (!checkCausality(iAct, oAct, v))
            return;

        createLink(iAct, oAct, vs, v.getSelfRef());
    }

    public void createLink(Activation iAct, Activation oAct, VisitorStep vs, boolean isSelfRef) {
        Link nl = oAct.addLink(
                this,
                iAct,
                isSelfRef
        );

        Synapse s = nl.getSynapse();
        if (s.getWeight() <= 0.0 && !s.isTemplate())
            return;

        vs.getNextSteps(nl);

        if(!nl.getConfig().isEnableTraining())
            return;

        QueueEntry.add(nl, INFORMATION_GAIN_GRADIENT);

        if (!Utils.belowTolerance(oAct.getOutputGradientSum()))
            QueueEntry.add(nl, new PropagateGradientAndUpdateWeight(oAct.getOutputGradientSum()));
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

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequencyIPosOPos *= alpha;
        frequencyIPosONeg *= alpha;
        frequencyINegOPos *= alpha;
        modified = true;
    }

    public void count(Link l) {
        boolean iActive = l.getInput().isActive(false);
        boolean oActive = l.getOutput().isActive(false);

        sampleSpace.countSkippedInstances(l.getInput().getReference());

        if(oActive) {
            Double alpha = l.getConfig().getAlpha();
            if (alpha != null)
                applyMovingAverage(alpha);
        }

        sampleSpace.count();

        if(iActive && oActive) {
            frequencyIPosOPos += 1.0;
            modified = true;
        } else if(iActive && !oActive) {
            frequencyIPosONeg += 1.0;
            modified = true;
        } else if(!iActive && oActive) {
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
                BETA_THRESHOLD
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
        out.writeByte(getTemplate().getTemplateInfo().getTemplateSynapseId());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeDouble(weight);

        out.writeDouble(frequencyIPosOPos);
        out.writeDouble(frequencyIPosONeg);
        out.writeDouble(frequencyINegOPos);

        sampleSpace.write(out);
    }

    public static Synapse read(DataInput in, Model m) throws IOException {
        byte templateSynapseId = in.readByte();
        Synapse templateSynapse = m.getTemplates().getTemplateSynapse(templateSynapseId);
        Synapse s = templateSynapse.instantiateTemplate();
        s.readFields(in, m);
        return s;
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
        long n = getModel().getN();
        return "f:" + Utils.round(getInput().getFrequency()) + " " +
                "N:" + Utils.round(n) + " " +
                "s(p,p):" + Utils.round(getSurprisal(POS, POS, null)) + " " +
                "s(n,p):" + Utils.round(getSurprisal(NEG, POS, null)) + " " +
                "s(p,n):" + Utils.round(getSurprisal(POS, NEG, null)) + " " +
                "s(n,n):" + Utils.round(getSurprisal(NEG, NEG, null)) + " \n";
    }
}
