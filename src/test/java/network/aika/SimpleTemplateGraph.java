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
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.conjunctive.text.TokenPositionRelationNeuron;
import network.aika.neuron.disjunctive.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class SimpleTemplateGraph implements TemplateGraph {

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
    public SamePatternSynapse SAME_PATTERN_SYNAPSE_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public NegativeFeedbackSynapse NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE;
    public PatternSynapse PATTERN_SYNAPSE_TEMPLATE;
    public InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE;
    public CategorySynapse CATEGORY_SYNAPSE_TEMPLATE;
    public BindingCategorySynapse BINDING_CATEGORY_SYNAPSE_TEMPLATE;

    private Model model;

    private Map<Long, double[]> coords;

    public SimpleTemplateGraph() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{0.223, 2.184});
        coords.put(2l, new double[]{-0.155, 1.125});
        coords.put(3l, new double[]{1.257, 2.85});
        coords.put(4l, new double[]{0.457, 1.111});
        coords.put(5l, new double[]{1.18, 2.231});
        coords.put(6l, new double[]{-0.556, 2.189});
        coords.put(7l, new double[]{3.484, 1.506});
        coords.put(8l, new double[]{1.4, 0.0});
        coords.put(9l, new double[]{1.4, 0.0});
        coords.put(10l, new double[]{2.768, -0.037});
        coords.put(11l, new double[]{2.768, -0.037});
        coords.put(12l, new double[]{2.04, -0.021});
        coords.put(13l, new double[]{2.04, -0.021});
        coords.put(14l, new double[]{1.397, 0.772});
        coords.put(15l, new double[]{2.766, 0.805});
        coords.put(16l, new double[]{2.261, 1.627});
        coords.put(17l, new double[]{1.028, 1.656});
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
                        "Primary Input Synapse from Pattern",
                        0.01
                );
//        PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE.setAllowPropagate(true);

        PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PrimaryInputSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Primary Input Synapse from Category",
                        0.01
                );
//        PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE.setAllowPropagate(true);

        RELATED_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new RelatedInputSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Related Input Synapse",
                        0.0
                );

        RELATED_INPUT_SYNAPSE_FROM_LATENT_RELATION_TEMPLATE =
                init(
                        new RelatedInputSynapse(),
                        TOKEN_POSITION_RELATION_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Related Input Synapse From Latent Relation",
                        0.0
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SamePatternSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Same Pattern Synapse",
                        0.0
                );

        POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Positive Feedback Synapse",
                        0.01
                );
        POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE.getFeedbackBias().setValue(0.0);


        POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Positive Feedback Synapse",
                        0.01
                );
        POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE.getFeedbackBias().setValue(0.0);

        REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Reverse Pattern Synapse",
                        0.0
                );

        REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Reverse Pattern Synapse",
                        0.0
                );

        NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE =
                init(
                        new NegativeFeedbackSynapse(),
                        INHIBITORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Negative Feedback Synapse",
                        0.0
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(),
                        BINDING_TEMPLATE,
                        PATTERN_TEMPLATE,
                        "Pattern Synapse",
                        1.0 // Needs to be above the tolerance
                );
//        PATTERN_SYNAPSE_TEMPLATE.setAllowPropagate(true);

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(),
                        BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        "Inhibitory Synapse",
                        1.0
                );

        CATEGORY_SYNAPSE_TEMPLATE =
                init(
                        new CategorySynapse(),
                        PATTERN_TEMPLATE,
                        CATEGORY_TEMPLATE,
                        "Category Synapse",
                        0.0
                );


        BINDING_CATEGORY_SYNAPSE_TEMPLATE =
                init(
                        new BindingCategorySynapse(),
                        BINDING_TEMPLATE,
                        BINDING_CATEGORY_TEMPLATE,
                        "Biding Category Synapse",
                        0.0
                );
    }

    @Override
    public Collection<Neuron> getAllTemplateNeurons() {
        return Arrays.asList(
                BINDING_TEMPLATE,
                PATTERN_TEMPLATE,
                INHIBITORY_TEMPLATE,
                CATEGORY_TEMPLATE,
                TOKEN_POSITION_RELATION_TEMPLATE
        );
    }

    @Override
    public double[] getCoordinate(Neuron templateNeuron) {
        return coords.get(templateNeuron.getId());
    }

    private <N extends Neuron> N init(String label, double initialBias, NeuronProducer<N> np) {
        N n = model.lookupNeuron(label, np);

        n.setLabel(label);
        n.getBias().setValue(initialBias);

        TemplateNeuron templateInfo = n.getTemplateInfo();
        templateInfo.setLabel(label);

        n.getProvider().save();
        return n;
    }

    private <S extends Synapse> S init(S ts, Neuron input, Neuron output, String templateLabel, double initialWeight) {
        ts.setInput(input);
        ts.setOutput(output);
        ts.getWeight().setValue(initialWeight);

        TemplateSynapse ti = ts.getTemplateInfo();
        ti.setLabel(templateLabel);

        ts.linkInput();
        ts.linkOutput();

        return ts;
    }
}
