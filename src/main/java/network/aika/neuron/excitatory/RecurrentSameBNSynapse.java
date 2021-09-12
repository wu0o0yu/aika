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
package network.aika.neuron.excitatory;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.activation.visitor.Scope;

import static network.aika.neuron.activation.Activation.INCOMING;
import static network.aika.neuron.activation.Activation.OWN;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentSameBNSynapse extends SameBNSynapse<PatternNeuron> {


    @Override
    public void propagateGradient(Link l, double[] gradient) {
        l.propagateGradient(gradient[INCOMING] + gradient[OWN]);
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        return v.getSelfRef();
    }

    public Direction getStartDir(Direction dir) {
        return isRecurrent() ? dir.invert() : dir;
    }

    public void transition(ActVisitor v, Synapse s, Link l) {
        s.patternTransitionLoop(v, l);
    }

    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getStartDir() == v.getCurrentDir())
            return;

        if(v.getScope() != Scope.INPUT)
            return;

        l.follow(v);
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getScope() != Scope.INPUT)
            return;

        l.follow(v);
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        if (dir == INPUT && act.getNeuron().isInputNeuron())
            return false;

        return dir != OUTPUT;
    }

    @Override
    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);

        getOutput().addAssumedWeights(weightDelta);
    }
}
