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
import network.aika.Utils;
import network.aika.neuron.Neuron;
import network.aika.neuron.Sign;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.VisitorPhase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.Activation.TOLERANCE;
import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.Visitor.Transition.LINK;
import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Link extends QueueEntry<LinkPhase> {

    private static final Logger log = LoggerFactory.getLogger(Link.class);

    private Synapse synapse;

    private Activation input;
    private Activation output;

    private boolean isSelfRef;

    private double gradient;

    public Link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;
    }


    @Override
    public void onProcessEvent() {
        getThought().onLinkProcessedEvent(this);
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
        output.setMarked(true);

        Visitor nv = synapse.transition(
                new Visitor(p, output, INPUT, ACT),
                this
        );
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

    public void computeSelfGradient() {
        if(isNegative()) return; // TODO: Check under which conditions negative synapses could contribute to the cost function.

        double s = 0.0;

        s -= input.getNeuron().getSurprisal(
                Sign.getSign(input)
        );

        s -= output.getNeuron().getSurprisal(
                Sign.getSign(output)
        );

        s += getSynapse().getSurprisal(
                Sign.getSign(input),
                Sign.getSign(output)
        );

        double f = s * getInputValue() * output.getNorm();

        double offsetGradient =  f * getActFunctionDerivative();
        double outputGradient = (f * output.getActFunctionDerivative()) - offsetGradient;

        gradient += offsetGradient;
        getOutput().propagateGradient(outputGradient);
    }

    private double getActFunctionDerivative() {
        if(output.getNet() == 0.0) {
            return 0.0;
        }

        return output.getNeuron()
                .getActivationFunction()
                .outerGrad(
                        output.getNet() - (input.getValue() * synapse.getWeight())
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
        gradient += g;

        if(input == null) {
            return;
        }

        input.propagateGradient(
                synapse.getWeight() *
                        g *
                        input.getActFunctionDerivative()
        );
    }

    public boolean gradientIsZero() {
        return Math.abs(gradient) < TOLERANCE;
    }

    public void updateSynapse() {
        if(gradientIsZero())
            return;

        Neuron on = output.getNeuron();

        boolean causal = isCausal();
        double x = getInputValue();
        double learnRate = on.getConfig().getLearnRate();

        double posWDelta = learnRate * x * gradient;
        double negWDelta = learnRate * (1.0 - x) * gradient;
        double biasDelta = learnRate * gradient;

        gradient = 0.0;

        synapse.addWeight(posWDelta - negWDelta);
        on.addConjunctiveBias(negWDelta, !causal);
        on.addBias(biasDelta);

        double finalBias = on.getBias(true);
        if(finalBias > 0.0) {
            on.addConjunctiveBias(-finalBias, false);
        }
    }

    public boolean follow(Direction dir) {
        Activation nextAct = dir.getActivation(this);
        return !isNegative() &&
                nextAct != null &&
                !nextAct.isMarked() &&
                isCausal();
    }

    public boolean isCausal() {
        return input == null || input.getFired().compareTo(output.getFired()) <= 0;
    }

    public double getInputValue() {
        return input != null ? input.getValue() : 0.0;
    }

    public static Link link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        if (!ActivationPhase.isFinal(output.getPhase()) && output.isFinal()) {
            output = output.getModifiable(s);
        }

        return output.addLink(s, input, isSelfRef);
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
        addToQueue(
                p.getNextLinkPhases(output.getConfig())
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public boolean isNegative() {
        return synapse.getWeight() < 0.0;
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    @Override
    protected int innerCompareTo(QueueEntry<LinkPhase> qe) {
        Link l = ((Link) qe);
        int r = output.innerCompareTo(l.output);
        if(r != 0) return r;

        return input.innerCompareTo(l.input);
    }

    public String toString() {
        return synapse.getClass().getSimpleName() +
                ": " + getIdString() +
                " --> " + output.getShortString() +
                Phase.toString(getPhase());
    }

    public String toDetailedString() {
        return "in:[" + input.getShortString() +
                " v:" + Utils.round(input.getValue()) + "] - s:[" + synapse.toString() + "] - out:[" + input.getShortString() + " v:" + Utils.round(input.getValue()) + "]";
    }

    public String getIdString() {
        return (input != null ? input.getShortString() : "X:" + synapse.getInput());
    }

    public String gradientsToString() {
        return "   " + getIdString() +
                " x:" + Utils.round(getInputValue()) +
                " w:" + Utils.round(getSynapse().getWeight());
    }
}
