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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.PatternPartNeuron;
import org.apache.commons.math3.distribution.BetaDistribution;

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

    public static double TOLERANCE = 0.0000001;

    protected NeuronProvider input;
    protected NeuronProvider output;

    private double weight;

    protected Instances instances;

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;
    private volatile boolean modified;


    public Synapse() {
    }

    public Synapse(I input, O output) {
        this.input = input.getProvider();
        this.output = output.getProvider();
    }

    public abstract boolean inductionRequired(Class<? extends Neuron> type);

    public abstract Visitor transition(Visitor v);

    public Reference getReference(Link l) {
        return l.getInput().getReference();
    }

    public abstract byte getType();

    public boolean isInputLinked() {
        return getInput().containsOutputSynapse(this);
    }

    public Activation getOutputActivationToLink(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron()) {
            return null;
        }

        return oAct;
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

    public void initInstance(Reference ref) {
        this.instances = new Instances(getModel(), ref);
    }

    public Instances getInstances() {
        return instances;
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
        instances.update(getModel(), l.getInput().getReference());

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
        double p = getP(si, so, instances.getN());
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

/*
    public boolean isWeak(State state) {
        return output.get().isWeak(this, state);
    }
*/
    public void setWeight(double weight) {
        this.weight = weight;
        modified = true;
    }

    public void addWeight(double weightDelta) {
        this.weight += weightDelta;
        modified = true;
    }

    public Link initInducedSynapse(Activation iAct, Activation oAct, Visitor v) {
        Link l = new Link(this, iAct, oAct, false);

        l.linkOutput();
        linkOutput();

        l.computeOutputGradient();
        l.removeGradientDependencies();
        oAct.addInputGradient(l.getOutputGradient());

        l.updateSelfGradient();

        if (!oAct.getConfig().checkSynapseInduction(l)) {
            return null;
        }

/*
        if(Neuron.debugOutput) {
            System.out.println("dbg:" + (Neuron.debugId++) + " " + s + "   FG:" + Utils.round(l.getFinalGradient()) + "               INDUCED!");
        }
*/

        l.updateSynapse();
//        oAct.sumUpLink(null, l);
//        l.linkInput();

        oAct.addLink(null, l);

        getInstances().update(getModel(), getReference(l));

        return l;
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

        instances.write(out);

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

        instances = Instances.read(in, m);

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
