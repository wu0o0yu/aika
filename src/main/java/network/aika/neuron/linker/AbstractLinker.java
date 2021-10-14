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
package network.aika.neuron.linker;

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractLinker {

    public abstract boolean checkPropagate(Activation act, Synapse targetSynapse);

    public abstract Neuron getPropagateTargetNeuron(Synapse targetSynapse, Activation iAct);

    public abstract Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir);

    protected abstract boolean exists(Activation act, Direction dir, Synapse s);

    protected abstract Link createLink(Activation iAct, Activation oAct, Synapse targetSynapse);

    public abstract void getNextSteps(Activation act);

    public abstract void getNextSteps(Link l);

    protected abstract boolean neuronMatches(Neuron<?, ?> currentN, Neuron<?, ?> targetN);

    public void propagate(Activation act) {
        getTargetSynapses(act, OUTPUT)
                .filter(s ->
                        checkPropagate(act, s)
                )
                .filter(s ->
                        !exists(act, OUTPUT, s)
                )
                .forEach(s ->
                        propagate(act, s, getPropagateTargetNeuron(s, act), false)
                );
    }

    public void propagate(Activation fromAct, Synapse targetSynapse, Neuron targetNeuron, boolean isSelfRef) {
        Thought t = fromAct.getThought();

        Activation toAct = targetNeuron.createActivation(t);
        t.onActivationCreationEvent(toAct, fromAct);
        getNextSteps(toAct);

        Link nl = targetSynapse.createLink(fromAct, toAct, isSelfRef);
        getNextSteps(nl);
    }

    public void link(Activation<?> fromAct, List<Direction> dirs, BindingSignal bindingSignal) {
        dirs.forEach(dir ->
            getTargetSynapses(fromAct, dir)
                    .filter(ts -> ts.allowLinking(bindingSignal.getBindingSignalAct()))
                    .forEach(ts ->
                            link(fromAct, dir, bindingSignal, ts)
                    )
        );
    }

    public void link(Activation<?> fromAct, Direction dir, BindingSignal bindingSignal, Synapse<?, ?, ?> targetSynapse) {
        targetSynapse.searchRelatedCandidates(bindingSignal, dir)
                .forEach(toAct ->
                        link(fromAct, toAct, dir, targetSynapse)
                );
    }

    private void link(Activation<?> fromAct, Activation<?> toAct, Direction dir, Synapse targetSynapse) {
        if(fromAct == toAct)
            return;

        Activation iAct = dir.getInput(fromAct, toAct);
        Activation oAct = dir.getOutput(fromAct, toAct);

        if(!targetSynapse.checkCausality(iAct, oAct))
            return;

        if(Link.linkExists(iAct, oAct))
            return;

        if (!neuronMatches(iAct.getNeuron(), targetSynapse.getInput()))
            return;

        if (!neuronMatches(oAct.getNeuron(), targetSynapse.getOutput()))
            return;

        Link nl = createLink(iAct, oAct, targetSynapse);
        if(nl != null)
            getNextSteps(nl);
    }
}
