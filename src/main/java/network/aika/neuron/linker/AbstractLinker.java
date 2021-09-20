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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.direction.Direction;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractLinker {

    public abstract boolean checkPropagate(Activation act, Direction dir, Synapse targetSynapse);

    public abstract Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir);

    protected abstract boolean exists(Activation act, Direction dir, Synapse s);

    protected abstract void linkIntern(Activation iAct, Activation oAct, Synapse targetSynapse);

    public abstract void getNextSteps(Activation act);

    public abstract void getNextSteps(Link l);

    public void propagate(Direction dir, Activation act) {
        getTargetSynapses(act, dir)
                .filter(s ->
                        checkPropagate(act, dir, s)
                )
                .filter(s ->
                        !exists(act, dir, s)
                )
                .forEach(s ->
                        propagate(act, dir, s, false)
                );
    }

    public void propagate(Activation fromAct, Direction dir, Synapse targetSynapse, boolean isSelfRef) {
        Thought t = fromAct.getThought();
        Activation toAct =
                dir.getNeuron(targetSynapse).createActivation(t);

        t.onActivationCreationEvent(toAct, fromAct);

        getNextSteps(toAct);

        Link nl = targetSynapse.createLink(
                dir.getInput(fromAct, toAct),
                dir.getOutput(fromAct, toAct),
                isSelfRef
        );
        getNextSteps(nl);
    }

    public void link(Activation<?> fromAct, Map<PatternActivation, Byte> bindingSignals, List<Direction> dirs) {
        bindingSignals.entrySet().stream()
                .forEach(e ->
                        link(fromAct, dirs, e.getKey(), e.getValue())
                );
    }

    public void link(Activation<?> fromAct, List<Direction> dirs, PatternActivation bindingSignal, Byte scope) {
        dirs.forEach(dir ->
            getTargetSynapses(fromAct, dir)
                    .forEach(ts ->
                            link(fromAct, dir, bindingSignal, scope, ts)
                    )
        );
    }

    public void link(Activation<?> fromAct, Direction dir, PatternActivation bindingSignal, Byte scope, Synapse targetSynapse) {
        searchRelatedCandidates(scope, dir, bindingSignal, targetSynapse)
                .forEach(toAct ->
                        link(fromAct, toAct, dir, targetSynapse)
                );
    }

    private void link(Activation<?> fromAct, Activation<?> toAct, Direction dir, Synapse targetSynapse) {
        if(fromAct == toAct)
            return;

        Activation iAct = dir.getInput(fromAct, toAct);
        Activation oAct = dir.getOutput(fromAct, toAct);

        if(targetSynapse.checkCausality(iAct, oAct))
            linkIntern(iAct, oAct, targetSynapse);
    }

    private Stream<Activation> searchRelatedCandidates(Byte fromScope, Direction dir, PatternActivation bs, Synapse targetSynapse) {
        return bs.getReverseBindingSignals().entrySet().stream()
                .filter(e -> targetSynapse.transitionScope(fromScope, dir) == e.getValue())
                .map(e -> e.getKey());
    }
}
