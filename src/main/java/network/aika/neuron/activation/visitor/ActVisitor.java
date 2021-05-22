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
import java.util.stream.Collectors;

import static network.aika.neuron.activation.direction.Direction.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class ActVisitor extends Visitor {

    private Activation act;

    private Collection<Scope> scopes;


    public ActVisitor() {
    }

    public ActVisitor(LinkVisitor v, Activation act) {
        super(v);
        this.act = act;
        scopes = v.getTransitions().stream()
                .map(t -> v.currentDir.getToScope(t))
                .collect(Collectors.toList());
    }

    public ActVisitor(VisitorStep vp, Activation act, Direction startDir, Direction currentDir) {
        this.visitorStep = vp;
        this.origin = this;
        this.act = act;
        this.targetDir = startDir;
        this.currentDir = currentDir;

        scopes = act.getNeuron()
                .getTemplates().stream()
                .flatMap(tn ->
                        startDir.getInitialScopes(tn.getTemplateInfo()).stream()
                )
                .collect(Collectors.toList());
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
                act == origin.act ||
                act.isConflicting()
        )
            return;

        visitorStep.closeLoop(
                this,
                targetDir.getInput(act, getOriginAct()),
                targetDir.getOutput(act, getOriginAct())
        );
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Current:" + (act != null ? act.toShortString() : "X") + ", ");
        sb.append("Scopes:" + scopes + ", ");
        sb.append("Origin:" + origin.act.toShortString() + ", ");

        sb.append(super.toString());

        return sb.toString();
    }
}
