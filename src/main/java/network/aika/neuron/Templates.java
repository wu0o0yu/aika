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
    public InputBNSynapse RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE;
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
    public Scope B_INPUT = new Scope("B_INPUT", 3);
    public Scope B_SAME = new Scope("B_SAME", 4);
    public Scope B_RELATED_INPUT = new Scope("B_RELATED_INPUT", 5);
    public Scope B_RELATED_SAME = new Scope("B_RELATED_SAME", 6);


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
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE =
                init(
                        new InputBNSynapse(INPUT_BINDING_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        true,
                        true
                );

        RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE =
                init(
                        new InputBNSynapse(INHIBITORY_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        true,
                        true
                );

        SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(SAME_BINDING_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        true,
                        true
                );

        RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new SameBNSynapse(SAME_PATTERN_TEMPLATE, SAME_BINDING_TEMPLATE, null, true),
                        true,
                        true
                );

        NEGATIVE_SYNAPSE_TEMPLATE =
                init(
                        new NegativeBNSynapse(INHIBITORY_TEMPLATE, SAME_BINDING_TEMPLATE, null),
                        false,
                        true
                );

        PATTERN_SYNAPSE_TEMPLATE =
                init(
                        new PatternSynapse(SAME_BINDING_TEMPLATE, SAME_PATTERN_TEMPLATE, null),
                        true,
                        true
                );

        PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new PrimaryInhibitorySynapse(INPUT_PATTERN_TEMPLATE, INHIBITORY_TEMPLATE, null),
                        true,
                        true
                );

        INHIBITORY_SYNAPSE_TEMPLATE =
                init(
                        new InhibitorySynapse(SAME_BINDING_TEMPLATE, INHIBITORY_TEMPLATE, null),
                        false,
                        true
                );


        Transition.add(PrimaryInhibitorySynapse.class, true, I_INPUT, I_SAME);
        Transition.add(InhibitorySynapse.class, true, I_SAME, I_SAME);
        Transition.add(InputBNSynapse.class, I_INPUT, I_SAME);



        Transition.add(PatternSynapse.class, true, P_SAME, P_SAME);
        Transition.add(PatternSynapse.class, P_SAME, P_SAME);

        Transition.add(SameBNSynapse.class, P_SAME, P_SAME);



        Transition.add(InputBNSynapse.class, true, B_INPUT, B_SAME);
        Transition.add(InputBNSynapse.class, B_RELATED_INPUT, B_RELATED_SAME);
        Transition.add(InputBNSynapse.class, B_RELATED_INPUT, B_INPUT);
        Transition.add(InputBNSynapse.class, B_INPUT, B_RELATED_INPUT); //TODO: OUTPUT only when startDir == INPUT

        Transition.add(SameBNSynapse.class, true, B_SAME, B_SAME);
        Transition.add(SameBNSynapse.class, B_INPUT, B_INPUT);
        Transition.add(SameBNSynapse.class, B_RELATED_INPUT, B_RELATED_INPUT);
        Transition.add(SameBNSynapse.class, B_SAME, B_RELATED_SAME);
        Transition.add(SameBNSynapse.class, B_RELATED_SAME, B_SAME);

        Transition.add(InhibitorySynapse.class, B_SAME, B_SAME);
        Transition.add(InhibitorySynapse.class, B_INPUT, B_INPUT);
        Transition.add(InhibitorySynapse.class, B_RELATED_SAME, B_RELATED_SAME);
        Transition.add(InhibitorySynapse.class, B_RELATED_INPUT, B_RELATED_INPUT);

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

    private <S extends Synapse> S init(S ts, boolean linkInput, boolean linkOutput) {
        if(linkInput) {
            ts.linkInput();
        }
        if(linkOutput) {
            ts.linkOutput();
        }
        return ts;
    }
}
