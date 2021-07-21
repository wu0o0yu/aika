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
package network.aika.neuron.steps;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;

import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class VisitorStep {

    protected Direction direction;

    public VisitorStep(Direction dir) {
        this.direction = dir;
    }

    public abstract boolean checkPropagate(Activation act, Synapse targetSynapse);

    public abstract Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir);

    public abstract boolean exists(Activation act, Synapse s, Direction dir);

    protected abstract boolean opposingNeuronMatches(Neuron<?> currentN, Neuron<?> targetN);

    protected abstract void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct);

    public abstract void getNextSteps(Activation act);

    public abstract void getNextSteps(Link l);


    public void link(Link l) {
        Direction startDir = l.getSynapse().getStartDir(direction);
        Activation startAct = startDir.invert().getActivation(l);

        getTargetSynapses(startAct, startDir)
                .map(s -> new ActVisitor(this, startAct, s, startDir, startDir))
                .forEach(v -> {
                            startAct.setMarked(true);
                            v.getTargetSynapse()
                                    .transition(v, l.getSynapse(), l);
//                            l.follow(v);
                            startAct.setMarked(false);
                        }
                );
    }

    public void link(Activation act) {
        getTargetSynapses(act, direction)
                .forEach(ts ->
                        act.follow(
                                new ActVisitor(this, act, ts, direction, INPUT)
                        )
                );
    }

    public void propagate(Activation act) {
        getTargetSynapses(act, direction)
                .filter(s ->
                        checkPropagate(act, s)
                )
                .forEach(s ->
                        s.propagate(act, direction, this, false)
                );
    }

    public void closeLoop(ActVisitor v, Activation currentAct, Activation originAct) {
        Neuron<?> currentN = currentAct.getNeuron();
        Neuron<?> targetN = direction.getNeuron(v.getTargetSynapse());
        if (!opposingNeuronMatches(currentN, targetN))
            return;

        Activation iAct = direction.getInput(originAct, currentAct);
        Activation oAct = direction.getOutput(originAct, currentAct);

        closeLoopIntern(v, iAct, oAct);
    }
}
