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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.*;
import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.Visitor.Transition.LINK;

/**
 *
 * @author Lukas Molzberger
 */
public class Visitor {
    public Activation origin;
    public Activation current; // Just debug code
    public Transition transition; // Just debug code
    public Visitor previousStep;

    public enum Transition {  // Just debug code
        ACT,
        LINK
    }

    public Direction downUpDir = INPUT;
    public Direction startDir;

    public boolean related;
    public Direction scope = SAME;
    public boolean samePattern;

    public int downSteps = 0;
    public int upSteps = 0;

    private Visitor() {}

    public Visitor(Activation origin, Direction startDir) {
        this.origin = origin;
        this.current = origin;
        this.transition = LINK;
        this.startDir = startDir;
    }

    public Visitor(Activation origin, Activation current, Direction scope, Direction startDir, Direction downUpDir, boolean related) {
        this.origin = origin;
        this.current = current;
        this.startDir = startDir;
        this.downUpDir = downUpDir;
        this.scope = scope;
        this.related = related;
    }

    public Visitor prepareNextStep(Activation current, Transition t) {
        Visitor nv = new Visitor();
        nv.current = current;
        nv.transition = t;
        nv.previousStep = this;
        nv.origin = origin;
        nv.downUpDir = downUpDir;
        nv.startDir = startDir;
        nv.related = related;
        nv.scope = scope;
        nv.upSteps = upSteps;
        nv.downSteps = downSteps;
        nv.samePattern = samePattern;
        return nv;
    }

    public void incrementPathLength() {
        if (downUpDir == INPUT) {
            this.downSteps++;
        } else {
            this.upSteps++;
        }
    }

    public boolean getSelfRef() {
        return downSteps == 0 || upSteps == 0;
    }

    public int numSteps() {
        return downSteps + upSteps;
    }

    public void tryToLink(Activation act) {
        if (downUpDir != OUTPUT || numSteps() < 1) return;
        if (scope == INPUT && related) return;
        if (startDir == INPUT && !act.isActive()) return; // <--
        if (act == origin || act.isConflicting()) return; // <--

        origin.getPhase().tryToLink(act, this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if(previousStep != null) {
            sb.append(previousStep + "\n");
        }

        sb.append("Origin:" + origin.getShortString() + ", ");
        sb.append("Current:" + (current != null ? current.getShortString() : "X") + ", ");

        sb.append("DownUp:" + downUpDir + ", ");
        sb.append("StartDir:" + startDir + ", ");

        sb.append("Related:" + related + ", ");
        sb.append("Scope:" + scope + ", ");
        sb.append("SamePattern:" + samePattern + ", ");

        sb.append("DownSteps:" + downSteps + ", ");
        sb.append("UpSteps:" + upSteps + "");

        return sb.toString();
    }
}
