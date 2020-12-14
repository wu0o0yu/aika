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
import network.aika.Writable;
import network.aika.neuron.activation.*;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Sign.NEG;
import static network.aika.neuron.Sign.POS;

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

    protected SampleSpace sampleSpace = new SampleSpace();

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;
    private volatile boolean modified;


    public Synapse() {
    }

    public Synapse(I input, O output, Synapse template) {
        this.input = input.getProvider();
        this.output = output.getProvider();
        this.template = template;

        assert input.getId() < 0 || input.getId() != output.getId();
    }

    public boolean isTemplate() {
        return template == null;
    }

    public Synapse getTemplate() {
        return template;
    }

    public abstract Synapse instantiateTemplate(I input, O output);

    public abstract void transition(Visitor v, Activation currentAct, Activation nextAct, boolean create);

    protected abstract boolean checkOnCreate(Activation fromAct, Activation toAct, Visitor v);

    public abstract boolean checkTemplate(Activation iAct, Activation oAct, Visitor v);

    public abstract boolean checkInduction(Link l);

    public abstract boolean checkTemplatePropagate(Visitor v, Activation act);

    public abstract byte getType();

    public abstract void updateReference(Link l);

    public Reference getReference(Link l) {
        return l.getInput().getReference();
    }

    public boolean isInputLinked() {
        return getInput().containsOutputSynapse(this);
    }

    public abstract Activation getOutputActivationToLink(Activation oAct, Visitor v);

    public void follow(Activation fromAct, Activation toAct, Visitor v, boolean create) {
        if(create) {
            createOutgoingLink(fromAct, toAct, v);
        } else {
            toAct.getNeuron()
                    .transition(v, toAct, false);
        }
    }

    public void createOutgoingLink(Activation fromAct, Activation toAct, Visitor v) {
        if(!checkOnCreate(fromAct, toAct, v)) {
            return;
        }

        Config c = fromAct.getConfig();

        // TODO: Check direction
        if (toAct == null) {
            toAct = fromAct.createActivation(getOutput());

            toAct.addToQueue(
                    v.getPhase().getNextActivationPhases(c)
            );
        } else {
            Link ol = toAct.getInputLink(this);
            if (ol != null) {
//                    toAct = oAct.cloneToReplaceLink(s);
                log.warn("Link already exists!  " + toAct.getThought());
                return;
            }
        }
        Link nl = Link.link(
                this,
                fromAct,
                toAct,
                v.getSelfRef()
        );

        if (nl.getSynapse().getWeight() > 0.0 || nl.getSynapse().isTemplate()) {
            nl.addToQueue(
                    v.getPhase().getNextLinkPhases(c)
            );
        }
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

    public SampleSpace getInstances() {
        return sampleSpace;
    }

    public double getFrequency(Sign is, Sign os, double n) {
        if(is == POS && os == POS) {
            return frequencyIPosOPos;
        } else if(is == POS && os == NEG) {
            return frequencyIPosONeg;
        } else if(is == NEG && os == POS) {
            return frequencyINegOPos;
        } else {
            return n - (frequencyIPosOPos + frequencyIPosONeg + frequencyINegOPos);
        }
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
        sampleSpace.update(getModel(), l.getInput().getReference());

        if(l.getInput().isActive() && l.getOutput().isActive()) {
            frequencyIPosOPos += 1.0;
            modified = true;
        } else if(l.getInput().isActive() && !l.getOutput().isActive()) {
            frequencyIPosONeg += 1.0;
            modified = true;
        } else if(!l.getInput().isActive() && l.getOutput().isActive()) {
            frequencyINegOPos += 1.0;
            modified = true;
        }
    }

    public Model getModel() {
        return getPOutput().getModel();
    }

    public double getSurprisal(Sign si, Sign so) {
        if(isTemplate())
            return 0.0;

        double p = getP(si, so, sampleSpace.getN());
        return -Math.log(p);
    }

    public double getP(Sign si, Sign so, double n) {
        BetaDistribution dist = new BetaDistribution(getFrequency(si, so, n) + 1, n + 1);

        return dist.inverseCumulativeProbability(
                getModel().getBetaThreshold()
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

        out.writeBoolean(modified);
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

        modified = in.readBoolean();
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
                "s(p,p):" + Utils.round(getSurprisal(POS, POS)) + " " +
                "s(n,p):" + Utils.round(getSurprisal(NEG, POS)) + " " +
                "s(p,n):" + Utils.round(getSurprisal(POS, NEG)) + " " +
                "s(n,n):" + Utils.round(getSurprisal(NEG, NEG)) + " \n";
    }
}
