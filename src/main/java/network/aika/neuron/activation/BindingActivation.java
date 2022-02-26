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
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.BranchBindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.neuron.conjunctive.PatternSynapse;
import network.aika.steps.activation.Linking;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;
import static network.aika.fields.FieldUtils.*;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.activation.Timestamp.NOT_SET_AFTER;
import static network.aika.steps.LinkingOrder.POST_FIRED;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    protected NavigableMap<Activation<?>, BranchBindingSignal> reverseBindingSignals = new TreeMap<>(NEURON_COMPARATOR);

    private Timestamp finalTimestamp = NOT_SET;

    private PatternBindingSignal bound;

    private final Set<BindingActivation> branches = new TreeSet<>();
    private BindingActivation mainBranch;
    private Field branchProbability = new Field("Branch-Probability");
    private Field bpNorm = new Field("BP-Norm");

    private Field ownInputGradient;
    protected Field ownOutputGradient;

    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);

        branchProbability.setAndTriggerUpdate(1.0);

        if (getNeuron().isNetworkInput())
            return;

        ownInputGradient = new QueueField(this, "Own-Input-Gradient");
        ownOutputGradient = new QueueField(this, "Own-Output-Gradient");

        func(
                    "f(bp * net)",
                    mul(
                            "bp * net",
                            branchProbability,
                            net
                    ),
                    x -> getNeuron().getActivationFunction().f(x),
                    value
            );

        mul(
                "ownIG * f'(net)",
                ownInputGradient,
                func(
                        "f'(net)",
                        net,
                        x -> getNeuron().getActivationFunction().outerGrad(x)
                ),
                ownOutputGradient
        );

        net.addFieldListener("propagateConflictingNetChange", (l, u) ->
                propagateConflictingNetChange()
        );

        func(
                "exp(net)",
                net,
                x -> Math.exp(x),
                bpNorm
        );

        outputGradient.addFieldListener("updateWeights", (l, u) ->
                updateWeights(u)
        );

        ownOutputGradient.addFieldListener("propagateGradient", (l, u) ->
                propagateGradient()
        );
    }

    @Override
    protected void initFields() {
        // Override parent
    }

    @Override
    protected void onFinal() {
        super.onFinal();

        div(
                "exp(net) / bpNorm",
                func(
                        "exp(net)",
                        net,
                        x -> Math.exp(x)
                ),
                bpNorm,
                branchProbability
        );
    }

    public void registerReverseBindingSignal(Activation targetAct, BranchBindingSignal bindingSignal) {
        reverseBindingSignals.put(targetAct, bindingSignal);
    }

    @Override
    public Stream<BranchBindingSignal> getReverseBindingSignals(Neuron toNeuron) {
        if(toNeuron.isTemplate()) {
            return reverseBindingSignals.values().stream()
                    .filter(bs -> bs.getActivation().getNeuron().templateNeuronMatches(toNeuron));
        } else {
            return reverseBindingSignals.subMap(
                    new DummyActivation(0, toNeuron),
                    new DummyActivation(Integer.MAX_VALUE, toNeuron)
            ).values().stream();
        }
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

    @Override
    public boolean isBoundToConflictingBS(BindingSignal conflictingBS) {
        return BindingSignal.originEquals(conflictingBS, bound);
    }

    @Override
    public boolean checkPropagateBranchBindingSignal(BranchBindingSignal bs) {
        return bs.getOriginActivation() == this;
    }

    private void propagateConflictingNetChange() {
        reverseBindingSignals.values().stream()
                .filter(bs -> bs.getActivation() instanceof BindingActivation)
                .map(bs -> (BindingActivation) bs.getActivation())
                .filter(act -> !act.isMainBranch())
                .forEach(act -> act.onConflictingNetChange(net));
    }

    private void onConflictingNetChange(Field inputNet) {
        double expUpdate = Math.exp(inputNet.getNewValue()) - Math.exp(inputNet.getCurrentValue());

        bpNorm.addAndTriggerUpdate(expUpdate);
    }

    @Override
    public boolean isSelfRef(Activation iAct) {
        return iAct != null && iAct.branchBindingSignals.containsKey(this);
    }

    public BindingActivation createBranch(NegativeFeedbackSynapse excludedSyn) {
        BindingActivation clonedAct = getNeuron().createActivation(getThought());
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        clonedAct.init(null, this);

        copySteps(clonedAct);
        copyBindingSignals(clonedAct);
//        linkClone(clonedAct, excludedSyn);

        return clonedAct;
    }

    private void copyBindingSignals(BindingActivation clonedAct) {
        getPatternBindingSignals().values().stream()
                .filter(bs -> !bs.isOwnPatternBS())
                .forEach(bs ->
                        clonedAct.addBindingSignal(
                                bs.clone(clonedAct)
                        )
                );
    }

    @Override
    protected void onBindingSignalArrived(BindingSignal bs) {
        if(bs.isOwnPatternBS()) {
            bound = (PatternBindingSignal) bs;
            onBound();
        }

        super.onBindingSignalArrived(bs);
    }

    @Override
    protected void onBindingSignalArrivedFired(BindingSignal bs) {
        Linking.add(this, bs, OUTPUT, POST_FIRED, false, "PATTERN-SYN", s -> s instanceof PatternSynapse);

        if(isFinal())
            onBindingSignalArrivedFinalFired(bs);

        if(isBound())
            onBindingSignalArrivedFiredBound(bs);

        if(isFinal() && isBound()) {
            onBindingSignalArrivedFinalFiredBound(bs);
        }
    }

    protected void onBindingSignalArrivedFiredBound(BindingSignal bs) {
        Linking.add(this, bs, OUTPUT, POST_FIRED, false, "", s -> true);

        if(isFinal())
            onBindingSignalArrivedFinalFiredBound(bs);
    }

    protected void onBindingSignalArrivedFinalFiredBound(BindingSignal bs) {

    }

    protected void onBound() {

        if(isFired()) {
            getBindingSignals()
                    .forEach(bs ->
                            onBindingSignalArrivedFiredBound(bs)
                    );
        }
    }

    public boolean isBound() {
        return bound != null;
    }

    public PatternBindingSignal getBoundPatternBindingSignal() {
        return bound;
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

    public Field getBpNorm() {
        return bpNorm;
    }

    public Field getBranchProbability() {
        return branchProbability;
    }

    public void receiveOwnGradientUpdate(double u) {
        super.receiveOwnGradientUpdate(u);

        if(ownInputGradient == null)
            return;

        ownInputGradient.addAndTriggerUpdate(u);
    }

    public boolean checkIfPrimaryInputBNLinkAlreadyExists() {
        return inputLinks.values().stream()
                .anyMatch(l -> l instanceof PrimaryInputLink<?>);
    }

    public boolean isSeparateBranch(InhibitoryActivation iAct) {
        if(isMainBranch())
            return false;

        return iAct.getBranchBindingSignals().values().stream()
                .anyMatch(bbs -> bbs.getOriginActivation() == mainBranch);
    }

    public boolean isMainBranch() {
        return mainBranch == null;
    }
}
