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
import network.aika.neuron.Neuron;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.fields.*;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.steps.activation.BranchProbability;
import network.aika.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.activation.Timestamp.NOT_SET_AFTER;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    protected Map<Activation<?>, BranchBindingSignal> reverseBindingSignals = new TreeMap<>();

    private Timestamp finalTimestamp = NOT_SET;

    private final Set<BindingActivation> branches = new TreeSet<>();
    private BindingActivation mainBranch;

    private double branchProbability = 1.0;
    private Field ownInputGradient = new QueueField(this, "ownInputGradient");

    protected MultiSourceFieldOutput ownOutputGradientMul = new FieldMultiplication(
            ownInputGradient,
            new FieldFunction(net, x ->
                    getNeuron().getActivationFunction().outerGrad(x)
            )
    );
    protected Field ownOutputGradient = new QueueField(this, "ownOutputGradient");

    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);

        ownInputGradient.setFieldListener(u -> {
                    if (ownOutputGradientMul.updateAvailable(1))
                        ownOutputGradient.addAndTriggerUpdate(ownOutputGradientMul.getUpdate(1));
                }
        );

        outputGradient.setFieldListener(u ->
                propagateGradient(u, true, false)
        );

        ownOutputGradient.setFieldListener(u ->
                propagateGradient(u, false, true)
        );
    }

    public void registerReverseBindingSignal(Activation targetAct, BranchBindingSignal bindingSignal) {
        reverseBindingSignals.put(targetAct, bindingSignal);
    }

    @Override
    public Stream<BranchBindingSignal> getReverseBindingSignals(Neuron toNeuron) {
        return reverseBindingSignals.values().stream()
                .filter(bs -> bs.getActivation().getNeuron().neuronMatches(toNeuron));
    }

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addBindingSignal(new BranchBindingSignal(this));
    }

    @Override
    public boolean checkAllowPropagate() {
        if(isTemplate()) {
            if (isNetworkInput())
                return false;

            if (getOutputGradient().updateAvailable())
                return false;
        }

        return super.checkAllowPropagate();
    }

    public boolean checkPropagateBranchBindingSignal(BranchBindingSignal bs) {
        return bs.getOriginActivation() == this;
    }

    protected void updateValue(double net) {
        if(!isInput)
            value.setAndTriggerUpdate(getBranchProbability() * getActivationFunction().f(net));
    }

    public boolean isSelfRef(Activation iAct) {
        return iAct != null && iAct.branchBindingSignals.containsKey(this);
    }

    protected void propagateGradient() {
        super.propagateGradient();

        if(ownOutputGradientMul.updateAvailable(2))
            ownOutputGradient.addAndTriggerUpdate(ownOutputGradientMul.getUpdate(2));
    }

    public BindingActivation createBranch(Synapse excludedSyn) {
        BindingActivation clonedAct = getNeuron().createActivation(getThought());
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        clonedAct.init(null, this);

        copySteps(clonedAct);
        linkClone(clonedAct, excludedSyn);

        return clonedAct;
    }

    @Override
    public void addFeedbackSteps() {
        BranchProbability.add(this);
    }

    @Override
    public Range getRange() {
        PatternBindingSignal bs = getPrimaryPatternBindingSignal();
        if(bs == null)
            return null;

        return bs.getOriginActivation()
                .getRange();
    }

    private PatternBindingSignal getPrimaryPatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .filter(bs -> NOT_SET_AFTER.compare(bs.getOriginActivation().getFired(), fired) < 0)
                .filter(bs -> !bs.isRelated())
                .min(Comparator.comparing(bs -> bs.isInput() ? 1 : 0))
                .orElse(null);
    }

    public PatternBindingSignal getSamePatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .filter(bs -> !bs.isInput() && !bs.isRelated())
                .findAny()
                .orElse(null);
    }

    public Field getOwnInputGradient() {
        return ownInputGradient;
    }

    public FieldOutput getOwnOutputGradient() {
        return ownOutputGradient;
    }

    public void updateBias(double u) {
        getNet().addAndTriggerUpdate(u);
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

    public boolean checkIfPrimaryInputBNLinkAlreadyExists() {
        return inputLinks.values().stream()
                .anyMatch(l -> l instanceof PrimaryInputBNLink<?>);
    }
}
