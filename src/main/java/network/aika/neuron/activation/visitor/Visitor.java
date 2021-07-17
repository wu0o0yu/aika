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
package network.aika.neuron.activation.visitor;

import network.aika.Thought;
import network.aika.callbacks.VisitorEvent;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.VisitorStep;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 * The Visitor is a finite state machine used to implement the linking process in the neural network.
 * It traverses the network of activations in order to find yet unlinked synapses.
 *
 * @author Lukas Molzberger
 */
public abstract class Visitor {
    protected Synapse targetSynapse;

    protected ActVisitor origin;
    private Visitor previousStep;
    protected VisitorStep visitorStep;
    protected Direction startDir;
    protected Direction currentDir;

    private int downSteps = 0;
    private int upSteps = 0;
    private int inputSteps = 0;

    protected Visitor() {}

    public Visitor(Visitor v) {
        previousStep = v;
        targetSynapse = v.targetSynapse;
        visitorStep = v.visitorStep;
        origin = v.origin;
        startDir = v.startDir;
        currentDir = v.currentDir;
        downSteps = v.downSteps;
        upSteps = v.upSteps;
        inputSteps = v.inputSteps;
    }

    public Synapse getTargetSynapse() {
        return targetSynapse;
    }

    public void switchDirection() {
        assert currentDir == INPUT;
        currentDir = currentDir.invert();

        inputSteps = 0;
    }

    public Visitor getPreviousStep() {
        return previousStep;
    }

    public Direction getStartDir() {
        return startDir;
    }

    public Direction getCurrentDir() {
        return currentDir;
    }

    public int getDownSteps() {
        return downSteps;
    }

    public int getUpSteps() {
        return upSteps;
    }

    public int getInputSteps() {
        return inputSteps;
    }

    public VisitorStep getVisitorStep() {
        return visitorStep;
    }

    public Activation getOriginAct() {
        return origin.getActivation();
    }

    public void incrementPathLength(boolean isInputStep) {
        if (currentDir == INPUT)
            downSteps++;
        else
            upSteps++;

        if(isInputStep) {
            inputSteps++;
        }
    }

    public boolean getSelfRef() {
        return downSteps == 0 || upSteps == 0;
    }

    private Thought getThought() {
        return origin.getActivation().getThought();
    }

    public void onEvent(VisitorEvent ve) {
        getThought().onVisitorEvent(this, ve);
    }

    public void onCandidateEvent(Synapse s) {
        getThought().onVisitorCandidateEvent(this, s);
    }

    public String toStringRecursive() {
        return (previousStep != null ? previousStep.toStringRecursive() : "") +
                "\n" +
                this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("DownUp:" + currentDir + ", ");
        sb.append("StartDir:" + startDir + ", ");

        sb.append("Steps: (down:" + downSteps + "), (up:" + upSteps + "), (input:" + inputSteps + ")");

        return sb.toString();
    }
}
