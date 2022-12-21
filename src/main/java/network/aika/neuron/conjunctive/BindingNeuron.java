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
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.disjunctive.BindingCategorySynapse;
import network.aika.neuron.disjunctive.CategorySynapse;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Lukas Molzberger
 */
public class BindingNeuron extends ConjunctiveNeuron<BindingNeuronSynapse, BindingActivation> {


    @Override
    public CategorySynapse newCategorySynapse() {
        return new BindingCategorySynapse();
    }

    public List<RelationInputSynapse> findLatentRelationNeurons() {
        return getInputSynapsesAsStream()
                .filter(s -> s instanceof RelationInputSynapse)
                .map(s -> (RelationInputSynapse) s)
                .collect(Collectors.toList());
    }

    @Override
    public BindingActivation createActivation(Thought t) {
        return new BindingActivation(t.createActivationId(), t, this);
    }

    @Override
    public BindingCategoryInputSynapse getCategoryInputSynapse() {
        return getProvider().getInputSynapses()
                .filter(BindingCategoryInputSynapse.class::isInstance)
                .map(BindingCategoryInputSynapse.class::cast)
                .findAny()
                .orElse(null);
    }

    public double getPreNetUBDummyWeightSum() {
        return getInputSynapsesAsStream()
                .mapToDouble(s -> s.getPreNetUBDummyWeight())
                .sum();
    }
}
