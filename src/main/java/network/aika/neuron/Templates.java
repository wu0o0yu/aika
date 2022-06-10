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
package network.aika.neuron;

import network.aika.Model;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.neuron.disjunctive.CategorySynapse;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.neuron.disjunctive.InhibitorySynapse;

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Templates {


    private final Model model;

    public BindingNeuron BINDING_TEMPLATE = new BindingNeuron();
    public PatternNeuron PATTERN_TEMPLATE = new PatternNeuron();
    public InhibitoryNeuron INHIBITORY_TEMPLATE = new InhibitoryNeuron();
    public CategoryNeuron CATEGORY_TEMPLATE = new CategoryNeuron();


    public PrimaryInputSynapse PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public PrimaryInputSynapse PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public RelatedInputSynapse RELATED_INPUT_SYNAPSE_TEMPLATE;
    public SamePatternSynapse SAME_PATTERN_SYNAPSE_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE;
    public ReversePatternSynapse REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE;
    public NegativeFeedbackSynapse NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE;
    public PatternSynapse PATTERN_SYNAPSE_TEMPLATE;
    public InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE;
    public CategorySynapse CATEGORY_SYNAPSE_TEMPLATE;

    private final Map<Byte, Neuron> templateNeuronIndex = new TreeMap<>();
    private final Map<Byte, Synapse> templateSynapseIndex = new TreeMap<>();

    public Templates(Model m) {
        model = m;

        init(BINDING_TEMPLATE, -1, "Binding Neuron", -0.02);
        init(PATTERN_TEMPLATE, -2, "Pattern Neuron", 0.0);
        init(INHIBITORY_TEMPLATE, -3, "Inhibitory Neuron", 0.0);
        init(CATEGORY_TEMPLATE, -4, "Category Neuron", 0.0);

        PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new PrimaryInputSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Primary Input Synapse from Pattern",
                        1,
                        0.01
                );
//        PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE.setAllowPropagate(true);

        PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PrimaryInputSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Primary Input Synapse from Category",
                        2,
                        0.01
                );
//        PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE.setAllowPropagate(true);

        RELATED_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new RelatedInputSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Related Input Synapse",
                        3,
                        0.0
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SamePatternSynapse(),
                        BINDING_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Same Pattern Synapse",
                        4,
                        0.0
                );

        POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Positive Feedback Synapse",
                        5,
                        0.01
                );
        POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE.getFeedbackBias().set(0.0);


        POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Positive Feedback Synapse",
                        6,
                        0.01
                );
        POSITIVE_FEEDBACK_SYNAPSE_FROM_CATEGORY_TEMPLATE.getFeedbackBias().set(0.0);

        REVERSE_PATTERN_SYNAPSE_FROM_PATTERN_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        PATTERN_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Reverse Pattern Synapse",
                        7,
                        0.0
                );

        REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE =
                init(
                        new ReversePatternSynapse(),
                        CATEGORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Reverse Pattern Synapse",
                        8,
                        0.0
                );

        NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE =
                init(
                        new NegativeFeedbackSynapse(),
                        INHIBITORY_TEMPLATE,
                        BINDING_TEMPLATE,
                        "Negative Feedback Synapse",
                        9,
                        0.0
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(),
                        BINDING_TEMPLATE,
                        PATTERN_TEMPLATE,
                        "Pattern Synapse",
                        10,
                        1.0 // Needs to be above the tolerance
                );
//        PATTERN_SYNAPSE_TEMPLATE.setAllowPropagate(true);

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(),
                        BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        "Inhibitory Synapse",
                        11,
                        1.0
                );

        CATEGORY_SYNAPSE_TEMPLATE =
                init(
                        new CategorySynapse(),
                        PATTERN_TEMPLATE,
                        CATEGORY_TEMPLATE,
                        "Category Synapse",
                        12,
                        0.0
                );
    }

    public Collection<Neuron> getAllTemplates() {
        return Arrays.asList(
                BINDING_TEMPLATE,
                PATTERN_TEMPLATE,
                INHIBITORY_TEMPLATE,
                CATEGORY_TEMPLATE
        );
    }

    private <N extends Neuron> void init(N n, int id, String label, double initialBias) {
        NeuronProvider np = new NeuronProvider(model, id);
        templateNeuronIndex.put((byte) id, n);
        np.setNeuron(n);
        n.setProvider(np);
        n.setLabel(label);
        n.getBias().set(initialBias);

        TemplateNeuron templateInfo = n.getTemplateInfo();
        templateInfo.setLabel(label);
    }

    private <S extends Synapse> S init(S ts, Neuron input, Neuron output, String templateLabel, int templateSynapseId, double initialWeight) {
        ts.setInput(input);
        ts.setOutput(output);
        ts.getWeight().set(initialWeight);

        TemplateSynapse ti = ts.getTemplateInfo();
        ti.setLabel(templateLabel);
        ti.setTemplateSynapseId((byte) templateSynapseId);
        templateSynapseIndex.put(ti.getTemplateSynapseId(), ts);

        ts.linkInput();
        ts.linkOutput();

        return ts;
    }

    public Neuron getTemplateNeuron(byte templateNeuronId) {
        return templateNeuronIndex.get(templateNeuronId);
    }

    public Synapse getTemplateSynapse(byte templateSynapseId) {
        return templateSynapseIndex.get(templateSynapseId);
    }
}
