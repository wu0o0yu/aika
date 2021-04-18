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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.scopes.PPRelatedInput;
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.activation.scopes.ScopeEntry;
import network.aika.neuron.steps.VisitorStep;

import java.util.Set;

import static network.aika.neuron.activation.direction.Direction.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Visitor {
    private Visitor origin;
    private Activation act; // Just debug code
    public Link link; // Just debug code
    private Transition transition; // Just debug code
    private Visitor previousStep;
    private VisitorStep phase;

    public enum Transition {  // Just debug code
        ACT,
        LINK
    }

    public Direction downUpDir;
    public Direction startDir;

    private Set<ScopeEntry> scopes;

    public int downSteps = 0;
    public int upSteps = 0;

    private Visitor() {}

    public Visitor(VisitorStep vp, Activation act, Direction startDir, Direction downUpDir, Transition t) {
        this.phase = vp;
        this.origin = this;
        this.act = act;
        this.transition = t;
        this.downUpDir = downUpDir;
        this.startDir = startDir;
        this.scopes = act.getNeuron().getInitialScopes(startDir);
    }

    public Visitor prepareNextStep(Activation currentAct, Link currentLink, Set<ScopeEntry> scopes, Transition t) {
        if(scopes.isEmpty())
            return null;

        Visitor nv = new Visitor();
        nv.phase = phase;
        nv.act = currentAct;
        nv.link = currentLink;
        nv.transition = t;
        nv.previousStep = this;
        nv.origin = origin;
        nv.downUpDir = downUpDir;
        nv.startDir = startDir;
        nv.upSteps = upSteps;
        nv.downSteps = downSteps;
        nv.scopes = scopes;

        return nv;
    }

    public Visitor getOrigin() {
        return origin;
    }

    public Activation getAct() {
        return act;
    }

    public Link getLink() {
        return link;
    }

    public Transition getTransition() {
        return transition;
    }

    public Visitor getPreviousStep() {
        return previousStep;
    }

    public Direction getDownUpDir() {
        return downUpDir;
    }

    public Direction getStartDir() {
        return startDir;
    }

    public int getDownSteps() {
        return downSteps;
    }

    public int getUpSteps() {
        return upSteps;
    }

    public Set<ScopeEntry> getScopes() {
        return scopes;
    }

    public VisitorStep getPhase() {
        return phase;
    }

    public Activation getOriginAct() {
        return origin.act;
    }

    public void incrementPathLength() {
        if (downUpDir == INPUT) {
            this.downSteps++;
        } else {
            this.upSteps++;
        }
    }

    public boolean isClosedCycle() {
        return scopes.stream()
                        .anyMatch(s ->
                                origin.scopes.contains(s)
                        );
    }

    public boolean getSelfRef() {
        return downSteps == 0 || upSteps == 0;
    }

    public int numSteps() {
        return downSteps + upSteps;
    }

    public void tryToLink(Activation act) {
        if (downUpDir != OUTPUT || numSteps() < 1)
            return;

        if (act == origin.act || act.isConflicting())
            return; // <--

        if (scopes
                .stream()
                .anyMatch(s -> s.getScope() instanceof PPRelatedInput)
        )
            return; // TODO

        phase.closeCycle(act, this);
    }

    private Thought getThought() {
        return origin.act.getThought();
    }

    public void onEvent(boolean dir) {
        getThought().onVisitorEvent(this, dir);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if(previousStep != null) {
            sb.append(previousStep + "\n");
        }

        sb.append("Origin:" + origin.act.toShortString() + ", ");

        if(act != null) {
            sb.append("Current:" + act.toShortString() + ", ");
        } else if(link != null) {
            sb.append("Current:" + link.toString() + ", ");
        }

        sb.append("DownUp:" + downUpDir + ", ");
        sb.append("StartDir:" + startDir + ", ");

        sb.append("Scopes:" + scopes + ", ");
        sb.append("DownSteps:" + downSteps + ", ");
        sb.append("UpSteps:" + upSteps + "");

        return sb.toString();
    }
}
