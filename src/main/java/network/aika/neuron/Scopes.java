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

import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class Scopes {

    private Model model;

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

    private List<Scope> scopes = new ArrayList<>();
    private List<Transition> transitions = new ArrayList<>();

    public List<Scope> getScopes() {
        return scopes;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    void add(boolean isTarget, Scope input, Scope output, Synapse... templateSynapse) {
        transitions.addAll(
                Transition.add(isTarget, input, output, templateSynapse)
        );
    }

    void add(Scope input, Scope output, Synapse... templateSynapse) {
        add(false, input, output, templateSynapse);
    }

    public Scopes(Model m) {
        model = m;

        Templates t = m.getTemplates();

        t.SAME_BINDING_TEMPLATE.getTemplateInfo().inputScopes = Set.of(IB_SAME, SB_SAME, PB_SAME, NB_SAME);
        t.SAME_BINDING_TEMPLATE.getTemplateInfo().outputScopes = Set.of(SB_RELATED_SAME, IB_INPUT, I_SAME, P_SAME);

        t.SAME_PATTERN_TEMPLATE.getTemplateInfo().inputScopes = Set.of(P_SAME, PB_SAME);
        t.SAME_PATTERN_TEMPLATE.getTemplateInfo().outputScopes = Set.of(P_SAME, PB_SAME, IB_INPUT, I_INPUT);

        t.INHIBITORY_TEMPLATE.getTemplateInfo().inputScopes = Set.of(I_SAME, NB_SAME); // TODO: fix startDir/targetDir for NB_SAME
        t.INHIBITORY_TEMPLATE.getTemplateInfo().outputScopes = Set.of(I_SAME, IB_INPUT, NB_SAME);

        add(true, I_INPUT, I_SAME, t.PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE);
        add(true, I_SAME, I_SAME, t.INHIBITORY_SYNAPSE_TEMPLATE);
        add(I_INPUT, I_SAME, t.PRIMARY_INPUT_SYNAPSE_TEMPLATE);


        add(true, P_SAME, P_SAME, t.PATTERN_SYNAPSE_TEMPLATE);
        add(P_SAME, P_SAME,
                t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE,
                t.SAME_PATTERN_SYNAPSE_TEMPLATE
        );

        add(true, PB_SAME, PB_SAME, t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE);
        add(PB_SAME, PB_SAME, t.PATTERN_SYNAPSE_TEMPLATE);


        add(true, NB_SAME, NB_SAME, t.NEGATIVE_SYNAPSE_TEMPLATE);
        add(NB_SAME, NB_SAME, t.INHIBITORY_SYNAPSE_TEMPLATE);


        add(true, IB_INPUT, IB_SAME,
                t.PRIMARY_INPUT_SYNAPSE_TEMPLATE,
                t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        );
        add(IB_INPUT, IB_INPUT,
                t.SAME_PATTERN_SYNAPSE_TEMPLATE,
                t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE
        );
        add(IB_INPUT, IB_INPUT, t.INHIBITORY_SYNAPSE_TEMPLATE);


        add(true, SB_RELATED_SAME, SB_SAME, t.SAME_PATTERN_SYNAPSE_TEMPLATE);
        add(SB_RELATED_INPUT, SB_RELATED_INPUT,
                t.SAME_PATTERN_SYNAPSE_TEMPLATE, t.INHIBITORY_SYNAPSE_TEMPLATE
        );
        Synapse[] inputBindingSynapses = new Synapse[] {
                t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE,
                t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
        };
        add(SB_INPUT, SB_SAME, t.PRIMARY_INPUT_SYNAPSE_TEMPLATE);
        add(SB_RELATED_INPUT, SB_RELATED_SAME, inputBindingSynapses);
        add(SB_RELATED_INPUT, SB_INPUT, inputBindingSynapses);
        add(SB_INPUT, SB_RELATED_INPUT, inputBindingSynapses);
        add(SB_INPUT, SB_INPUT,
                t.SAME_PATTERN_SYNAPSE_TEMPLATE,
                t.INHIBITORY_SYNAPSE_TEMPLATE,
                t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE
        );
        add(SB_RELATED_INPUT, SB_RELATED_INPUT, t.INHIBITORY_SYNAPSE_TEMPLATE);
    }
}
