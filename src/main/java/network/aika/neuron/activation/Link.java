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

import network.aika.Config;
import network.aika.Thought;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.fields.ConstantField;
import network.aika.neuron.activation.fields.Field;
import network.aika.neuron.activation.fields.FieldMultiplication;
import network.aika.neuron.activation.fields.FieldOutput;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.activation.PostTraining;
import network.aika.neuron.steps.link.AddLink;
import network.aika.neuron.steps.link.PropagateBindingSignal;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.activation.fields.ConstantField.ZERO;
import static network.aika.neuron.sign.Sign.POS;
import static network.aika.neuron.sign.Sign.SIGNS;

/**
 *
 * @author Lukas Molzberger
 */
public class Link<A extends Activation> extends Element<Link> {

    public static final Comparator<Link> COMPARE = Comparator.
            <Link, Activation<?>>comparing(l -> l.output)
            .thenComparing(l -> l.input);

    private Synapse synapse;

    private final Activation input;
    private final A output;

    private final boolean isSelfRef;

    private Field igGradient = new Field();
    private FieldOutput weightedInput;
    private FieldOutput backPropGradient;

    public Link(Synapse s, Activation input, A output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;

        initInformationGain();

        weightedInput = input != null ?
                new FieldMultiplication(input.getValue(), synapse.getWeight()) :
                ZERO;
        backPropGradient = new FieldMultiplication(output.outputGradient, synapse.getWeight());

        AddLink.add(this);

        getThought().onLinkCreationEvent(this);
    }

    private void initInformationGain() {
        igGradient.setFieldListener(u ->
                output.receiveOwnGradientUpdate(u)
        );
    }

    public Field getInformationGainGradient() {
        return igGradient;
    }

    public FieldOutput getWeightedInput() {
        return weightedInput;
    }

    public FieldOutput getBackPropGradient() {
        return backPropGradient;
    }

    public void updateWeight(double g) {
        double weightDelta = getConfig().getLearnRate() * g;
        boolean oldWeightIsZero = synapse.isZero();

        assert !synapse.isTemplate();
        synapse.updateSynapse(this, weightDelta);

        if (oldWeightIsZero && !synapse.isZero() && getInput().isFired())
            PropagateBindingSignal.add(this);

        PostTraining.add(getOutput());
    }

    public void backPropagate() {
        input.getInputGradient().addAndTriggerUpdate(
                backPropGradient.getUpdate(1, true)
        );
    }

    public void receiveWeightUpdate() {
        output.getNet().addAndTriggerUpdate(
                weightedInput.getUpdate(2, true)
        );
        input.getInputGradient().addAndTriggerUpdate(
                backPropGradient.getUpdate(2, true)
        );
    }

    @Override
    public Timestamp getFired() {
        return output.getFired();
    }

    public static boolean linkExists(Activation iAct, Activation oAct) {
        Link existingLink = oAct.getInputLink(iAct.getNeuron());
        return existingLink != null && existingLink.getInput() == iAct;
    }

    public static boolean templateLinkExists(Synapse ts, Activation iAct, Activation oAct) {
        Link l = oAct.getInputLink(iAct.getNeuron());
        if(l == null)
            return false;
        return l.getSynapse().isOfTemplate(ts);
    }

    public void updateInformationGainGradient() {
        Range range = input.getAbsoluteRange();
        assert range != null;

        double igGrad = 0.0;
        for(Sign si: Sign.SIGNS)
            for (Sign so : Sign.SIGNS)
                igGrad += synapse.getRelativeSurprisal(si, so, range) * getInputValue(si).getCurrentValue();

        igGradient.setAndTriggerUpdate(igGrad);
    }

/*
    public void removeGradientDependencies() {
        output.getInputLinks()
                .filter(l -> l.input != null && l != this && input.isConnected(l.input))
                .forEach(l -> {
                    outputGradient -= l.outputGradient;
                    outputGradient = Math.min(0.0, outputGradient); // TODO: check if that's correct.
                });
    }
*/

    public FieldOutput getInputValue(Sign s) {
        return s.getValue(input != null ? input.getValue() : ZERO);
    }

    public FieldOutput getOutputValue(Sign s) {
        return s.getValue(output != null ? output.getValue() : ZERO);
    }

    public Synapse getSynapse() {
        return synapse;
    }

    public void setSynapse(Synapse synapse) {
        this.synapse = synapse;
    }

    public Activation getInput() {
        return input;
    }

    public A getOutput() {
        return output;
    }

    public boolean isSelfRef() {
        return isSelfRef;
    }

    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }

    public boolean isCausal() {
        return input == null || input.getFired().compareTo(output.getFired()) < 0;
    }

    public boolean isForward() {
        return isForward(input, output);
    }

    public static boolean isForward(Activation iAct, Activation oAct) {
        if(!iAct.isFired())
            return false;

        return oAct.getFired() == NOT_SET || iAct.getFired().compareTo(oAct.getFired()) < 0;
    }

    public void linkInput() {
        if(input == null)
            return;

        input.outputLinks.put(
                new OutputKey(output.getNeuronProvider(), output.getId()),
                this
        );
    }

    public void propagateValue() {
        if(!synapse.propagateValue(this))
            return;

        output.getNet().addAndTriggerUpdate(
                weightedInput.getUpdate(1, true)
        );
    }

    public void linkOutput() {
        output.inputLinks.put(input != null ? input.getNeuronProvider() : synapse.getPInput(), this);
    }

    public void unlinkInput() {
        OutputKey ok = output.getOutputKey();
        input.outputLinks.remove(ok, this);
    }

    public void unlinkOutput() {
        output.inputLinks.remove(input.getNeuronProvider(), this);
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    @Override
    public Config getConfig() {
        return output.getConfig();
    }

    @Override
    public int compareTo(Link l) {
        return COMPARE.compare(this, l);
    }

    public String toString() {
        return synapse.getClass().getSimpleName() +
                ": " + getIdString() +
                " --> " + output.toShortString();
    }

    public String toDetailedString() {
        return "in:[" + input.toShortString() + " v:" + input.getValue() + "] - " +
                "s:[" + synapse.toString() + "] - " +
                "out:[" + input.toShortString() + " v:" + input.getValue() + "]";
    }

    public String getIdString() {
        return (input != null ? input.toShortString() : "X:" + synapse.getInput());
    }

    public String gradientsToString() {
        return "   " + getIdString() +
                " x:" + getInputValue(POS) +
                " w:" + getSynapse().getWeight();
    }

    public String toShortString() {
        return toString();
    }
}
