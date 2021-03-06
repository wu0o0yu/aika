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
import network.aika.utils.Utils;
import network.aika.neuron.sign.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.link.SumUpLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.Activation.TOLERANCE;
import static network.aika.neuron.activation.Element.RoundType.WEIGHT;
import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public class Link extends Element {

    private static final Logger log = LoggerFactory.getLogger(Link.class);

    private Synapse synapse;

    private Activation input;
    private Activation output;

    private boolean isSelfRef;

    private double lastIGGradient;
    private double gradient;

    public Link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;
    }

    public Link(Link oldLink, Synapse s, Activation input, Activation output, boolean isSelfRef) {
        this(s, input, output, isSelfRef);

        Thought t = getThought();

        t.onLinkCreationEvent(this);

        linkInput();
        linkOutput();

        getSynapse().updateReference(this);

        double w = getSynapse().getWeight();

        if (w <= 0.0 && isSelfRef())
            return;

        t.addToQueue(
                this,
                new SumUpLink(w * (getInputValue(POS) - getInputValue(POS, oldLink)))
        );
    }

    public double getGradient() {
        return gradient;
    }

    public static boolean synapseExists(Activation iAct, Activation oAct) {
        return Synapse.synapseExists(iAct.getNeuron(), oAct.getNeuron());
    }

    public static boolean linkExists(Activation iAct, Activation oAct) {
        return iAct.outputLinkExists(oAct);
    }

    public static boolean linkExists(Synapse s, Activation oAct) {
        Link ol = oAct.getInputLink(s);
        if (ol != null) {
//                    toAct = oAct.cloneToReplaceLink(s);
            log.warn("Link already exists! ");
            return false;
        }
        return true;
    }

    public static Synapse getSynapse(Activation iAct, Activation oAct) {
        return oAct.getNeuron()
                .getInputSynapse(
                        iAct.getNeuronProvider()
                );
    }

    public void count() {
        if(synapse != null) {
            synapse.count(this);
        }
    }

    public void follow(VisitorPhase p) {
        follow(p, synapse.isRecurrent() ? OUTPUT : INPUT);
    }

    public void follow(VisitorPhase p, Direction dir) {
        output.setMarked(true);

        Visitor v = new Visitor(p, output, dir, INPUT, ACT);
        Visitor nv = synapse.transition(v, this);
        synapse.follow(input, nv);

        output.setMarked(false);
    }

    public void follow(Visitor v) {
        Visitor nv = synapse.transition(v, this);
        if(nv != null) {
            synapse.follow(
                    v.downUpDir.getActivation(this),
                    nv
            );
        }
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
        if(Math.abs(igGradientDelta) >= TOLERANCE) {
            getOutput().propagateGradient(igGradientDelta);
            lastIGGradient = igGradient;
        }
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
        gradient += g;

        if(input == null)
            return;

        input.propagateGradient(
                synapse.getWeight() *
                        g
        );
    }

    public boolean gradientIsZero() {
            return Math.abs(gradient) < TOLERANCE;
    }

    public boolean followAllowed(Direction dir) {
        Activation nextAct = dir.getActivation(this);
        return !isNegative() &&
                nextAct != null &&
                !nextAct.isMarked();// &&
//                isCausal();
    }

    public boolean isCausal() {
        return input == null || input.getFired().compareTo(output.getFired()) <= 0;
    }

    public static double getInputValue(Sign s, Link l) {
        return l != null ? l.getInputValue(s) : 0.0;
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

    public double getAndResetGradient() {
        double oldGradient = gradient;
        gradient = 0.0;
        return oldGradient;
    }

    public void linkInput() {
        if(input != null) {
/*            if(synapse.isPropagate()) {
                SortedMap<Activation, Link> outLinks = input.getOutputLinks(synapse);
                if(!outLinks.isEmpty()) {
                    Activation oAct = outLinks.firstKey();
//                    assert oAct.getId() == output.getId();
                }
            }
*/
            input.outputLinks.put(
                    new OutputKey(output.getNeuronProvider(), output.getId()),
                    this
            );
        }
    }

    public void linkOutput() {
        output.inputLinks.put(input.getNeuronProvider(), this);
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

    public void addNextLinkPhases(VisitorPhase p) {
        getThought().addToQueue(
                this,
                p.getNextLinkPhases()
        );
    }

    public void sumUpLink(double delta) {
        Activation oAct = getOutput();
        oAct.addToSum(delta);
        oAct.updateRound(
                RoundType.ACT,
                getRound(WEIGHT),
                getSynapse().isRecurrent() && !oAct.getNeuron().isInputNeuron()
        );
    }

    public boolean isNegative() {
        return synapse.getWeight() < 0.0;
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    @Override
    public int compareTo(Element ge) {
        Link l = ((Link) ge);
        int r = output.compareTo(l.output);
        if(r != 0) return r;

        return input.compareTo(l.input);
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
