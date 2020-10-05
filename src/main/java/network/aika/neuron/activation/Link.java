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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

import static network.aika.Phase.INITIAL_LINKING;
import static network.aika.neuron.activation.Activation.TOLERANCE;
import static network.aika.neuron.activation.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Link {

    private static final Logger log = LoggerFactory.getLogger(Link.class);

    public static Comparator<Link> SORTED_BY_FIRED = Comparator
            .<Link, Fired>comparing(l -> l.getInput().getFired())
            .thenComparing(l -> l.getInput());

    private final Synapse synapse;

    private Activation input;
    private Activation output;

    private boolean isSelfRef;

    private double gradient;
    private double linkGradient;

    private double selfGradient;
    private double finalGradient;

    public Link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;
    }

    public double getInitialGradient() {
        return gradient;
    }

    public double getLinkGradient() {
        return linkGradient;
    }

    public void count() {
        if(synapse != null) {
            synapse.count(this);
        }
    }

    public void propagate() {
        if(!output.getNeuron().isInputNeuron()) {
            output.marked = true;

            Visitor v = new Visitor(output, INPUT);
            v = synapse.transition(v);
            v.follow(input);

            output.marked = false;
        }
    }

    public void computeGradient() {
        if(isNegative()) return; // TODO: Check under which conditions negative synapses could contribute to the cost function.

        double s = getSynapse().getSurprisal(
                Sign.getSign(input),
                Sign.getSign(output)
        );

        s -= input.getNeuron().getSurprisal(
                Sign.getSign(input)
        );

        s -= output.getNeuron().getSurprisal(
                Sign.getSign(output)
        );

        gradient = s * output.getActFunctionDerivative();
        linkGradient =  s * getActFunctionDerivative();
        gradient -= linkGradient;
    }

    public void removeGradientDependencies() {
        output.getInputLinks()
                .filter(l -> l.input != null && l != this && input.isConnected(l.input))
                .forEach(l -> {
                    gradient -= l.gradient;
                    linkGradient -= l.linkGradient;
                });
    }

    public double getActFunctionDerivative() {
        if(!output.getNeuron().isInitialized())
            return 0.0;

        return output.getNeuron()
                .getActivationFunction()
                .outerGrad(
                        output.getNet() - (input.getValue() * synapse.getWeight())
                );
    }

    public void computeSelfGradient(double g) {
        selfGradient = g;
        selfGradient += linkGradient;

        propagateGradient(selfGradient);
    }

    public void propagateGradient(double unpropagatedGradient) {
        if(Math.abs(unpropagatedGradient) < TOLERANCE) {
            return;
        }

        finalGradient += unpropagatedGradient;

        if(input == null) {
            return;
        }

        input.propagateGradient(
                synapse.getWeight() * unpropagatedGradient * input.getActFunctionDerivative()
        );
    }

    public void updateSynapse() {
        Thought t = output.getThought();
        Neuron on = output.getNeuron();

        boolean causal = isCausal();
        double x = getInputValue();
        double learnRate = t.getTrainingConfig().getLearnRate();

        double posWDelta = learnRate * x * finalGradient;
        double negWDelta = learnRate * (1.0 - x) * finalGradient;
        double biasDelta = learnRate * finalGradient;

        synapse.addWeight(posWDelta - negWDelta);
        on.addConjunctiveBias(negWDelta, causal);
        on.addBias(biasDelta);
    }

    public boolean follow(Direction dir) {
        return !isNegative() &&
                getActivation(dir) != null &&
                !getActivation(dir).marked &&
                isCausal();
    }

    public boolean isCausal() {
        return input == null || input.getFired().compareTo(output.getFired()) <= 0;
    }

    public double getInputValue() {
        return input != null ? input.getValue() : 0.0;
    }

    public static Link link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        if (output.getPhase() != INITIAL_LINKING && output.isFinal()) {
            output = output.getModifiable(s);
        }

        return output.addLink(s, input, isSelfRef);
    }

    public Synapse getSynapse() {
        return synapse;
    }

    public Activation getInput() {
        return input;
    }

    public Activation getOutput() {
        return output;
    }

    public Activation getActivation(Direction dir) {
        assert dir != null;
        return dir == INPUT ? input : output;
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    public boolean isSelfRef() {
        return isSelfRef;
    }

    public void link() {
        if(input != null) {
/*            if(synapse.isPropagate()) {
                SortedMap<Activation, Link> outLinks = input.getOutputLinks(synapse);
                if(!outLinks.isEmpty()) {
                    Activation oAct = outLinks.firstKey();
//                    assert oAct.getId() == output.getId();
                }
            }
*/
            input.outputLinks.put(output, this);
        }
        output.inputLinks.put(synapse.getPInput(), this);
    }

    public void unlink() {
        input.outputLinks.remove(output);
    }

    public String toString() {
        return "L " + synapse.getClass().getSimpleName() + ": " + getIdString() + " --> " + output.getIdString();
    }

    public String getIdString() {
        return (input != null ? input.getIdString() : "X:" + synapse.getInput());
    }

    public String gradientsToString() {
        return "   " + getIdString() +
                " x:" + Utils.round(getInputValue()) +
                " w:" + Utils.round(getSynapse().getWeight());
    }
}
