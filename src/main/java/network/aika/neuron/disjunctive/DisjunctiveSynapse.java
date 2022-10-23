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

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.visitor.linking.LinkingOperator;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class DisjunctiveSynapse<
        S extends DisjunctiveSynapse,
        I extends Neuron,
        O extends DisjunctiveNeuron<?, OA>,
        L extends Link<S, IA, OA>,
        IA extends Activation<?>,
        OA extends DisjunctiveActivation
        > extends Synapse<S,I,O,L,IA,OA>
{

    @Override
    public void startVisitor(LinkingOperator c, Activation bs) {

    }

    @Override
    public void linkAndPropagateOut(IA bs) {
        if (isPropagate())
            propagate(bs);
    }

    @Override
    public void setModified() {
        if(input != null)
            getInput().setModified();
    }

    @Override
    public double getSumOfLowerWeights() {
        return 0.0;
    }
}
