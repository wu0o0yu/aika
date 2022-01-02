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
import network.aika.neuron.excitatory.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Templates {


    private final Model model;

    public BindingNeuron INPUT_BINDING_TEMPLATE = new BindingNeuron();
    public BindingNeuron OUTPUT_BINDING_TEMPLATE = new BindingNeuron();
    public PatternNeuron INPUT_PATTERN_TEMPLATE = new PatternNeuron();
    public PatternNeuron OUTPUT_PATTERN_TEMPLATE = new PatternNeuron();
    public InhibitoryNeuron INHIBITORY_TEMPLATE = new InhibitoryNeuron();


    public PrimaryBNSynapse PRIMARY_INPUT_SYNAPSE_TEMPLATE;
    public RelatedBNSynapse RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE;
    public RelatedBNSynapse RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE;
    public RelatedRecurrentBNSynapse RELATED_RECURRENT_INPUT_SYNAPSE_TEMPLATE;
    public SameBNSynapse SAME_PATTERN_SYNAPSE_TEMPLATE;
    public PositiveFeedbackSynapse POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE;
    public NegativeFeedbackSynapse NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE;
    public PatternSynapse PATTERN_SYNAPSE_TEMPLATE;
    public InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE;

    private final Map<Byte, Neuron> templateNeuronIndex = new TreeMap<>();
    private final Map<Byte, Synapse> templateSynapseIndex = new TreeMap<>();

    public Templates(Model m) {
        model = m;

        init(INPUT_BINDING_TEMPLATE, -1, "Input Binding Neuron");
        init(OUTPUT_BINDING_TEMPLATE, -2, "Output Binding Neuron");
        init(INPUT_PATTERN_TEMPLATE, -3, "Input Pattern Neuron");
        init(OUTPUT_PATTERN_TEMPLATE, -4, "Output Pattern Neuron");
        init(INHIBITORY_TEMPLATE, -5, "Inhibitory Neuron");

        Set<Neuron> BINDING_NEURON_TEMPLATE_GROUP = Set.of(INPUT_BINDING_TEMPLATE, OUTPUT_BINDING_TEMPLATE);
        INPUT_BINDING_TEMPLATE.getTemplateInfo().setTemplateGroup(BINDING_NEURON_TEMPLATE_GROUP);
        OUTPUT_BINDING_TEMPLATE.getTemplateInfo().setTemplateGroup(BINDING_NEURON_TEMPLATE_GROUP);

        Set<Neuron> PATTERN_NEURON_TEMPLATE_GROUP = Set.of(INPUT_PATTERN_TEMPLATE, OUTPUT_PATTERN_TEMPLATE);
        INPUT_PATTERN_TEMPLATE.getTemplateInfo().setTemplateGroup(PATTERN_NEURON_TEMPLATE_GROUP);
        OUTPUT_PATTERN_TEMPLATE.getTemplateInfo().setTemplateGroup(PATTERN_NEURON_TEMPLATE_GROUP);

        INHIBITORY_TEMPLATE.getTemplateInfo().setTemplateGroup(Set.of(INHIBITORY_TEMPLATE));

        PRIMARY_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new PrimaryBNSynapse(),
                        INPUT_PATTERN_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Primary Input Synapse",
                        1,
                        0.01
                );

        RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE =
                init(
                        new RelatedBNSynapse(),
                        INPUT_BINDING_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Related Input Synapse from Binding Neuron",
                        2,
                        0.0
                );

        RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE =
                init(
                        new RelatedBNSynapse(),
                        INHIBITORY_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Related Input Synapse from Inhibitory Neuron",
                        3,
                        0.0
                );

        RELATED_RECURRENT_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new RelatedRecurrentBNSynapse(),
                        INHIBITORY_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Related Input Synapse from Inhibitory Neuron",
                        10,
                        0.0
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(),
                        OUTPUT_BINDING_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Same Pattern Synapse",
                        4,
                        0.0
                );

        POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE =
                init(
                        new PositiveFeedbackSynapse(),
                        OUTPUT_PATTERN_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Positive Feedback Synapse",
                        5,
                        0.0
                );

        NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE =
                init(
                        new NegativeFeedbackSynapse(),
                        INHIBITORY_TEMPLATE,
                        OUTPUT_BINDING_TEMPLATE,
                        "Negative Feedback Synapse",
                        6,
                        0.0
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(),
                        OUTPUT_BINDING_TEMPLATE,
                        OUTPUT_PATTERN_TEMPLATE,
                        "Pattern Synapse",
                        7,
                        1.0 // Needs to be above the tolerance
                );

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(),
                        OUTPUT_BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        "Inhibitory Synapse",
                        8,
                        0.0
                );
    }

    public Collection<Neuron> getAllTemplates() {
        return Arrays.asList(
                INPUT_BINDING_TEMPLATE,
                OUTPUT_BINDING_TEMPLATE,
                INPUT_PATTERN_TEMPLATE,
                OUTPUT_PATTERN_TEMPLATE,
                INHIBITORY_TEMPLATE
        );
    }

    private <N extends Neuron> void init(N n, int id, String label) {
        NeuronProvider np = new NeuronProvider(model, id);
        templateNeuronIndex.put((byte) id, n);
        np.setNeuron(n);
        n.setProvider(np);
        n.setLabel(label);
        TemplateNeuronInfo templateInfo = n.getTemplateInfo();
        templateInfo.setLabel(label);
    }

    private <S extends Synapse> S init(S ts, Neuron input, Neuron output, String templateLabel, int templateSynapseId, double initialWeight) {
        ts.setInput(input);
        ts.setOutput(output);
        ts.getWeight().setInitialValue(initialWeight);

        TemplateSynapseInfo ti = ts.getTemplateInfo();
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
