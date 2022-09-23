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
package network.aika;

import network.aika.callbacks.NeuronProducer;
import network.aika.neuron.*;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.conjunctive.text.TokenPositionRelationNeuron;
import network.aika.neuron.disjunctive.*;
import network.aika.text.Document;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class SimpleTemplateGraph {

    public BindingNeuron BINDING_TEMPLATE;
    public PatternNeuron PATTERN_TEMPLATE;
    public InhibitoryNeuron INHIBITORY_TEMPLATE;
    public CategoryNeuron CATEGORY_TEMPLATE;
    public BindingCategoryNeuron BINDING_CATEGORY_TEMPLATE;

    public TokenNeuron TOKEN_TEMPLATE;
    public TokenPositionRelationNeuron TOKEN_POSITION_RELATION_TEMPLATE;

    public PrimaryInputSynapse PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public PrimaryInputSynapse PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public RelatedInputSynapse RELATED_INPUT_SYNAPSE_TEMPLATE;
    public RelatedInputSynapse RELATED_INPUT_SYNAPSE_FROM_LATENT_RELATION_TEMPLATE;
    public CategoryInputSynapse CATEGORY_INPUT_SYNAPSE_TEMPLATE;
    public SamePatternSynapse SAME_PATTERN_SYNAPSE_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public NegativeFeedbackSynapse NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE;
    public PatternSynapse PATTERN_SYNAPSE_TEMPLATE;
    public InhibitorySynapse INPUT_INHIBITORY_SYNAPSE_TEMPLATE;
    public InhibitorySynapse SAME_INHIBITORY_SYNAPSE_TEMPLATE;
    public CategorySynapse CATEGORY_SYNAPSE_TEMPLATE;
    public BindingCategorySynapse BINDING_CATEGORY_SYNAPSE_TEMPLATE;

    private Model model;

    public SimpleTemplateGraph() {
    }

    public void init(Model m) {
        this.model = m;

        BINDING_TEMPLATE = init("Binding Neuron", -0.02, l -> new BindingNeuron());
        PATTERN_TEMPLATE = init("Pattern Neuron", 0.0, l -> new PatternNeuron());
        INHIBITORY_TEMPLATE = init("Inhibitory Neuron", 0.0, l -> new InhibitoryNeuron());
        CATEGORY_TEMPLATE = init("Category Neuron", 0.0, l -> new CategoryNeuron());
        BINDING_CATEGORY_TEMPLATE = init("Binding Category Neuron", 0.0, l -> new BindingCategoryNeuron());
        TOKEN_TEMPLATE = init("Token Neuron", 0.0, l -> new TokenNeuron());
        TOKEN_POSITION_RELATION_TEMPLATE = init("Token Position Relation Neuron", 0.0, l -> new TokenPositionRelationNeuron());

        PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new PrimaryInputSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.01
                );

        PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PrimaryInputSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.01
                );

        RELATED_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new RelatedInputSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        RELATED_INPUT_SYNAPSE_FROM_LATENT_RELATION_TEMPLATE =
                init(
                        new RelatedInputSynapse(),
                        TOKEN_POSITION_RELATION_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        CATEGORY_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new CategoryInputSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SamePatternSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.01
                );

        POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.01
                );

        REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE =
                init(
                        new NegativeFeedbackSynapse(),
                        INHIBITORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        0.0
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(),
                        BINDING_TEMPLATE,
                        PATTERN_TEMPLATE,
                        1.0 // Needs to be above the tolerance
                );

        INPUT_INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InputInhibitorySynapse(),
                        BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        1.0
                );

        SAME_INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new SameInhibitorySynapse(),
                        BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        1.0
                );

        CATEGORY_SYNAPSE_TEMPLATE =
                init(
                        new CategorySynapse(),
                        PATTERN_TEMPLATE,
                        CATEGORY_TEMPLATE,
                        0.0
                );


        BINDING_CATEGORY_SYNAPSE_TEMPLATE =
                init(
                        new BindingCategorySynapse(),
                        BINDING_TEMPLATE,
                        BINDING_CATEGORY_TEMPLATE,
                        0.0
                );
    }

    public TokenActivation addToken(Document doc, String t, Integer pos, int i, int j) {
        return doc.addToken(TOKEN_TEMPLATE.lookupToken(t), pos, i, j);
    }

    private <N extends Neuron> N init(String label, double initialBias, NeuronProducer<N> np) {
        N n = model.lookupNeuron(label, np);

        n.setLabel(label);
        n.getBias().setValue(initialBias);
        n.setTemplate(true);

        n.getProvider().save();
        return n;
    }

    private <S extends Synapse> S init(S ts, Neuron input, Neuron output, double initialWeight) {
        ts.setInput(input);
        ts.setOutput(output);
        ts.getWeight().setValue(initialWeight);

        ts.linkInput();
        ts.linkOutput();

        return ts;
    }
}
