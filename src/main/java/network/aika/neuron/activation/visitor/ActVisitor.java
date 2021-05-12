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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.steps.VisitorStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class ActVisitor extends Visitor {

    Activation act;

    private Collection<Scope> scopes;


    public ActVisitor() {
    }

    public ActVisitor(LinkVisitor v, Activation act) {
        super(v);
        this.act = act;
        scopes = new ArrayList<>();
        visitedScopes = new TreeSet<>(v.visitedScopes);

        v.getTransitions()
                .forEach(t -> {
                    Scope fromScope = v.downUpDir.getFromScope(t.getTemplate());
                    Scope toScope = v.downUpDir.getToScope(t.getTemplate());
                    if(fromScope == toScope || !v.visitedScopes.contains(toScope))
                        scopes.add(toScope.getInstance(v.downUpDir, t));
                });
    }

    public ActVisitor(VisitorStep vp, Activation act, Direction startDir, Direction downUpDir) {
        this.phase = vp;
        this.origin = this;
        this.act = act;
        this.startDir = startDir;
        this.downUpDir = downUpDir;
        this.scopes = new ArrayList<>();
        this.visitedScopes = new TreeSet<>();

        act.getNeuron()
                .getInitialScopeTemplates(startDir)
                .forEach(s -> {
                    visitedScopes.add(s);
                    scopes.add(s.getInstance(downUpDir, null));
                });
    }

    public boolean follow() {
        return !scopes.isEmpty();
    }

    public Collection<Scope> getScopes() {
        return scopes;
    }

    public Activation getActivation() {
        return act;
    }

    public void tryToLink(Activation act) {
        if (
                downUpDir == INPUT ||
                numSteps() < 1 ||
                act == origin.act ||
                act.isConflicting()
        )
            return;

        phase.closeLoop(act, this);
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
