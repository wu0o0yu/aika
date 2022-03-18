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
import network.aika.fields.Field;
import network.aika.fields.FieldInput;
import network.aika.fields.FieldOutput;
import network.aika.fields.QueueField;
import network.aika.neuron.Neuron;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.steps.activation.Linking;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.fields.FieldOutput.isTrue;
import static network.aika.fields.FieldUtils.*;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.activation.Timestamp.NOT_SET_AFTER;
import static network.aika.neuron.bindingsignal.State.*;
import static network.aika.steps.LinkingOrder.POST_FIRED;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private Timestamp finalTimestamp = NOT_SET;

    private BindingSignal<PatternActivation> bound;

    private final Set<BindingActivation> branches = new TreeSet<>();
    private BindingActivation mainBranch;
    private Field branchProbability = new Field("Branch-Probability");
    private Field bpNorm = new Field("BP-Norm");

    protected BindingActivation(int id, BindingNeuron n) {
        super(id, n);
    }

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);

        branchProbability.setAndTriggerUpdate(1.0);

        if(!isInput) {
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
        }

        net.addFieldListener("propagateConflictingNetChange", (l, u) ->
                propagateConflictingNetChange()
        );

        func(
                "exp(net)",
                net,
                x -> Math.exp(x),
                bpNorm
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

    @Override
    public void init(Synapse originSynapse, Activation originAct) {
        super.init(originSynapse, originAct);
        addBindingSignal(new BindingSignal(this, BRANCH));
    }

    public void registerReverseBindingSignal(Activation targetAct, BindingSignal bindingSignal) {
        reverseBindingSignals.put(targetAct, bindingSignal);
    }

    @Override
    public Stream<BindingSignal> getReverseBindingSignals(Neuron toNeuron) {
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
    public boolean checkAllowPropagate() {
        if(isTemplate()) {
            if (isNetworkInput())
                return false;
        }

        return super.checkAllowPropagate();
    }

    @Override
    public boolean isBoundToConflictingBS(BindingSignal conflictingBS) {
        return BindingSignal.originEquals(conflictingBS, bound);
    }

    private void propagateConflictingNetChange() {
        reverseBindingSignals.values().stream()
                .filter(bs -> bs.getActivation() instanceof BindingActivation)
                .map(bs -> (BindingActivation) bs.getActivation())
                .filter(act -> !act.isMainBranch())
                .forEach(act -> act.onConflictingNetChange(net));
    }

    private void onConflictingNetChange(Field inputNet) {
        double expUpdate = Math.exp(inputNet.getNewValue());

        if(inputNet.isInitialized())
            expUpdate -= Math.exp(inputNet.getCurrentValue());

        bpNorm.addAndTriggerUpdate(expUpdate);
    }

    @Override
    public boolean isSelfRef(Activation iAct) {
        return iAct != null && iAct.bindingSignals.containsKey(this);
    }

    public BindingActivation createBranch() {
        BindingActivation clonedAct = getNeuron().createActivation(getThought());
        branches.add(clonedAct);
        clonedAct.mainBranch = this;
        clonedAct.init(null, this);

        copyBindingSignals(clonedAct);

        return clonedAct;
    }

    private void copyBindingSignals(BindingActivation clonedAct) {
        getPatternBindingSignals().values().stream()
                .filter(bs -> !bs.isOrigin())
                .forEach(bs ->
                        clonedAct.addBindingSignal(
                                bs.clone(clonedAct)
                        )
                );
    }

    @Override
    protected void onBindingSignalArrived(BindingSignal bs) {
        if(bs.getState() == SAME) {
            bound = bs;
            onBound();
        }

        super.onBindingSignalArrived(bs);
    }

    @Override
    protected void onBindingSignalArrivedFired(BindingSignal bs) {
        Linking.addUnboundLinking(this, bs);

        super.onBindingSignalArrivedFired(bs);

        if(isBound())
            onBindingSignalArrivedFiredBound(bs);

        if(isTrue(isFinal) && isBound()) {
            onBindingSignalArrivedFinalFiredBound(bs);
        }
    }

    protected void onBindingSignalArrivedFiredBound(BindingSignal bs) {
        Linking.add(this, bs, POST_FIRED);

        if(isTrue(isFinal))
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

    public BindingSignal<PatternActivation> getBoundPatternBindingSignal() {
        return bound;
    }

    @Override
    public Range getRange() {
        BindingSignal bs = getPrimaryPatternBindingSignal();
        if(bs == null)
            return null;

        return bs.getOriginActivation()
                .getRange();
    }

    private BindingSignal<?> getPrimaryPatternBindingSignal() {
        return getPatternBindingSignals().values().stream()
                .filter(bs -> NOT_SET_AFTER.compare(bs.getOriginActivation().getFired(), fired) < 0)
                .filter(bs -> bs.getState() == SAME || bs.getState() == INPUT)
                .min(Comparator.comparing(bs -> bs.getState().ordinal()))
                .orElse(null);
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

    public boolean checkIfPrimaryInputBNLinkAlreadyExists() {
        return inputLinks.values().stream()
                .anyMatch(l -> l instanceof PrimaryInputLink<?>);
    }

    public boolean isSeparateBranch(InhibitoryActivation iAct) {
        if(isMainBranch())
            return false;

        return iAct.getBindingSignals()
                .anyMatch(bs -> bs.getOriginActivation() == mainBranch);
    }

    public boolean isMainBranch() {
        return mainBranch == null;
    }
}
