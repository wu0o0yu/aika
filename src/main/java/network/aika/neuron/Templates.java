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
import network.aika.neuron.activation.scopes.Scope;
import network.aika.neuron.activation.scopes.Transition;
import network.aika.neuron.excitatory.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Templates {

    private Model model;

    public BindingNeuron INPUT_BINDING_TEMPLATE = new BindingNeuron();
    public BindingNeuron SAME_BINDING_TEMPLATE = new BindingNeuron();
    public PatternNeuron INPUT_PATTERN_TEMPLATE = new PatternNeuron();
    public PatternNeuron SAME_PATTERN_TEMPLATE = new PatternNeuron();
    public InhibitoryNeuron INHIBITORY_TEMPLATE = new InhibitoryNeuron();


    public InputBNSynapse PRIMARY_INPUT_SYNAPSE_TEMPLATE;
    public InputBNSynapse RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE;
    public InputBNSynapse RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE;
    public SameBNSynapse SAME_PATTERN_SYNAPSE_TEMPLATE;
    public SameBNSynapse RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE;
    public NegativeBNSynapse NEGATIVE_SYNAPSE_TEMPLATE;
    public PatternSynapse PATTERN_SYNAPSE_TEMPLATE;
    public PrimaryInhibitorySynapse PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE;
    public InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE;

    double OFFSET_I_X = -4.0;
    double OFFSET_I_Y = 4.0;
    public Scope I_INPUT = new Scope("I_INPUT", 0, 0.0 + OFFSET_I_X, -1.0 + OFFSET_I_Y);
    public Scope I_SAME = new Scope("I_SAME", 1, 0.0 + OFFSET_I_X, 0.0 + OFFSET_I_Y);

    double OFFSET_P_X = 0.0;
    double OFFSET_P_Y = 4.0;
    public Scope P_SAME = new Scope("P_SAME", 2, 0.0 + OFFSET_P_X, 0.0 + OFFSET_P_Y);

    double OFFSET_SB_X = 4.0;
    double OFFSET_SB_Y = 4.0;
    public Scope SB_INPUT = new Scope("SB_INPUT", 3, 0.0 + OFFSET_SB_X, -1.0 + OFFSET_SB_Y);
    public Scope SB_SAME = new Scope("SB_SAME", 4, 0.0 + OFFSET_SB_X, 0.0 + OFFSET_SB_Y);
    public Scope SB_RELATED_INPUT = new Scope("SB_RELATED_INPUT", 5, 1.0 + OFFSET_SB_X, -1.0 + OFFSET_SB_Y);
    public Scope SB_RELATED_SAME = new Scope("SB_RELATED_SAME", 6, 1.0 + OFFSET_SB_X, 0.0 + OFFSET_SB_Y);

    double OFFSET_IB_X = -4.0;
    double OFFSET_IB_Y = -4.0;
    public Scope IB_INPUT = new Scope("IB_INPUT", 7, 0.0 + OFFSET_IB_X, -1.0 + OFFSET_IB_Y);
    public Scope IB_SAME = new Scope("IB_SAME", 8, 0.0 + OFFSET_IB_X, 0.0 + OFFSET_IB_Y);

    double OFFSET_PB_X = 0.0;
    double OFFSET_PB_Y = -4.0;
    public Scope PB_SAME = new Scope("PB_SAME", 9, 0.0 + OFFSET_PB_X, 0.0 + OFFSET_PB_Y);

    double OFFSET_NB_X = 4.0;
    double OFFSET_NB_Y = -4.0;
    public Scope NB_SAME = new Scope("NB_SAME", 10, 0.0 + OFFSET_NB_X, 0.0 + OFFSET_NB_Y);


    private Map<Byte, Neuron> templateNeuronIndex = new TreeMap<>();
    private Map<Byte, Synapse> templateSynapseIndex = new TreeMap<>();

    public Templates(Model m) {
        model = m;

        init(INPUT_BINDING_TEMPLATE, -1, "Input Template Binding Neuron", -1.0, -1.0);
        init(SAME_BINDING_TEMPLATE, -2, "Same Template Binding Neuron", 0.0, 0.0);
        init(INPUT_PATTERN_TEMPLATE, -3, "Input Template Pattern Neuron", 1.0, -1.0);
        init(SAME_PATTERN_TEMPLATE, -4, "Same Template Pattern Neuron", -1.0, 1.0);
        init(INHIBITORY_TEMPLATE, -5, "Template Inhibitory Neuron", 1.0, 1.0);

        Set<Neuron<?>> BINDING_NEURON_TEMPLATE_GROUP = Set.of(INPUT_BINDING_TEMPLATE, SAME_BINDING_TEMPLATE);
        INPUT_BINDING_TEMPLATE.getTemplateInfo().setTemplateGroup(BINDING_NEURON_TEMPLATE_GROUP);
        SAME_BINDING_TEMPLATE.getTemplateInfo().setTemplateGroup(BINDING_NEURON_TEMPLATE_GROUP);

        Set<Neuron<?>> PATTERN_NEURON_TEMPLATE_GROUP = Set.of(INPUT_PATTERN_TEMPLATE, SAME_PATTERN_TEMPLATE);
        INPUT_PATTERN_TEMPLATE.getTemplateInfo().setTemplateGroup(PATTERN_NEURON_TEMPLATE_GROUP);
        SAME_PATTERN_TEMPLATE.getTemplateInfo().setTemplateGroup(PATTERN_NEURON_TEMPLATE_GROUP);

        INHIBITORY_TEMPLATE.getTemplateInfo().setTemplateGroup(Set.of(INHIBITORY_TEMPLATE));

        PRIMARY_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new InputBNSynapse(),
                        INPUT_PATTERN_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Primary Input Synapse Template",
                        1,
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE =
                init(
                        new InputBNSynapse(),
                        INPUT_BINDING_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Related Input Synapse from Binding Template",
                        2,
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE =
                init(
                        new InputBNSynapse(),
                        INHIBITORY_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Related Input Synapse from Inhibitory Template",
                        3,
                        true,
                        true
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(false),
                        SAME_BINDING_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Same Pattern Synapse Template",
                        4,
                        true,
                        true
                );

        RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(true),
                        SAME_PATTERN_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Recurrent Same Pattern Synapse Template",
                        5,
                        true,
                        true
                );

        NEGATIVE_SYNAPSE_TEMPLATE =
                init(
                        new NegativeBNSynapse(),
                        INHIBITORY_TEMPLATE,
                        SAME_BINDING_TEMPLATE,
                        "Negative Synapse Template",
                        6,
                        false,
                        true
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(),
                        SAME_BINDING_TEMPLATE,
                        SAME_PATTERN_TEMPLATE,
                        "Pattern Synapse Template",
                        7,
                        true,
                        true
                );

        PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new PrimaryInhibitorySynapse(),
                        INPUT_PATTERN_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        "Primary Inhibitory Synapse Template",
                        8,
                        true,
                        true
                );

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(),
                        SAME_BINDING_TEMPLATE,
                        INHIBITORY_TEMPLATE,
                        "Inhibitory Synapse Template",
                        9,
                        false,
                        true
                );


        SAME_BINDING_TEMPLATE.getTemplateInfo().inputScopes = Set.of(IB_SAME, SB_SAME, PB_SAME, NB_SAME);
        SAME_BINDING_TEMPLATE.getTemplateInfo().outputScopes = Set.of(SB_RELATED_SAME, IB_INPUT, I_SAME, P_SAME);

        SAME_PATTERN_TEMPLATE.getTemplateInfo().inputScopes = Set.of(P_SAME, PB_SAME);
        SAME_PATTERN_TEMPLATE.getTemplateInfo().outputScopes = Set.of(P_SAME, PB_SAME, IB_INPUT, I_INPUT);

        INHIBITORY_TEMPLATE.getTemplateInfo().inputScopes = Set.of(I_SAME, NB_SAME); // TODO: fix startDir/targetDir for NB_SAME
        INHIBITORY_TEMPLATE.getTemplateInfo().outputScopes = Set.of(I_SAME, IB_INPUT, NB_SAME);

        Transition.add(true, I_INPUT, I_SAME, PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE);
        Transition.add(true, I_SAME, I_SAME, INHIBITORY_SYNAPSE_TEMPLATE);
        Transition.add(I_INPUT, I_SAME, PRIMARY_INPUT_SYNAPSE_TEMPLATE);


        Transition.add(true, P_SAME, P_SAME, PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(P_SAME, P_SAME,
                RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE,
                SAME_PATTERN_SYNAPSE_TEMPLATE
        );

        Transition.add(true, PB_SAME, PB_SAME, RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(PB_SAME, PB_SAME, PATTERN_SYNAPSE_TEMPLATE);


        Transition.add(true, NB_SAME, NB_SAME, NEGATIVE_SYNAPSE_TEMPLATE);
        Transition.add(NB_SAME, NB_SAME, INHIBITORY_SYNAPSE_TEMPLATE);


        Transition.add(true, IB_INPUT, IB_SAME,
                PRIMARY_INPUT_SYNAPSE_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        );
        Transition.add(IB_INPUT, IB_INPUT,
                SAME_PATTERN_SYNAPSE_TEMPLATE,
                RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE
        );
        Transition.add(IB_INPUT, IB_INPUT, INHIBITORY_SYNAPSE_TEMPLATE);


        Transition.add(true, SB_RELATED_SAME, SB_SAME, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(SB_INPUT, SB_INPUT, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(SB_RELATED_INPUT, SB_RELATED_INPUT, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Synapse[] inputBindingSynapses = new Synapse[] {
                RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        };
        Transition.add(SB_INPUT, SB_SAME, PRIMARY_INPUT_SYNAPSE_TEMPLATE);
        Transition.add(SB_RELATED_INPUT, SB_RELATED_SAME, inputBindingSynapses);
        Transition.add(SB_RELATED_INPUT, SB_INPUT, inputBindingSynapses);
        Transition.add(SB_INPUT, SB_RELATED_INPUT, inputBindingSynapses);
        Transition.add(SB_INPUT, SB_INPUT,
                INHIBITORY_SYNAPSE_TEMPLATE,
                RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE
        );
        Transition.add(SB_RELATED_INPUT, SB_RELATED_INPUT, INHIBITORY_SYNAPSE_TEMPLATE);
    }

    public Collection<Neuron> getAllTemplates() {
        return Arrays.asList(
                INPUT_BINDING_TEMPLATE,
                SAME_BINDING_TEMPLATE,
                INPUT_PATTERN_TEMPLATE,
                SAME_PATTERN_TEMPLATE,
                INHIBITORY_TEMPLATE
        );
    }

    private <N extends Neuron> N init(N n, int id, String label, double x, double y) {
        NeuronProvider np = new NeuronProvider(model, id);
        templateNeuronIndex.put((byte) id, n);
        np.setNeuron(n);
        n.setProvider(np);
        n.setLabel(label);
        TemplateNeuronInfo templateInfo = n.getTemplateInfo();
        templateInfo.setXCoord(x);
        templateInfo.setYCoord(y);
        templateInfo.setLabel(label);
        return n;
    }

    private <S extends Synapse> S init(S ts, Neuron input, Neuron output, String templateLabel, int templateSynapseId, boolean linkInput, boolean linkOutput) {
        ts.setInput(input);
        ts.setOutput(output);

        TemplateSynapseInfo ti = ts.getTemplateInfo();
        ti.setLabel(templateLabel);
        ti.setTemplateSynapseId((byte) templateSynapseId);
        templateSynapseIndex.put(ti.getTemplateSynapseId(), ts);

        if(linkInput) {
            ts.linkInput();
        }
        if(linkOutput) {
            ts.linkOutput();
        }
        return ts;
    }

    public Neuron getTemplateNeuron(byte templateNeuronId) {
        return templateNeuronIndex.get(Byte.valueOf(templateNeuronId));
    }

    public Synapse getTemplateSynapse(byte templateSynapseId) {
        return templateSynapseIndex.get(Byte.valueOf(templateSynapseId));
    }
}
