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

import network.aika.Thought;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;

import static network.aika.neuron.activation.linker.Mode.INDUCTION;


/**
 *
 * @author Lukas Molzberger
 */
public class LTargetNode<N extends INeuron> extends LNode<N> {

    public LTargetNode(Class<N> neuronClass, Boolean isMature, String label) {
        super(neuronClass, isMature, label);
    }

    public Activation follow(Mode m, INeuron n, Activation act, LLink from, Activation startAct) {
        if(n == null && m == INDUCTION) {
            n = createNeuron(startAct.getNeuron().getModel(), "");
        }

        if(act == null) {
            Thought doc = startAct.getThought();
            act = new Activation(doc.getNewActivationId(), doc, n);
        }

        return act;
    }

    private INeuron createNeuron(Model m, String label) {
        INeuron n;
        try {
            n = neuronClass.getConstructor(Model.class, String.class, Boolean.class)
                    .newInstance(m, label, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return n;
    }
}
