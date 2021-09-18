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
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.Scope;
import network.aika.neuron.activation.visitor.VisitorTask;

import java.util.Set;
import java.util.stream.Stream;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Linker implements VisitorTask {

    protected Direction direction;
    protected Synapse<?, ?, ?> targetSynapse;

    public Linker(Direction dir) {
        this.direction = dir;
    }

    public abstract boolean checkPropagate(Activation act, Synapse targetSynapse);

    public abstract Stream<? extends Synapse> getTargetSynapses(Activation act, Direction dir);

    public Direction getDirection() {
        return direction;
    }

    public Stream<? extends Synapse> getTemplateTargetSynapses(Activation act, Direction dir) {
        Stream<Neuron<?, ?>> s = act.getNeuron().getTemplateGroup().stream();
        return s.flatMap(dir::getSynapses);
    }

    protected abstract boolean exists(Activation act, Synapse s);

    protected abstract void closeLoopIntern(ActVisitor v, Activation iAct, Activation oAct);

    public abstract void getNextSteps(Activation act);

    public abstract void getNextSteps(Link l);

    public Synapse getTargetSynapse() {
        return targetSynapse;
    }

    @Override
    public void processTask(ActVisitor v) {
        /*
        if (
                v.getActivation() == v.getOriginAct() ||
                        v.getActivation().isConflicting()
        )
            return;
*/
        if(v.getActivation() == v.getOriginAct())
            return;

        if(v.getScope() != getEndScope(v.getStartDir(), targetSynapse))
            return;

        if(!targetSynapse.checkLoopClosure(v))
            return;

        Activation currentAct = v.getActivation();
        Activation originAct = v.getOriginAct();

        Activation iAct = direction.getInput(originAct, currentAct);
        Activation oAct = direction.getOutput(originAct, currentAct);

        closeLoopIntern(v, iAct, oAct);
    }

    @Override
    public void neuronTransition(ActVisitor av, Activation act) {
        act.getNeuron()
                .transition(av, act);
    }

    @Override
    public void synapseTransition(ActVisitor v, Synapse s, Link l) {
        if(l.isNegative())
            return;

        targetSynapse.transition(v, s, l);
    }

    public void link(Link l) {
        Activation startAct = l.getOutput();

        getTemplateTargetSynapses(startAct, direction)
                .forEach(ts ->
                        follow(l, INPUT, startAct, ts)
                );
    }

    public void link(Activation startAct) {
        getTemplateTargetSynapses(startAct, direction)
                .forEach(ts ->
                        follow(startAct, ts)
                );
    }

    private void follow(Link l, Direction startDir, Activation startAct, Synapse ts) {
        startAct.setMarked(true);
        targetSynapse = ts;
        Scope startScope = getStartScope(startDir, ts);
        ActVisitor v = new ActVisitor(null, this, startAct, startScope, startDir, startDir);
        ts.transition(v, l.getSynapse(), l);

        targetSynapse = null;
        startAct.setMarked(false);
    }

    private void follow(Activation startAct, Synapse ts) {
        targetSynapse = ts;
        Scope startScope = getStartScope(direction, ts);
        neuronTransition(
                new ActVisitor(null, this, startAct, startScope, direction, INPUT),
                startAct
        );
        targetSynapse = null;
    }

    public Scope getStartScope(Direction dir, Synapse ts) {
        return getScope(dir.invert(), ts);
    }

    public Scope getEndScope(Direction dir, Synapse ts) {
        return getScope(dir, ts);
    }

    private Scope getScope(Direction dir, Synapse ts) {
        return dir
                .getNeuron(ts)
                .getTemplateInfo()
                .getScope();
    }

    public void propagate(Activation act) {
        getTargetSynapses(act, direction)
                .filter(s ->
                        checkPropagate(act, s)
                )
                .filter(s ->
                        !exists(act, s)
                )
                .forEach(s ->
                        s.propagate(act, direction, this, false)
                );
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Direction: " + direction);
        if (targetSynapse.isTemplate())
            sb.append(targetSynapse.getTemplateInfo().getLabel() + " : ");

        sb.append(targetSynapse.toString());
        return sb.toString();
    }
}
