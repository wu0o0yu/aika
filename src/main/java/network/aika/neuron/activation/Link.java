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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.steps.VisitorStep;
import network.aika.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

import static network.aika.callbacks.VisitorEvent.AFTER;
import static network.aika.callbacks.VisitorEvent.BEFORE;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public class Link extends Element<Link> {

    private static final Logger log = LoggerFactory.getLogger(Link.class);

    public static final Comparator<Link> COMPARE = Comparator.
            <Link, Activation>comparing(l -> l.output)
            .thenComparing(l -> l.input);

    private Synapse synapse;

    private Activation input;
    private Activation output;

    private boolean isSelfRef;

    private double lastIGGradient;

    public Link(Synapse s, Activation input, Activation output, boolean isSelfRef, LinkVisitor v) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;

        if(input != null)
            linkInput();

        if(output != null)
            linkOutput();

        getSynapse().updateReference(this);

        if(v != null)
            v.setLink(this);

        getThought().onLinkCreationEvent(this, v);
    }

    @Override
    public Fired getFired() {
        return output.getFired();
    }

    public static boolean synapseExists(Activation iAct, Activation oAct) {
        return Synapse.synapseExists(iAct.getNeuron(), oAct.getNeuron());
    }

    public static boolean linkExists(Activation iAct, Activation oAct) {
        return iAct.outputLinkExists(oAct);
    }

    public static boolean linkExists(Synapse s, Activation iAct, Activation oAct) {
        Link ol = oAct.getInputLink(iAct.getNeuron());
        if (ol != null) {
            assert s == ol.getSynapse();
//                    toAct = oAct.cloneToReplaceLink(s);
            log.warn("Link already exists! ");
            return true;
        }
        return false;
    }

    public static Synapse getSynapse(Activation iAct, Activation oAct) {
        return oAct.getNeuron()
                .getInputSynapse(
                        iAct.getNeuronProvider()
                );
    }

    public void count() {
        if(synapse != null)
            synapse.count(this);
    }

    public void follow(VisitorStep p) {
        follow(p,
                synapse.isRecurrent() ? OUTPUT : INPUT
        );
    }

    public void follow(VisitorStep p, Direction startDir) {
        Activation startAct = startDir.invert().getActivation(this);
        ActVisitor v = new ActVisitor(p, startAct, startDir, startDir);

        startAct.setMarked(true);
        follow(v);
        startAct.setMarked(false);
    }

    public void follow(ActVisitor v) {
        LinkVisitor lv = synapse.transition(v, this);
        if(lv == null)
            return;

        lv.onEvent(BEFORE);
        Activation toAct = v.getCurrentDir().getActivation(this);

        toAct.follow(
                new ActVisitor(lv, toAct)
        );
        lv.onEvent(AFTER);
    }

    public void computeInformationGainGradient() {
        if(isNegative())
            return; // TODO: Check under which conditions negative synapses could contribute to the cost function.

        double igGradient = 0.0;
        for(Sign si: Sign.SIGNS) {
            for (Sign so : Sign.SIGNS) {
                Reference ref = getInput().getReference();
                double s = getSynapse().getSurprisal(si, so, ref);
                s -= input.getNeuron().getSurprisal(si, ref);
                s -= output.getNeuron().getSurprisal(so, ref);

                igGradient += s * getInputValue(si) * getOutputValue(so) * output.getNorm();
            }
        }

        double igGradientDelta = igGradient - lastIGGradient;
        Utils.checkTolerance(this, igGradientDelta);

        getOutput().propagateGradientIn(igGradientDelta);
        lastIGGradient = igGradient;
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

    public boolean followAllowed(Direction dir) {
        Activation nextAct = dir.getActivation(this);
        return !isNegative() &&
                nextAct != null &&
                !nextAct.isMarked();// &&
//                isCausal();
    }

    public boolean isCausal() {
        return input == null || Fired.COMPARATOR.compare(input.getFired(), output.getFired()) <= 0;
    }

    public static double getInputValue(Sign s, Link l) {
        return l != null ? l.getInputValue(s) : 0.0;
    }

    public static double getInputValueDelta(Sign s, Link nl, Link ol) {
        return nl.getInputValue(s) - Link.getInputValue(s, ol);
    }

    public double getInputValue(Sign s) {
        return s.getValue(input != null ? input.getValue() : Double.valueOf(0.0));
    }

    public double getOutputValue(Sign s) {
        return s.getValue(output != null ? output.getValue() : Double.valueOf(0.0));
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

    public Activation getOutput() {
        return output;
    }

    public boolean isSelfRef() {
        return isSelfRef;
    }

    public void linkInput() {
        if(input != null) {
            input.outputLinks.put(
                    new OutputKey(output.getNeuronProvider(), output.getId()),
                    this
            );
        }
    }

    public void linkOutput() {
        output.inputLinks.put(input != null ? input.getNeuronProvider() : synapse.getPInput(), this);
    }

    public void unlinkInput() {
        OutputKey ok = output.getOutputKey();
        boolean successful = input.outputLinks.remove(ok, this);
        assert successful;
    }

    public void unlinkOutput() {
        boolean successful = output.inputLinks.remove(input.getNeuronProvider(), this);
        assert successful;
    }

    public void sumUpLink(double delta) {
        getOutput().updateNet(delta);
    }

    public boolean isNegative() {
        return synapse.getWeight() < 0.0;
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
        return "in:[" + input.toShortString() +
                " v:" + Utils.round(input.getValue()) + "] - s:[" + synapse.toString() + "] - out:[" + input.toShortString() + " v:" + Utils.round(input.getValue()) + "]";
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
