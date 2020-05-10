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
package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;


/**
 *
 * @author Lukas Molzberger
 */
public class LMatchingNode<N extends INeuron> extends LNode<N> {

    public LMatchingNode(Class<N> neuronClass, String label) {
        super(neuronClass, label);
    }

    public Activation follow(Mode m, INeuron n, Activation act, LLink from, Activation startAct) {
        if(!checkNeuron(n)) {
            return null;
        }

        if(act.isConflicting()) return null;

        act.lNode = this;

        links.stream()
                .filter(nl -> nl != from)
                .forEach(nl -> nl.follow(m, act, this, startAct));

        act.lNode = null;

        return act;
    }
}
