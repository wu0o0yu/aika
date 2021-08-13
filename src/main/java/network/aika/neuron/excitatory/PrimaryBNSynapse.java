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
package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PrimaryBNSynapse<I extends Neuron<?>> extends InputBNSynapse<I> {

    private static final Logger log = LoggerFactory.getLogger(PrimaryBNSynapse.class);


    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        if(dir == OUTPUT) {
            log.info(act.getLabel() + " CandidateGradient:" + act.getNeuron().getCandidateGradient(act));
        }

        return dir == OUTPUT;
    }

    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
        Direction currentDir = v.getCurrentDir();
        if(v.getStartDir() != currentDir)
            return;

        Scope ns = currentDir.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return;

        l.follow(v, ns);
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
        Direction currentDir = v.getCurrentDir();
        if(v.getStartDir() != currentDir)
            return;

        Scope ns = currentDir.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return;

        l.follow(v, ns);
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {
    }

    @Override
    public void inhibitoryTransitionLoop(ActVisitor v, Link l) {
        if (v.getStartDir() == v.getCurrentDir())
            return;

        l.follow(v);
    }

    public void negativeSynapseTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }
}
