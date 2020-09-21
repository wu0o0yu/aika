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

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Visitor {
    public Activation origin;

    public Direction downUpDir;
    public Direction startDir;

    public boolean related;
    public boolean input;
    public boolean selfRef;

    public int sameDirSteps;

    public Visitor(Visitor c, boolean incr) {
        this.origin = c.origin;
        this.downUpDir = c.downUpDir;
        this.startDir = c.startDir;
        this.related = c.related;
        this.input = c.input;
        this.selfRef = c.selfRef;
        this.sameDirSteps = incr ? c.sameDirSteps++ : c.sameDirSteps;
    }

    public Visitor(Activation origin, Direction startDir) {
        this.origin = origin;
        this.startDir = startDir;
        this.downUpDir = INPUT;
        this.selfRef = true;
        this.sameDirSteps = 0;
    }

    public Visitor(Activation origin, boolean input, boolean related) {
        this.origin = origin;
        this.startDir = null;
        this.downUpDir = null;
        this.selfRef = false;
        this.input = input;
        this.related = related;
        this.sameDirSteps = 0;
    }

    public void follow(Activation act) {
        if (downUpDir == OUTPUT) {
            if (startDir == INPUT && !act.isActive()) return; // <--
            if (act == origin || act.isConflicting()) return; // <--
            tryToLink(act);
        }

        act.marked = true;
        Stream<Link> s = act.getLinks(downUpDir)
                .filter(l -> l.follow(downUpDir));

        if (downUpDir == OUTPUT) {
            s = s.collect(Collectors.toList()).stream();
        }

        s.forEach(l -> {
                    Activation nextAct = l.getActivation(downUpDir);
                    Visitor nv = l.getSynapse().transition(this);
                    nv = nextAct.getNeuron().transition(nv);
                    nv.follow(nextAct);
                }
        );
        act.marked = false;
    }

    public void tryToLink(Activation act) {
        Activation iAct = startDir == INPUT ? act : origin;
        Activation oAct = startDir == OUTPUT ? act : origin;

        Neuron on = oAct.getNeuron();
        if (!on.isInputNeuron()) {
            on.tryToLink(iAct, oAct, this);
        }
    }
}
