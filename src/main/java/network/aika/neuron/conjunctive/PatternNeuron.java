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
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.disjunctive.CategorySynapse;
import network.aika.neuron.disjunctive.PatternCategorySynapse;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ConjunctiveNeuron<ConjunctiveSynapse, PatternActivation> {

    public PatternNeuron() {
        super();
    }

    @Override
    public CategorySynapse newCategorySynapse() {
        return new PatternCategorySynapse();
    }

    @Override
    public PatternActivation createActivation(Thought t) {
        return new PatternActivation(t.createActivationId(), t, this);
    }

    @Override
    public PatternNeuron instantiateTemplate(boolean addProvider) {
        PatternNeuron n = new PatternNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    public PatternCategoryInputSynapse getCategoryInputSynapse() {
        return inputSynapses.stream()
                .filter(s -> s instanceof PatternCategoryInputSynapse)
                .map(s -> (PatternCategoryInputSynapse) s)
                .findAny()
                .orElse(null);
    }

    @Override
    protected void updateSumOfLowerWeights() {
    }
}
