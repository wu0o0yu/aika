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
import network.aika.neuron.activation.scopes.*;
import network.aika.neuron.excitatory.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;

import java.util.Arrays;
import java.util.Collection;

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


    public Scope I_INPUT = new Scope("I_INPUT", 0);
    public Scope I_SAME = new Scope("I_SAME", 1);
    public Scope P_SAME = new Scope("P_SAME", 2);
    public Scope SB_INPUT = new Scope("SB_INPUT", 3);
    public Scope SB_SAME = new Scope("SB_SAME", 4);
    public Scope SB_RELATED_INPUT = new Scope("SB_RELATED_INPUT", 5);
    public Scope SB_RELATED_SAME = new Scope("SB_RELATED_SAME", 6);
    public Scope IB_INPUT = new Scope("IB_INPUT", 7);
    public Scope IB_SAME = new Scope("IB_SAME", 8);
    public Scope PB_SAME = new Scope("PB_SAME", 9);



    public Templates(Model m) {
        model = m;

        init(INPUT_BINDING_TEMPLATE, -1, "Input Template Binding Neuron");
        init(SAME_BINDING_TEMPLATE, -2, "Same Template Binding Neuron");
        init(INPUT_PATTERN_TEMPLATE, -3, "Input Template Pattern Neuron");
        init(SAME_PATTERN_TEMPLATE, -4, "Same Template Pattern Neuron");
        init(INHIBITORY_TEMPLATE, -5, "Template Inhibitory Neuron");

        INPUT_PATTERN_TEMPLATE.getTemplates().add(SAME_PATTERN_TEMPLATE);
        SAME_PATTERN_TEMPLATE.getTemplates().add(INPUT_PATTERN_TEMPLATE);

        INPUT_BINDING_TEMPLATE.getTemplates().add(SAME_BINDING_TEMPLATE);
        SAME_BINDING_TEMPLATE.getTemplates().add(INPUT_BINDING_TEMPLATE);

        PRIMARY_INPUT_SYNAPSE_TEMPLATE =
                init(
                        new InputBNSynapse(INPUT_PATTERN_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        "PRIMARY_INPUT_SYNAPSE_TEMPLATE",
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE =
                init(
                        new InputBNSynapse(INPUT_BINDING_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        "RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE",
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE =
                init(
                        new InputBNSynapse(INHIBITORY_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        "RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE",
                        true,
                        true
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(SAME_BINDING_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        "SAME_PATTERN_SYNAPSE_TEMPLATE",
                        true,
                        true
                );

        RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(SAME_PATTERN_TEMPLATE, SAME_BINDING_TEMPLATE, null, true),
                        "RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE",
                        true,
                        true
                );

        NEGATIVE_SYNAPSE_TEMPLATE =
                init(
                        new NegativeBNSynapse(INHIBITORY_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        "NEGATIVE_SYNAPSE_TEMPLATE",
                        false,
                        true
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(SAME_BINDING_TEMPLATE, SAME_PATTERN_TEMPLATE, null),
                        "PATTERN_SYNAPSE_TEMPLATE",
                        true,
                        true
                );

        PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new PrimaryInhibitorySynapse(INPUT_PATTERN_TEMPLATE, INHIBITORY_TEMPLATE, null),
                        "PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE",
                        true,
                        true
                );

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(SAME_BINDING_TEMPLATE, INHIBITORY_TEMPLATE, null),
                        "INHIBITORY_SYNAPSE_TEMPLATE",
                        false,
                        true
                );


        Transition.add(true, I_INPUT, I_SAME, PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE);
        Transition.add(true, I_SAME, I_SAME, INHIBITORY_SYNAPSE_TEMPLATE);
        Transition.add(I_INPUT, I_SAME, PRIMARY_INPUT_SYNAPSE_TEMPLATE);


        Transition.add(true, P_SAME, P_SAME, PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(P_SAME, P_SAME, RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE);

        Transition.add(true, PB_SAME, PB_SAME, RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(PB_SAME, PB_SAME, PATTERN_SYNAPSE_TEMPLATE);


        Transition.add(true, IB_INPUT, IB_SAME,
                PRIMARY_INPUT_SYNAPSE_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        );
        Transition.add(IB_INPUT, IB_INPUT, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(IB_INPUT, IB_INPUT, INHIBITORY_SYNAPSE_TEMPLATE);


        Transition.add(true, SB_RELATED_SAME, SB_SAME, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(SB_INPUT, SB_INPUT, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Transition.add(SB_RELATED_INPUT, SB_RELATED_INPUT, SAME_PATTERN_SYNAPSE_TEMPLATE);
        Synapse[] inputBindingSynapses = new Synapse[] {
                PRIMARY_INPUT_SYNAPSE_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        };
        Transition.add(SB_INPUT, SB_SAME, inputBindingSynapses);
        Transition.add(SB_RELATED_INPUT, SB_RELATED_SAME, inputBindingSynapses);
        Transition.add(SB_RELATED_INPUT, SB_INPUT, inputBindingSynapses);
        Transition.add(SB_INPUT, SB_RELATED_INPUT, inputBindingSynapses);
        Transition.add(SB_INPUT, SB_INPUT, INHIBITORY_SYNAPSE_TEMPLATE);
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

    private <N extends Neuron> N init(N n, int id, String label) {
        NeuronProvider np = new NeuronProvider(model, id);
        np.setNeuron(n);
        n.setProvider(np);
        n.setLabel(label);
        return n;
    }

    private <S extends Synapse> S init(S ts, String templateLabel, boolean linkInput, boolean linkOutput) {
        TemplateSynapseInfo templateInfo = new TemplateSynapseInfo();
        templateInfo.setLabel(templateLabel);

        ts.setTemplateInfo(templateInfo);
        if(linkInput) {
            ts.linkInput();
        }
        if(linkOutput) {
            ts.linkOutput();
        }
        return ts;
    }
}
