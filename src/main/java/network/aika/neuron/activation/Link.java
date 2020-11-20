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

import static network.aika.neuron.phase.Phase.FINAL_LINKING;
import static network.aika.neuron.phase.Phase.INITIAL_LINKING;
import static network.aika.neuron.activation.Activation.TOLERANCE;
import static network.aika.neuron.activation.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Link {

    private static final Logger log = LoggerFactory.getLogger(Link.class);

    private final Synapse synapse;

    private Activation input;
    private Activation output;

    private boolean isSelfRef;

    private double outputGradient;
    private double offsetGradient;
    private double selfGradient;
    private double finalGradient;

    public Link(Synapse s, Activation input, Activation output, boolean isSelfRef) {
        this.synapse = s;
        this.input = input;
        this.output = output;
        this.isSelfRef = isSelfRef;
    }

    public double getFinalGradient() {
        return finalGradient;
    }

    public double getOffsetGradient() {
        return offsetGradient;
    }

    public void count() {
        if(synapse != null) {
            synapse.count(this);
        }
    }

    public void propagate() {
        output.setMarked(true);

        Visitor v = new Visitor(output, INPUT);
        synapse.transition(v, input);

        output.setMarked(false);
    }

    public void computeOutputGradient() {
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

        offsetGradient =  f * getActFunctionDerivative();
        outputGradient = (f * output.getActFunctionDerivative()) - offsetGradient;
    }

    private double getActFunctionDerivative() {
        if(!output.getNeuron().isInitialized()) {
            return 0.0;
        }

        return output.getNeuron()
                .getActivationFunction()
                .outerGrad(
                        output.getNet() - (input.getValue() * synapse.getWeight())
                );
    }

    public void removeGradientDependencies() {
        output.getInputLinks()
                .filter(l -> l.input != null && l != this && input.isConnected(l.input))
                .forEach(l -> {
                    outputGradient -= l.outputGradient;
                    outputGradient = Math.min(0.0, outputGradient); // TODO: check if that's correct.
                });
    }

    public double getOutputGradient() {
        return outputGradient;
    }

    public void updateSelfGradient() {
        selfGradient = getOutput().getSelfGradient();
        selfGradient += offsetGradient;

        finalGradient += selfGradient;
    }

    public void updateAndPropagateSelfGradient() {
        updateSelfGradient();
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
        if(Math.abs(finalGradient) < TOLERANCE) {
            return;
        }

        Thought t = output.getThought();
        Neuron on = output.getNeuron();

        boolean causal = isCausal();
        double x = getInputValue();
        double learnRate = t.getConfig().getLearnRate();

        double posWDelta = learnRate * x * finalGradient;
        double negWDelta = learnRate * (1.0 - x) * finalGradient;
        double biasDelta = learnRate * finalGradient;

        synapse.addWeight(posWDelta - negWDelta);
        on.addConjunctiveBias(negWDelta, !causal);
        on.addBias(biasDelta);

        double finalBias = on.getBias(FINAL_LINKING);
        if(finalBias > 0.0) {
            on.addConjunctiveBias(-finalBias, false);
        }
    }

    public boolean follow(Direction dir) {
        Activation nextAct = getActivation(dir);
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
        if (output.getPhase() != INITIAL_LINKING && output.isFinal()) {
            output = output.getModifiable(s);
        }

        return output.addLink(s, input, isSelfRef);
    }

    public void addToQueue() {
        if (synapse.getWeight() > 0.0) {
            output.getThought().addLinkToQueue(this);
        }
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
        output.inputLinks.put(synapse.getPInput(), this);
    }

    public void unlink() {
        OutputKey ok = output.getOutputKey();
        input.outputLinks.remove(ok, this);
    }

    public String toString() {
        return "l " + synapse.getClass().getSimpleName() + ": " + getIdString() + " --> " + output.getShortString();
    }

    public String toDetailedString() {
        return "l in:[" + input.getShortString() +
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

    public boolean isNegative() {
        return synapse.getWeight() < 0.0;
    }
}
