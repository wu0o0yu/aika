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


import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.steps.VisitorStep;

import java.util.Collection;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class ActVisitor extends Visitor {

    Activation act;

    protected Collection<Scope> scopes;


    protected ActVisitor() {
    }

    public ActVisitor(VisitorStep vp, Activation act, Direction startDir, Direction downUpDir) {
        this.phase = vp;
        this.origin = this;
        this.act = act;
        this.downUpDir = downUpDir;
        this.startDir = startDir;
        this.scopes = act.getNeuron()
                .getInitialScopeTemplates(startDir)
                .stream()
                .map(s -> s.getInstance(downUpDir, null))
                .collect(Collectors.toList());
    }

    public LinkVisitor prepareNextStep(Synapse<?, ?> syn, Link l) {
        LinkVisitor nv = new LinkVisitor();
        prepareNextStep(nv);
        nv.link = l;
        nv.transitions = getScopes()
                .stream()
                .flatMap(s ->
                        syn.transition(s, downUpDir, l == null)
                ).collect(Collectors.toList());

        return nv.transitions.isEmpty() ? null : nv;
    }

    public Collection<Scope> getScopes() {
        return scopes;
    }

    public Activation getActivation() {
        return act;
    }

    public void tryToLink(Activation act) {
        if (downUpDir != OUTPUT || numSteps() < 1)
            return;

        if (act == origin.act || act.isConflicting())
            return;

        Scope ppRelatedInput = getOriginAct().getModel().getTemplates().B_RELATED_INPUT;
        if (scopes
                .stream()
                .anyMatch(s -> s.getTemplate() == ppRelatedInput)
        )
            return; // TODO

        phase.closeCycle(act, this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Origin:" + origin.act.toShortString() + ", ");
        sb.append("Current:" + act.toShortString() + ", ");

        sb.append("Scopes:" + scopes + ", ");

        sb.append(super.toString());

        return sb.toString();
    }
}
