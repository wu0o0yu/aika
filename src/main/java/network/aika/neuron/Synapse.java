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
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.FieldOutput;
import network.aika.fields.QueueField;
import network.aika.neuron.activation.*;
import network.aika.fields.Field;
import network.aika.neuron.bindingsignal.*;
import network.aika.sign.Sign;
import network.aika.utils.Bound;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.stream.Stream;

import static network.aika.direction.Direction.*;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.activation.Timestamp.MAX;
import static network.aika.neuron.activation.Timestamp.MIN;
import static network.aika.sign.Sign.NEG;
import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<S extends Synapse, I extends Neuron, O extends Neuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation<?>, OA extends Activation> implements Element, Writable {

    private static final Logger log = LoggerFactory.getLogger(Synapse.class);

    protected NeuronProvider input;
    protected NeuronProvider output;

    private boolean isInputLinked;
    private boolean isOutputLinked;

    protected S template;

    protected QueueField weight = new QueueField(this, "weight", () -> {
        setModified();
    });

    protected SampleSpace sampleSpace = new SampleSpace();

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;

    protected boolean allowTraining = true;

    public PrimitiveTransition getRelatedTransition() {
        return null;
    }

    public Stream<BindingSignal> getRelatedBindingSignals(PatternActivation fromOriginAct, PrimitiveTransition t, Direction dir) {
        return dir.getNeuron(this)
                .getRelatedBindingSignals(fromOriginAct, dir.getPrimitiveTerminal(t).getState());
    }

    public abstract double getSumOfLowerWeights();

    public boolean propagateCheck(IA iAct) {
        return true;
    }

    public boolean checkLinkingEvent(Activation act, Direction dir) {
        FieldOutput linkingEvent = getLinkingEvent(act, dir.invert());
        return linkingEvent == null || isTrue(linkingEvent);
    }

    public boolean linkCheck(BindingSignal inputBS, BindingSignal outputBS) {
        return inputBS.getOrigin() == outputBS.getOrigin();
    }

    public void linkAndPropagate(Transition t, Direction dir, BindingSignal... fromBSs) {
        t.link(this, dir, fromBSs);

        if (dir != OUTPUT)
            return;

        t.latentLinking(this, fromBSs);

        if (isPropagate())
            propagate(fromBSs[0]);
    }

    public L propagate(BindingSignal inputBS) {
        IA iAct = (IA) inputBS.getActivation();

//        if(!propagateCheck(iAct))
//            return null;

        if(propagateLinkExists(iAct))
            return null;

        OA oAct = getOutput().createActivation(iAct.getThought());
        oAct.init(this, iAct);

        return createLink(iAct, oAct);
    }

    public L link(Activation fromAct, Activation toAct, Direction dir) {
        if(!checkLinkingEvent(toAct, dir))
            return null;

        IA iAct = (IA) dir.getInput(fromAct, toAct);
        OA oAct = (OA) dir.getOutput(fromAct, toAct);

        if(linkExists(iAct, oAct))
            return null;

        if(!checkCausal(iAct, oAct))
            return null;

        return createLink(iAct, oAct);
    }

    protected boolean checkCausal(IA iAct, OA oAct) {
        return !Link.isCausal(iAct, oAct);
    }

    public boolean isPropagate() {
        double tsWeight = getWeight().getCurrentValue();
        double tnBias = getOutput().getBias().getCurrentValue();
        return tsWeight + tnBias > 0.0;
    }

    public boolean isLatentLinking() {
        return getWeight().getCurrentValue() + getSumOfLowerWeights() > 0.0;
    }

    public static boolean isLatentLinking(Synapse synA, Synapse synB) {
        double sumOfLowerWeights = Math.min(synA.getSumOfLowerWeights(), synB.getSumOfLowerWeights());
        return synA.getWeight().getCurrentValue() + synB.getWeight().getCurrentValue() + sumOfLowerWeights > 0.0;
    }

    public boolean hasOutputTerminal(State s) {
        return getTransitions()
                .map(t -> t.getOutput())
                .anyMatch(t -> t.matchesState(s));
    }

    public FieldOutput getLinkingEvent(Activation act, Direction dir) {
        if(act == null)
            return null;

        if(dir == INPUT)
            return null;

        return act.getIsFired();
    }

    public void registerTerminals(Thought t) {
        for (Direction dir : DIRECTIONS) {
            Neuron<?, ?> n = dir.getNeuron(this);
            n.getActivations(t).stream()
                    .forEach(act -> {
                                initFixedTransitions(act, dir);

                                Stream<BindingSignal> BSs = act.getBindingSignals();
                                BSs.forEach(bs ->
                                        notifyVariableTransitions(bs, dir)
                                );
                            }
                    );
        }
    }

    public void initFixedTransitions(Activation act, Direction dir) {
        getTransitions()
                .forEach(t ->
                        dir.getTerminal(t).initFixedTerminal(t, this, act)
                );
    }

    public void notifyVariableTransitions(BindingSignal bs, Direction dir) {
        getTransitions()
                .forEach(t ->
                        dir.getTerminal(t).notify(t, this, bs)
                );
    }

    public Stream<Terminal> getTerminals(Direction dir) {
        return getTransitions()
                .map(transition ->
                        dir.getTerminal(transition)
                );
    }

    public boolean linkExists(IA iAct, OA oAct) {
        Link existingLink = oAct.getInputLink(iAct.getNeuron());
        return existingLink != null && existingLink.getInput() == iAct;
    }

    public boolean propagateLinkExists(IA iAct) {
        return iAct.getOutputLinks(this)
                        .findAny()
                        .isPresent();
    }

    public abstract Stream<Transition> getTransitions();

    public abstract void setModified();

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public S instantiateTemplate(L l, I input, O output) {
        S s = instantiateTemplate(input, output);

        if(l != null)
            s.weight.setValue(computeInitialWeight(l.getInput()));

        return s;
    }

    public S instantiateTemplate(I input, O output) {
        S s = instantiateTemplate();

        s.input = input.getProvider();
        s.output = output.getProvider();

        return s;
    }

    public S instantiateTemplate() {
        S s;
        try {
            s = (S) getClass().getConstructor().newInstance();
            initNewInstance(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    protected void initNewInstance(S s) {
        s.template = this;
        s.weight.setValue(weight.getCurrentValue());
    }

    public abstract L createLink(IA input, OA output);

    public boolean checkCandidateSynapse(IA iAct) {
        double candidateWeight = computeInitialWeight(iAct);
        double candidateNet = (iAct.getValueLB().getCurrentValue() * candidateWeight) + getOutput().getBias().getCurrentValue();

        return candidateNet >= 0.0;
    }

    protected double computeInitialWeight(IA iAct) {
        double initialWeight = weight.getCurrentValue();
        initialWeight -= iAct.getConfig().getLearnRate() *
                        iAct.getNeuron().getSurprisal(POS, iAct.getAbsoluteRange(), true);
        return initialWeight;
    }

    public void setWeight(double w) {
        weight.setValue(w);
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public S getTemplate() {
        return template;
    }

    public boolean isOfTemplate(Synapse templateSynapse) {
        if(template == templateSynapse)
            return true;

        if(template == null)
            return false;

        return template.isOfTemplate(templateSynapse);
    }

    public boolean isInputLinked() {
        return isInputLinked;
    }

    public void linkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = true;
        in.addOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public void unlinkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = false;
        in.removeOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public boolean isOutputLinked() {
        return isOutputLinked;
    }

    public void linkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = true;
        out.addInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public void unlinkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = false;
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
        double oldN = sampleSpace.getN();

        boolean iActive = l.getInput().isFired();
        boolean oActive = l.getOutput().isFired();

        Range absoluteRange = l.getInput().getAbsoluteRange();

        sampleSpace.countSkippedInstances(absoluteRange);

        sampleSpace.count();

        if(oActive) {
            Double alpha = l.getConfig().getAlpha();
            if (alpha != null)
                applyMovingAverage(
                        Math.pow(alpha, sampleSpace.getN() - oldN)
                );
        }

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
        return output != null ?
                output.getModel() :
                null;
    }

    public double getSurprisal(Sign si, Sign so, Range range, boolean addCurrentInstance) {
        double n = sampleSpace.getN(range);
        double p = getProbability(si, so, n, addCurrentInstance);
        return Utils.surprisal(p);
    }

    public double getProbability(Sign si, Sign so, double n, boolean addCurrentInstance) {
        double f = getFrequency(si, so, n);

        // Add the current instance
        if(addCurrentInstance) {
            f += 1.0;
            n += 1.0;
        }

        return Bound.UPPER.probability(f, n);
    }

    public Field getWeight() {
        return weight;
    }

    public boolean isZero() {
        return Utils.belowTolerance(weight.getCurrentValue());
    }

    public boolean isNegative() {
        return weight.getCurrentValue() < 0.0;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getClass().getName());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeBoolean(isInputLinked);
        out.writeBoolean(isOutputLinked);

        weight.write(out);

        out.writeDouble(frequencyIPosOPos);
        out.writeDouble(frequencyIPosONeg);
        out.writeDouble(frequencyINegOPos);

        sampleSpace.write(out);
    }

    public static Synapse read(DataInput in, Model m) throws IOException {
        String synClazz = in.readUTF();

        Synapse s = (Synapse) m.modelClass(synClazz);
        s.readFields(in, m);
        return s;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readLong());
        output = m.lookupNeuron(in.readLong());

        isInputLinked = in.readBoolean();
        isOutputLinked = in.readBoolean();

        weight.readFields(in, m);

        frequencyIPosOPos = in.readDouble();
        frequencyIPosONeg = in.readDouble();
        frequencyINegOPos = in.readDouble();

        sampleSpace = SampleSpace.read(in, m);
    }

    @Override
    public Timestamp getCreated() {
        return MIN;
    }

    @Override
    public Timestamp getFired() {
        return MAX;
    }

    @Override
    public Thought getThought() {
        Model m = getModel();
        return m != null ?
                m.getCurrentThought() :
                null;
    }

    public static <S extends Synapse> S init(S ts, Neuron input, Neuron output, double initialWeight) {
        ts.setInput(input);
        ts.setOutput(output);
        ts.getWeight().setValue(initialWeight);
        ts.linkInput();
        ts.linkOutput();

        return ts;
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + (input != null ? input.getNeuron().toKeyString() : "--")  + "](" + (isInputLinked ? "+" : "-") + ") " +
                getArrow() +
                " out:[" + (output != null ? output.getNeuron().toKeyString() : "--") + "](" + (isOutputLinked ? "+" : "-") + ")";
    }

    private String getArrow() {
        if(isPropagate())
            return "==>";
        if(isLatentLinking())
            return "-=>";
        return "-->";
    }
}
