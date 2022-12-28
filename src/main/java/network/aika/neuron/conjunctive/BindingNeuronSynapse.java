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
package network.aika.neuron.conjunctive;

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.*;
import network.aika.neuron.visitor.linking.binding.BindingDownVisitor;
import network.aika.neuron.visitor.linking.LinkingOperator;
import network.aika.neuron.visitor.linking.binding.RelationLinkingDownVisitor;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class BindingNeuronSynapse<S extends BindingNeuronSynapse, I extends Neuron, L extends BindingNeuronLink<S, IA>, IA extends Activation<?>> extends
        ConjunctiveSynapse<
                S,
                I,
                BindingNeuron,
                L,
                IA,
                BindingActivation
                >
{
    public BindingNeuronSynapse(Scope scope) {
        super(scope);
    }

    @Override
    public void startVisitor(LinkingOperator c, Activation bs) {
        Thought t = bs.getThought();
        RelationInputSynapse rel = getOutput().findLatentRelationNeurons()
                .stream()
                .findAny()
                .orElse(null);

        BindingDownVisitor v = rel != null ?
                new RelationLinkingDownVisitor(t, c, rel, c.getRelationDir(scope)) :
                new BindingDownVisitor(t, c);

        v.start(bs);
    }

    public void initDummyLink(BindingActivation oAct) {
    }

    @Override
    public double getPropagatePreNetUB(IA iAct) {
        return getOutput().getPreNetUBDummyWeightSum() +
                super.getPropagatePreNetUB(iAct);
    }

    public double getPreNetDummyWeight() {
        return 0.0;
    }
}
