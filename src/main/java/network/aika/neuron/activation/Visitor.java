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

    public Direction downUpDir;
    public Direction startDir;

    public boolean related;
    public Direction scope = NEUTRAL;

    public int downSteps;
    public int upSteps;

    public Visitor(Visitor v, boolean incr) {
        this.origin = v.origin;
        this.downUpDir = v.downUpDir;
        this.startDir = v.startDir;
        this.related = v.related;
        this.scope = v.scope;
        this.upSteps = v.upSteps;
        this.downSteps = v.downSteps;

        if(incr) {
            if (downUpDir == INPUT) {
                this.downSteps++;
            } else {
                this.upSteps++;
            }
        }
    }

    public Visitor(Activation origin, Direction startDir) {
        this.origin = origin;
        this.startDir = startDir;
        this.downUpDir = INPUT;
        this.downSteps = startDir == INPUT ? 1 : 0;
        this.upSteps = 0;
    }

    public Visitor(Activation origin, Direction scope, boolean related) {
        this.origin = origin;
        this.startDir = null;
        this.downUpDir = null;
        this.scope = scope;
        this.related = related;
        this.downSteps = 0;
        this.upSteps = 0;
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
        if (downUpDir == OUTPUT && numSteps() > 1) {
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
        Activation iAct = startDir == INPUT ? act : origin;
        Activation oAct = startDir == OUTPUT ? act : origin;

        if (scope == NEUTRAL && !checkSamePattern(iAct, oAct)) return;
        if (startDir == INPUT && !act.isActive()) return; // <--
        if (act == origin || act.isConflicting()) return; // <--

        Neuron on = oAct.getNeuron();
        if (!on.isInputNeuron()) {
            on.tryToLink(iAct, oAct, this);
        }
    }

    public boolean checkSamePattern(Activation in, Activation out) {
        Activation inPattern = in.getNeuron().getSamePattern(in);
        Activation outPattern = out.getNeuron().getSamePattern(out);

        return inPattern == null || outPattern == null || inPattern == outPattern;
    }
}
