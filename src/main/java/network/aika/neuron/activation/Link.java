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
import network.aika.fields.*;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.sign.Sign;
import network.aika.steps.activation.PostTraining;
import network.aika.steps.link.InitializeLink;
import network.aika.steps.link.PropagateBindingSignal;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.fields.ConstantField.ZERO;

/**
 *
 * @author Lukas Molzberger
 */
public class Link<S extends Synapse, I extends Activation, O extends Activation> extends Element<Link> {

    public static final Comparator<Link> COMPARE = Comparator.
            <Link, Activation<?>>comparing(l -> l.output)
            .thenComparing(l -> l.input);

    protected S synapse;

    protected final I input;
    protected final O output;

    private Field igGradient = new Field();
    private MultiSourceFieldOutput weightedInput;
    private MultiSourceFieldOutput backPropGradient;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        igGradient.setFieldListener(u ->
                output.receiveOwnGradientUpdate(u)
        );

        weightedInput = input != null ?
                new FieldMultiplication(input.getValue(), getWeightOutput()) :
                ZERO;
        backPropGradient = new FieldMultiplication(output.outputGradient, getWeightOutput());

        InitializeLink.add(this);

        getThought().onLinkCreationEvent(this);
    }

    public Field getInformationGainGradient() {
        return igGradient;
    }

    public FieldInput getWeightInput() {
        return synapse.getWeight();
    }

    public FieldOutput getWeightOutput() {
        return synapse.getWeight();
    }

    public MultiSourceFieldOutput getWeightedInput() {
        return weightedInput;
    }

    public MultiSourceFieldOutput getBackPropGradient() {
        return backPropGradient;
    }

    public void updateWeight(double g) {
        double weightDelta = getConfig().getLearnRate() * g;
        boolean oldWeightIsZero = synapse.isZero();

        assert !synapse.isTemplate();
        synapse.updateWeight(this, weightDelta);

        if (oldWeightIsZero && !synapse.isZero() && getInput().isFired())
            PropagateBindingSignal.add(this);

        PostTraining.add(getOutput());
    }

    public void backPropagate() {
        input.getInputGradient().addAndTriggerUpdate(
                backPropGradient.getUpdate(1)
        );
    }

    public void receiveWeightUpdate() {
        output.getNet().addAndTriggerUpdate(
                weightedInput.getUpdate(2)
        );
        input.getInputGradient().addAndTriggerUpdate(
                backPropGradient.getUpdate(2)
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

    public double getRelativeSurprisal(Sign si, Sign so, Range range) {
        double s = synapse.getSurprisal(this, si, so, range);
        s -= input.getNeuron().getSurprisal(input, si, range);
        s -= output.getNeuron().getSurprisal(output, so, range);

        return s;
    }

    public void updateInformationGainGradient() {
        Range range = input.getAbsoluteRange();
        assert range != null;

        Sign si = Sign.getSign(input);
        Sign so = Sign.getSign(output);

        double igGrad = getRelativeSurprisal(si, so, range) * getInputValue(si).getCurrentValue();

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

    public S getSynapse() {
        return synapse;
    }

    public void setSynapse(S synapse) {
        this.synapse = synapse;
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public boolean isSelfRef() {
        return output.isSelfRef(input);
    }

    public boolean isCausal() {
        return input == null || input.getFired().compareTo(output.getFired()) < 0;
    }

    public boolean isForward() {
        return isForward(input, output);
    }

    public boolean isTemplate() {
        return getSynapse().isTemplate();
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
        output.getNet().addAndTriggerUpdate(
                getOutputValue()
        );
    }

    protected double getOutputValue() {
        return getWeightedInput().getUpdate(1);
    }

    public void setFinalMode() {
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
        return (isTemplate() ? "Template-" : "") + getClass().getSimpleName() +
                " in:[" + getInputKeyString() + "] " +
                "--> " +
                "out:[" + getOutputKeyString() + "]";
    }

    private String getInputKeyString() {
        return (input != null ? input.toKeyString() : "id:X n:[" + synapse.getInput() + "]");
    }

    private String getOutputKeyString() {
        return (output != null ? output.toKeyString() : "id:X n:[" + synapse.getOutput() + "]");
    }
}
