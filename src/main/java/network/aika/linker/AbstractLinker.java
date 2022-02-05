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
package network.aika.linker;

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.activation.Link;
import network.aika.direction.Direction;

import java.util.List;
import java.util.stream.Stream;

import static network.aika.direction.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractLinker {

    public abstract boolean checkPropagate(Activation act);

    public abstract Neuron getPropagateTargetNeuron(Synapse targetSynapse, Activation iAct);

    public abstract Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir);

    protected abstract boolean exists(Activation act, Direction dir, Synapse s);

    public abstract void addNextStepsToQueue(Activation act);

    public abstract void addNextStepsToQueue(Link l);

    public void propagate(Activation act) {
        if(!checkPropagate(act))
            return;

        getTargetSynapses(act, OUTPUT)
                .filter(s ->
                        s.allowPropagate() &&
                        !exists(act, OUTPUT, s)
                )
                .forEach(s ->
                        propagate(act, s, getPropagateTargetNeuron(s, act))
                );
    }

    public void propagate(Activation fromAct, Synapse targetSynapse, Neuron targetNeuron) {
        Thought t = fromAct.getThought();

        Activation toAct = targetNeuron.createActivation(t);
        toAct.init(targetSynapse, fromAct);

        addNextStepsToQueue(toAct);

        Link nl = targetSynapse.createLink(fromAct, toAct);
        addNextStepsToQueue(nl);
    }

    public void link(Activation<?> fromAct, List<Direction> dirs, BindingSignal bindingSignal) {
        dirs.forEach(dir ->
            getTargetSynapses(fromAct, dir)
                    .filter(ts ->
                            ts.checkBindingSignal(bindingSignal, dir)
                    )
                    .forEach(ts ->
                            link(ts, fromAct, dir, bindingSignal)
                    )
        );
    }

    public void link(Synapse targetSynapse, Activation<?> fromAct, Direction dir, BindingSignal<?> fromBindingSignal) {
        getRelatedBindingSignal(targetSynapse, fromBindingSignal)
                .filter(toBS -> fromBindingSignal != toBS)
                .filter(toBS ->
                        checkRelatedBindingSignal(targetSynapse, dir, fromBindingSignal, toBS)
                )
                .map(toBS -> toBS.getActivation())
                .filter(toAct -> fromAct != toAct)
                .forEach(toAct ->
                        link(targetSynapse, fromAct, toAct, dir)
                );
    }

    private Stream<? extends BindingSignal<?>> getRelatedBindingSignal(Synapse targetSynapse, BindingSignal<?> fromBindingSignal) {
        Activation originAct = fromBindingSignal.getOriginActivation();
        Stream<? extends BindingSignal<?>> relatedBindingSignals = originAct.getReverseBindingSignals();

        if(targetSynapse.allowLooseLinking()) {
            relatedBindingSignals = Stream.concat(
                    relatedBindingSignals,
                    originAct.getThought().getLooselyRelatedBindingSignals(fromBindingSignal, targetSynapse.getLooseLinkingRange())
            );
        }

        return relatedBindingSignals;
    }

    private boolean checkRelatedBindingSignal(Synapse targetSynapse, Direction dir, BindingSignal<?> fromBindingSignal, BindingSignal<?> toBindingSignal) {
        //           fromBindingSignal.checkRelatedBindingSignal(targetSynapse, toBindingSignal, dir)
        BindingSignal inputBS = dir.getInputBindingSignal(fromBindingSignal, toBindingSignal);
        BindingSignal outputBS = dir.getOutputBindingSignal(fromBindingSignal, toBindingSignal);
        return inputBS.checkRelatedBindingSignal(targetSynapse, outputBS);
    }

    private void link(Synapse targetSynapse, Activation<?> fromAct, Activation<?> toAct, Direction dir) {
        Activation iAct = dir.getInput(fromAct, toAct);
        Activation oAct = dir.getOutput(fromAct, toAct);

        if(!targetSynapse.checkLinkingPreConditions(iAct, oAct))
            return;

        Link nl = targetSynapse.createLink(iAct, oAct);
        if(nl != null)
            addNextStepsToQueue(nl);
    }
}
