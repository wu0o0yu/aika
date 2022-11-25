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
package network.aika.neuron.disjunctive;

import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.neuron.visitor.linking.LinkingOperator;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class CategorySynapse<S extends CategorySynapse, I extends ConjunctiveNeuron, O extends CategoryNeuron<?, OA>, IA extends ConjunctiveActivation<?>, OA extends CategoryActivation> extends DisjunctiveSynapse<
        S,
        I,
        O,
        CategoryLink<S, IA, OA>,
        IA,
        OA
        >
{

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    protected void link() {
        linkInput();
    }

    @Override
    public void startVisitor(LinkingOperator c, Activation bs) {

    }

    @Override
    public void linkAndPropagateOut(IA bs) {
        if (getPropagatePreNetUB(bs) > 0.0)
            propagate(bs);
    }
}
