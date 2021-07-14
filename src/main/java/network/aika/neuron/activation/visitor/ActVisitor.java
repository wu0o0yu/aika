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
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.steps.VisitorStep;

import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class ActVisitor extends Visitor {

    private Activation act;

    public ActVisitor() {
    }

    public ActVisitor(LinkVisitor v, Activation act) {
        super(v);
        this.act = act;
    }

    public static Stream<ActVisitor> getInitialActVisitors(VisitorStep vp, Activation act, Direction startDir, Direction currentDir) {
        return act.getNeuron()
                .getTemplateGroup().stream()
                .flatMap(tn ->
                        startDir.getTargetSynapses(tn.getTemplateInfo()).stream()
                )
                .map(ts ->
                        new ActVisitor(vp, act, ts, startDir, currentDir)
                );
    }

    public ActVisitor(VisitorStep vp, Activation act, Synapse targetSynapse, Direction startDir, Direction currentDir) {
        this.visitorStep = vp;
        this.origin = this;
        this.act = act;
        this.targetSynapse = targetSynapse;
        this.startDir = startDir;
        this.targetDir = startDir;
        this.currentDir = currentDir;
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
        sb.append("TargetSynapse:" + targetSynapse + ", ");
        sb.append("Origin:" + origin.act.toShortString() + ", ");

        sb.append(super.toString());

        return sb.toString();
    }
}
