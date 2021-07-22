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
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.scope.Scope;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class SameBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public SameBNSynapse() {
    }


    public Direction getStartDir(Direction dir) {
        return isRecurrent() ? dir.invert() : dir;
    }

    public void transition(ActVisitor v, Synapse s, Link l) {
        s.samePatternTransitionLoop(v, l);
    }

    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    @Override
    public void inhibitoryTransitionLoop(ActVisitor v, Link l) {
        l.follow(v);
    }

    public boolean checkTemplatePropagate(Direction dir, Activation act) {
        if (dir == INPUT && act.getNeuron().isInputNeuron()) {
            return false;
        }

        if(dir == OUTPUT && isRecurrent) {
            return false;
        }

        return super.checkTemplatePropagate(dir, act);
    }
}
