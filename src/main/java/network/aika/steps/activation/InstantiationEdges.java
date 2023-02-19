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
package network.aika.steps.activation;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.ConjunctiveActivation;
import network.aika.steps.Phase;
import network.aika.steps.Step;

import static network.aika.steps.Phase.INSTANTIATION_EDGES;


/**
 *
 * @author Lukas Molzberger
 */
public class InstantiationEdges extends Step<Activation> {

    private Activation instanceAct;

    public static void add(Activation act, Activation instanceAct) {
        if(act.instantiationEdgesIsQueued)
            return;

        act.instantiationEdgesIsQueued = true;

        Step.add(new InstantiationEdges(act, instanceAct));
    }

    public InstantiationEdges(Activation act, Activation instanceAct) {
        super(act);
        this.instanceAct = instanceAct;
    }

    @Override
    public void process() {
        getElement()
                .instantiateTemplateEdges(instanceAct);

        getElement().instantiationEdgesIsQueued = false;
    }

    @Override
    public Phase getPhase() {
        return INSTANTIATION_EDGES;
    }
}
