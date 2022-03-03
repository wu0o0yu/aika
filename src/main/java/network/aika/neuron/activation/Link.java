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
import network.aika.steps.link.LinkCounting;
import network.aika.steps.link.LinkInduction;
import network.aika.steps.link.PropagateBindingSignal;

import java.util.Comparator;

import static network.aika.fields.ConstantDoubleField.ZERO;
import static network.aika.fields.FieldUtils.*;
import static network.aika.neuron.activation.Timestamp.NOT_SET_AFTER;

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

    private BiFunction igGradient;
    private AbstractBiFunction weightedInput;
    private AbstractBiFunction backPropGradient;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

        if(input != null && output != null) {
            initWeightInput();

            if (getConfig().isTrainingEnabled() && !isNegative()) {
                igGradient = func("Information-Gain", input.net, output.net, (x1, x2) ->
                                getRelativeSurprisal(
                                        Sign.getSign(x1),
                                        Sign.getSign(x2),
                                        input.getAbsoluteRange()
                                ),
                        output.getGradientInputFields()
                );

                backPropGradient = mulUnregistered("oAct.og * s.weight", output.outputGradient, getWeightOutput());
            }
        }

        getThought().onLinkCreationEvent(this);
    }

    protected void initWeightInput() {
        weightedInput = mulUnregistered(
                "iAct.value * s.weight",
                input.getValue(),
                getWeightOutput(),
                getOutput().getNet()
        );
    }

    public void init() {
        if(getInput() != null) {
            linkInput();
            PropagateBindingSignal.add(this);

            if(getSynapse().isTemplate())
                LinkInduction.add(this);
        }

        if(getOutput() != null) {
            linkOutput();
        }

        if(getConfig().isCountingEnabled())
            LinkCounting.add(this);
    }

    public DoubleFieldOutput getInformationGainGradient() {
        return igGradient;
    }

    public DoubleFieldInput getWeightInput() {
        return synapse.getWeight();
    }

    public DoubleFieldOutput getWeightOutput() {
        return synapse.getWeight();
    }

    public DoubleFieldOutput getWeightedInput() {
        return weightedInput;
    }

    public DoubleFieldOutput getBackPropGradient() {
        return backPropGradient;
    }

    public void updateWeight(double g) {
        double weightDelta = getConfig().getLearnRate() * g;
        boolean oldWeightIsZero = synapse.isZero();

        assert !synapse.isTemplate();
        synapse.updateWeight(this, weightDelta);

        if (oldWeightIsZero && !synapse.isZero() && getInput().isFired())
            PropagateBindingSignal.add(this);
    }

    public void backPropagate() {
        if(backPropGradient != null)
            backPropGradient.triggerUpdate(1);
    }

    public void receiveWeightUpdate() {
        weightedInput.triggerUpdate(2);

        if(backPropGradient != null)
            backPropGradient.triggerUpdate(2);
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

    public DoubleFieldOutput getInputValue(Sign s) {
        return s.getValue(input != null ? input.getValue() : ZERO);
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

    public boolean isTemplate() {
        return getSynapse().isTemplate();
    }

    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }

    public boolean isCausal() {
        return input == null || isCausal(input, output);
    }

    public static boolean isCausal(Activation iAct, Activation oAct) {
        return NOT_SET_AFTER.compare(iAct.getFired(), oAct.getFired()) < 0;
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
        if(weightedInput == null)
            return;

        weightedInput.triggerUpdate(1);
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
