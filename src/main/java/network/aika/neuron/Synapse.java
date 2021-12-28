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
import network.aika.neuron.activation.*;
import network.aika.direction.Direction;
import network.aika.fields.Field;
import network.aika.sign.Sign;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Neuron.BETA_THRESHOLD;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.sign.Sign.NEG;
import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<I extends Neuron, O extends Neuron<?, A>, A extends Activation> implements Writable {

    private static final Logger log = LoggerFactory.getLogger(Synapse.class);

    protected NeuronProvider input;
    protected NeuronProvider output;

    private Synapse template;
    private TemplateSynapseInfo templateInfo;

    protected Field weight = new Field(u -> weightUpdate(u));


    protected SampleSpace sampleSpace = new SampleSpace();

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;

    protected boolean allowTraining = true;

    public abstract boolean checkBindingSignal(BindingSignal fromBS, Direction dir);

    public boolean checkRelatedBindingSignal(BindingSignal fromBS, BindingSignal toBS, Direction dir) {
        return checkRelatedBindingSignal(
                dir.getInputBindingSignal(fromBS, toBS),
                dir.getOutputBindingSignal(fromBS, toBS)
        );
    }

    public boolean checkRelatedBindingSignal(BindingSignal iBS, BindingSignal oBS) {
        Byte targetOutputScope = transitionScope(iBS.getScope(), OUTPUT);
        return targetOutputScope != null && targetOutputScope.byteValue() == oBS.getScope();
    }

    public Byte transitionScope(Byte fromScope, Direction dir) {
        return fromScope;
    }

    public abstract void setModified();

    public boolean checkTemplatePropagate(Activation act) {
        return false;
    }

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public Synapse<I, O, A> instantiateTemplate(I input, O output) {
        Synapse<I, O, A> s = instantiateTemplate();

        s.input = input.getProvider();
        s.output = output.getProvider();
        return s;
    }

    public Synapse<I, O, A> instantiateTemplate() {
        Synapse<I, O, A> s;
        try {
            s = getClass().getConstructor().newInstance();
            s.weight = weight;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        initFromTemplate(s);
        return s;
    }

    public abstract void updateSynapse(Link l, double delta);

    public abstract boolean checkCausality(Activation<?> iAct, Activation<?> oAct);

    public A branchIfNecessary(Activation iAct, A oAct) {
        return oAct;
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public boolean checkTemplateLink(Activation iAct, Activation oAct) {
        return true;
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
        s.weight.setInitialValue(weight.getCurrentValue());
        s.template = this;
    }

    public boolean isInputLinked() {
        return getInput().containsOutputSynapse(this);
    }

    public boolean isRecurrent() {
        return false;
    }

    public Neuron getTemplatePropagateTargetNeuron(Activation<?> act) {
        return getOutput();
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

        //TODO:
        return Math.max(n - (frequencyIPosOPos + frequencyIPosONeg + frequencyINegOPos), 0);
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
        setModified();
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequencyIPosOPos *= alpha;
        frequencyIPosONeg *= alpha;
        frequencyINegOPos *= alpha;
        setModified();
    }

    public void count(Link l) {
        boolean iActive = l.getInput().isFired();
        boolean oActive = l.getOutput().isFired();

        Range absoluteRange = l.getInput().getAbsoluteRange();

        sampleSpace.countSkippedInstances(absoluteRange);

        if(oActive) {
            Double alpha = l.getConfig().getAlpha();
            if (alpha != null)
                applyMovingAverage(alpha);
        }

        sampleSpace.count();

        if(iActive && oActive) {
            frequencyIPosOPos += 1.0;
            setModified();
        } else if(iActive) {
            frequencyIPosONeg += 1.0;
            setModified();
        } else if(oActive) {
            frequencyINegOPos += 1.0;
            setModified();
        }

        sampleSpace.updateLastPosition(absoluteRange);
    }

    public Model getModel() {
        return getPOutput().getModel();
    }

    public double getRelativeSurprisal(Sign si, Sign so, Range range) {
        double s = getSurprisal(si, so, range);
        s -= getInput().getSurprisal(si, range);
        s -= getOutput().getSurprisal(so, range);

        return s;
    }

    public double getSurprisal(Sign si, Sign so, Range range) {
        double N = sampleSpace.getN(range);
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

    /**
     * Determines whether this synapse is able to influence the boolean decision of its output neuron.
     */
    public abstract boolean isWeak();

    public Field getWeight() {
        return weight;
    }

    public boolean isZero() {
        return Utils.belowTolerance(weight.getCurrentValue());
    }

    public boolean isNegative() {
        return weight.getCurrentValue() < 0.0;
    }

    protected void weightUpdate(double u) {
        getOutput()
                .getActivations(getModel().getCurrentThought())
                .stream()
                .map(act -> act.getInputLink(this))
                .filter(l -> l != null)
                .forEach(l -> l.receiveWeightUpdate());
        setModified();
    }

    public boolean propagateValue(Link<A> l) {
        return true;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getTemplate().getTemplateInfo().getTemplateSynapseId());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        weight.write(out);

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

        weight.readFields(in, m);

        frequencyIPosOPos = in.readDouble();
        frequencyIPosONeg = in.readDouble();
        frequencyINegOPos = in.readDouble();

        sampleSpace = SampleSpace.read(in, m);
    }

    public String toString() {
        return "S " +
                getClass().getSimpleName() +
                "  w:" + getWeight() +
                " in:[" + input.getNeuron() + "](" + (isInputLinked() ? "+" : "-") + ")" +
                " --> out:[" + output.getNeuron() + "](" + (isOutputLinked() ? "+" : "-") + ")";
    }
}
