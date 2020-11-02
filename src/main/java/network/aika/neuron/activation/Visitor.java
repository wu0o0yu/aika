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

import network.aika.neuron.Neuron;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Visitor {
    public Activation origin;

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
        this.startDir = startDir;
    }

    public Visitor(Activation origin, Direction scope, boolean related) {
        this.origin = origin;
        this.startDir = null;
        this.scope = scope;
        this.related = related;
    }

    public Visitor copy() {
        Visitor nv = new Visitor();
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

    public void follow(Activation act) {
        Visitor v = act.getNeuron()
                .transition(this);

        if(v != null) {
            v.followLinks(act);
        }
    }

    public void followLinks(Activation act) {
        if (downUpDir == OUTPUT && numSteps() >= 1) {
            tryToLink(act);
//            return;
        }

        act.marked = true;
        Stream<Link> s = act.getLinks(downUpDir)
                .filter(l -> l.follow(downUpDir));

        if (downUpDir == OUTPUT) {
            s = s.collect(Collectors.toList()).stream();
        }

        s.forEach(l -> {
                    Activation nextAct = l.getActivation(downUpDir);
                    Visitor v = l.getSynapse()
                            .transition(this);
                    if(v != null) {
                        v.follow(nextAct);
                    }
                }
        );
        act.marked = false;
    }

    public void tryToLink(Activation act) {
        if (scope == INPUT && related) return;
        if (startDir == INPUT && !act.isActive()) return; // <--
        if (act == origin || act.isConflicting()) return; // <--

        Activation iAct = startDir == INPUT ? act : origin;
        Activation oAct = startDir == OUTPUT ? act : origin;

        if (!oAct.getNeuron().isInputNeuron()) {
            iAct.getPhase().tryToLink(iAct, oAct, this);
        }
    }
}
