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

import network.aika.Model;
import network.aika.callbacks.InputRelationsCallback;
import network.aika.neuron.bindingsignal.BindingSignal;


import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class LatentRelationNeuron extends BindingNeuron {

    private InputRelationsCallback inputRelationsCallback;

    public LatentRelationNeuron() {
    }

    public LatentRelationNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }

    public void setInputRelationsCallback(InputRelationsCallback inputRelationsCallback) {
        this.inputRelationsCallback = inputRelationsCallback;
    }

    @Override
    public BindingNeuron instantiateTemplate(boolean addProvider) {
        LatentRelationNeuron n = new LatentRelationNeuron(getModel(), addProvider);
        initFromTemplate(n);

        return n;
    }

    public Stream<BindingSignal> getRelatedBindingSignals(BindingSignal fromBS) {
        Stream<BindingSignal> bindingSignals = super.getRelatedBindingSignals(fromBS);
        if(inputRelationsCallback != null) {
            bindingSignals = Stream.concat(
                    bindingSignals,
                    inputRelationsCallback.getRelatedBindingSignals(fromBS)
            );
        }

        return bindingSignals;
    }
}
