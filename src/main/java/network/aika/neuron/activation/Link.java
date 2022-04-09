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
import network.aika.fields.ThresholdOperator;
import network.aika.fields.AbstractBiFunction;
import network.aika.fields.BiFunction;
import network.aika.fields.FieldOutput;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.sign.Sign;
import network.aika.steps.link.Cleanup;
import network.aika.steps.link.LinkCounting;
import network.aika.steps.link.PropagateBindingSignal;

import java.util.Comparator;

import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.Fields.*;
import static network.aika.neuron.activation.Timestamp.NOT_SET;
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

    private ThresholdOperator onTransparent;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

        if(input != null && output != null) {
            onTransparent = threshold(
                    "onTransparent",
                    0.0,
                    mul("isFired * weight",
                            synapse.getWeight(),
                            input.isFired
                    )
            );
            onTransparent.addEventListener(() ->
                    PropagateBindingSignal.add(this)
            );

            initWeightInput();

            output.getIsFinal().addEventListener(() -> {
                if (getConfig().isTrainingEnabled() && !isNegative() && getSynapse().isAllowTraining()) {
                    initGradients(input, output);
                }
            });
        }

        getThought().onLinkCreationEvent(this);
    }

    private void initGradients(I input, O output) {
        if(isTemplate())
            induce();

        igGradient = func("Information-Gain", input.net, output.net, (x1, x2) ->
                        getRelativeSurprisal(
                                Sign.getSign(x1),
                                Sign.getSign(x2),
                                input.getAbsoluteRange()
                        ),
                output.ownInputGradient
        );

        backPropGradient = mul(
                "oAct.ownOutputGradient * s.weight",
                output.ownOutputGradient,
                synapse.getWeight(),
                input.backpropInputGradient
        );

        synapse.initWeightUpdate(this);
    }

    protected void initWeightInput() {
        weightedInput = mul(
                "iAct.value * s.weight",
                input.getValue(),
                synapse.getWeight(),
                getOutput().getNet()
        );
    }

    public void init() {
        if(getInput() != null) {
            linkInput();
            PropagateBindingSignal.add(this);
        }

        if(getOutput() != null)
            linkOutput();

        if(getConfig().isCountingEnabled())
            LinkCounting.add(this);
    }

    public FieldOutput getInformationGainGradient() {
        return igGradient;
    }

    public AbstractBiFunction getWeightedInput() {
        return weightedInput;
    }

    public FieldOutput getBackPropGradient() {
        return backPropGradient;
    }

    @Override
    public Timestamp getFired() {
        return isCausal() ? input.getFired() : output.getFired();
    }

    public static boolean templateLinkExists(Synapse ts, Activation iAct, Activation oAct) {
        Link l = oAct.getInputLink(iAct.getNeuron());
        if(l == null)
            return false;
        return l.getSynapse().isOfTemplate(ts);
    }

    public double getRelativeSurprisal(Sign si, Sign so, Range range) {
        double s = synapse.getSurprisal(si, so, range, true);
        s -= input.getNeuron().getSurprisal(si, range, true);
        s -= output.getNeuron().getSurprisal(so, range, true);
        return s;
    }

    public FieldOutput getInputValue(Sign s) {
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
        return oAct.getFired() == NOT_SET || NOT_SET_AFTER.compare(iAct.getFired(), oAct.getFired()) < 0;
    }

    public void induce() {
        assert isTemplate();

        synapse = (S) synapse
                .instantiateTemplate(
                        input.getNeuron(),
                        output.getNeuron()
                );

        synapse.linkOutput();

        Cleanup.add(this);
    }

    public void linkInput() {
        if(input == null)
            return;

        input.outputLinks.put(
                new OutputKey(output.getNeuronProvider(), output.getId()),
                this
        );
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
