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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.fields.*;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.StepType;
import network.aika.neuron.steps.activation.BranchProbability;
import network.aika.neuron.steps.activation.SetFinalMode;
import network.aika.utils.Utils;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.NOT_SET;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends Activation<BindingNeuron> {

    public static BindingActivation MIN_BINDING_ACT = new BindingActivation(0, null);
    public static BindingActivation MAX_BINDING_ACT = new BindingActivation(Integer.MAX_VALUE, null);

    private boolean finalMode = false;
    private Timestamp finalTimestamp = NOT_SET;

    private final Set<BindingActivation> branches = new TreeSet<>();
    private BindingActivation mainBranch;

    private double branchProbability = 1.0;
    private Field ownInputGradient = new QueueField(this, "ownInputGradient", Phase.LINKING, StepType.TRAINING);

    protected FieldOutput ownOutputGradient = new FieldMultiplication(
            ownInputGradient,
            new FieldFunction(net, x ->
                    getNeuron().getActivationFunction().outerGrad(x)
            )
    );

    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);

        inputGradient.setFieldListener(u -> {
                    if (outputGradient.updateAvailable(1))
                        propagateGradient(outputGradient.getUpdate(1, true), true, false);
                }
        );

        ownInputGradient.setFieldListener(u -> {
                    if (ownOutputGradient.updateAvailable(1))
                        propagateGradient(ownOutputGradient.getUpdate(1, true), false, true);
                }
        );
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addSelfBindingSignal((byte) 0);
    }

    protected void propagateGradient() {
        if (outputGradient.updateAvailable(2))
            propagateGradient(outputGradient.getUpdate(2, true), true, false);

        if (ownOutputGradient.updateAvailable(2))
            propagateGradient(ownOutputGradient.getUpdate(2, true), false, true);
    }

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    protected void propagateGradient(double g, boolean updateWeights, boolean backPropagate) {
        getNeuron().getFinalBias().addAndTriggerUpdate(getConfig().getLearnRate() * g);
        super.propagateGradient(g, updateWeights, backPropagate);
    }

    public BindingActivation createBranch(Synapse excludedSyn) {
        BindingActivation clonedAct = getNeuron().createActivation(getThought());
        clonedAct.init(null, this);

        copySteps(clonedAct);
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        linkClone(clonedAct, excludedSyn);

        return clonedAct;
    }

    public void addFeedbackSteps() {
        SetFinalMode.add(this);
        BranchProbability.add(this);
    }

    @Override
    public Range getRange() {
        BindingSignal bs = getPrimaryPatternBindingSignal();
        if(bs == null)
            return null;

        return bs.getBindingSignalAct()
                .getRange();
    }

    private BindingSignal getPrimaryPatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .filter(bs -> bs.getBindingSignalAct().getFired().compareTo(fired) < 0)
                .min(Comparator.comparing(bs -> bs.getScope()))
                .orElse(null);
    }

    public Field getOwnInputGradient() {
        return ownInputGradient;
    }

    public FieldOutput getOwnOutputGradient() {
        return ownOutputGradient;
    }

    public void updateBias(double u, boolean isFinalBias) {
        if(finalMode == isFinalBias)
            getNet().addAndTriggerUpdate(u);
    }

    public boolean isFinalMode() {
        return finalMode;
    }

    public Timestamp getFinalTimestamp() {
        return finalTimestamp;
    }

    public void setFinalTimestamp() {
        this.finalTimestamp = getThought().getCurrentTimestamp();
    }

    public BindingActivation getMainBranch() {
        return mainBranch;
    }

    public boolean hasBranches() {
        return !branches.isEmpty();
    }

    public Set<BindingActivation> getBranches() {
        return branches;
    }

    public Stream<BindingActivation> getAllBranches() {
        if (mainBranch != null)
            return Stream.concat(Stream.of(mainBranch), branches.stream());
        else
            return branches.stream();
    }

    public void setFinalMode() {
        finalMode = true;

        BindingNeuron n = getNeuron();

        double biasDelta = n.getFinalBias().getCurrentValue() - n.getBias().getCurrentValue();

        getNet().addAndTriggerUpdate(biasDelta - computeForwardLinkedRecurrentInputs(this));

        getPositiveRecurrentInputLinks(this)
                .filter(l -> !l.isForward())
                .forEach(l ->
                        l.propagateValue()
                );

        setFinalTimestamp();

        if (!isFired() || getNet().getCurrentValue() > 0.0)
            return;

        setFired(NOT_SET);
        propagate();
    }

    private double computeForwardLinkedRecurrentInputs(BindingActivation act) {
        return getPositiveRecurrentInputLinks(act)
                .filter(l -> l.isForward())
                .mapToDouble(l -> l.getSynapse().getWeight().getCurrentValue())
                .sum();
    }

    private Stream<Link> getPositiveRecurrentInputLinks(BindingActivation act) {
        return act.getInputLinks()
                .filter(l -> l.getSynapse().isRecurrent())
                .filter(l -> !l.isNegative());
    }

    @Override
    public double getBranchProbability() {
        return branchProbability;
    }

    public void setBranchProbability(double p) {
        branchProbability = p;
    }

    public void computeBranchProbability() {
        Stream<Link> linksStream = getBranches()
                .stream()
                .flatMap(Activation::getInputLinks)
                .filter(Link::isNegative)
                .flatMap(l -> l.getInput().getInputLinks());  // Walk through to the inhib. Activation.

        Set<BindingActivation> conflictingActs = linksStream
                .map(l -> (BindingActivation) l.getInput())
                .collect(Collectors.toSet());

        double offset = conflictingActs
                .stream()
                .mapToDouble(cAct -> cAct.getNet().getCurrentValue())
                .min()
                .getAsDouble();

        double norm = Math.exp(getNet().getCurrentValue() - offset);
        norm += conflictingActs
                .stream()
                .mapToDouble(cAct -> Math.exp(cAct.getNet().getCurrentValue() - offset))
                .sum();

        double p = Math.exp(getNet().getCurrentValue() - offset) / norm;

        if(Utils.belowTolerance(p - getBranchProbability()))
            return;
// TODO
//        BindingActivation cAct = act.clone(null);
//        cAct.setBranchProbability(p);
    }

    public void receiveOwnGradientUpdate(double u) {
        super.receiveOwnGradientUpdate(u);
        ownInputGradient.addAndTriggerUpdate(u);
    }

    public String toString(boolean includeLink) {
        return super.toString(includeLink) + " bp:" + Utils.round(branchProbability);
    }
}
