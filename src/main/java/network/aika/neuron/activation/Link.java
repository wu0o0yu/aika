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
import network.aika.neuron.activation.fields.Field;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.link.AddLink;
import network.aika.utils.Utils;

import java.util.Comparator;

import static network.aika.neuron.activation.Timestamp.NOT_SET;
import static network.aika.neuron.sign.Sign.POS;

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

    private Field igGradient = new Field(getOutput().receiveOwnGradientUpdate(igGradient.getUpdate()));

    public Link(Synapse s, Activation input, A output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;

        AddLink.add(this);

        getThought().onLinkCreationEvent(this);
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

    private double computeGradient(Sign si, Sign so) {
        Range range = input.getAbsoluteRange();
        assert range != null;

        double s = getSynapse().getSurprisal(si, so, range);
        s -= input.getNeuron().getSurprisal(si, range);
        s -= output.getNeuron().getSurprisal(so, range);

        return s * getInputValue(si) * getOutputValue(so);
    }

    public void updateInformationGainGradient() {
        if(isNegative())
            return; // TODO: Check under which conditions negative synapses could contribute to the cost function.

        double igGrad = 0.0;
        for(Sign si: Sign.SIGNS) {
            for (Sign so : Sign.SIGNS) {
                igGrad += computeGradient(si, so);
            }
        }

        igGradient.add(igGrad);
    }


    public void propagateIGGradient() {
        igGradient.propagateUpdate(u ->
                getOutput().receiveOwnGradientUpdate(u)
        );
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

    public void propagateGradient(double g) {
        if(input == null)
            return;

        input.propagateGradientIn(
                synapse.getWeight() *
                        g
        );
    }

    public static double getInputValue(Sign s, Link l) {
        return l != null ? l.getInputValue(s) : 0.0;
    }

    public static double getInputValueDelta(Sign s, Link nl, Link ol) {
        return nl.getInputValue(s) - Link.getInputValue(s, ol);
    }

    public double getInputValue(Sign s) {
        return s.getValue(input != null ? input.getValue() : 0.0);
    }

    public double getOutputValue(Sign s) {
        return s.getValue(output != null ? output.getValue() : 0.0);
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

    public void updateNetByWeight(double weightDelta) {
        synapse.updateOutputNet(this, getInputValue(POS) * weightDelta);
    }

    public void updateNetByInputValue(double inputValueDelta) {
        synapse.updateOutputNet(this, inputValueDelta * synapse.getWeight());
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
        return "in:[" + input.toShortString() + " v:" + Utils.round(input.getValue()) + "] - " +
                "s:[" + synapse.toString() + "] - " +
                "out:[" + input.toShortString() + " v:" + Utils.round(input.getValue()) + "]";
    }

    public String getIdString() {
        return (input != null ? input.toShortString() : "X:" + synapse.getInput());
    }

    public String gradientsToString() {
        return "   " + getIdString() +
                " x:" + Utils.round(getInputValue(POS)) +
                " w:" + Utils.round(getSynapse().getWeight());
    }

    public String toShortString() {
        return toString();
    }

}
