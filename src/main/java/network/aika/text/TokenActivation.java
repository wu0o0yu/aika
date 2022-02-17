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
package network.aika.text;

import network.aika.neuron.Range;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PositiveFeedbackSynapse;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.steps.activation.Propagate;

import static network.aika.text.TextModel.REL_NEXT_TOKEN_LABEL;
import static network.aika.text.TextModel.REL_PREVIOUS_TOKEN_LABEL;


/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private Range range;
    private TokenActivation previousToken;
    private TokenActivation nextToken;


    public TokenActivation(int id, int begin, int end, Document doc, PatternNeuron patternNeuron) {
        super(id, doc, patternNeuron);
        range = new Range(begin, end);
    }

    private static PrimaryInputSynapse getRelatedSynapse(CategoryNeuron input, BindingNeuron output) {
        return (PrimaryInputSynapse) output.getInputSynapse(input.getProvider());
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;

        TextModel model = (TextModel) prev.getModel();

        Propagate.propagate(next, next.getNeuron().getOutputSynapse(model.getTokenCategory().getProvider()));
        Propagate.propagate(next, getRelationSynapse(next.getNeuron(), REL_PREVIOUS_TOKEN_LABEL));

        CategoryActivation prevCatAct = prev.getCategoryTokenAct(model.getTokenCategory());
        BindingActivation relActNext = getRelationActivation(prev, REL_NEXT_TOKEN_LABEL);

        CategoryActivation nextCatAct = next.getCategoryTokenAct(model.getTokenCategory());
        BindingActivation relActPrev = getRelationActivation(next, REL_PREVIOUS_TOKEN_LABEL);

        PrimaryInputSynapse relSynNext = getRelatedSynapse(nextCatAct.getNeuron(), relActNext.getNeuron());
        relSynNext.createLink(nextCatAct, relActNext);

        PrimaryInputSynapse relSynPrev = getRelatedSynapse(prevCatAct.getNeuron(), relActPrev.getNeuron());
        relSynPrev.createLink(prevCatAct, relActPrev);
    }

    private static PositiveFeedbackSynapse getRelationSynapse(PatternNeuron patternNeuron, String direction) {
        return (PositiveFeedbackSynapse) patternNeuron
                .getOutputSynapses()
                .filter(s -> s.getOutput().getLabel().contains(direction))
                .findFirst()
                .orElse(null);
    }

    private static BindingActivation getRelationActivation(PatternActivation patternAct, String direction) {
        return (BindingActivation) patternAct
                .getOutputLinks()
                .filter(l -> l.getOutput().getLabel().contains(direction))
                .findFirst()
                .orElse(null)
                .getOutput();
    }

    private CategoryActivation getCategoryTokenAct(CategoryNeuron categoryNeuron) {
        return (CategoryActivation) outputLinks.values().stream()
                .map(l -> l.getOutput())
                .filter(oAct -> oAct != null)
                .filter(oAct -> oAct.getNeuron() == categoryNeuron)
                .findFirst()
                .orElse(null);
    }

    public TokenActivation getPreviousToken() {
        return previousToken;
    }

    public TokenActivation getNextToken() {
        return nextToken;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
