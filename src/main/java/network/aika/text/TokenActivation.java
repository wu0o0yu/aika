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

import network.aika.fields.Field;
import network.aika.fields.QueueField;
import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;

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

    private BindingActivation getRelNTBindingActivation(TextModel m) {
        return followSynapse(
                getCategoryActivation(m),
                m.getRelNTRevPatternSyn()
        );
    }

    private BindingActivation getRelPTBindingActivation(TextModel m) {
        return followSynapse(
                getCategoryActivation(m),
                m.getRelPTRevPatternSyn()
        );
    }

    private CategoryActivation getCategoryActivation(TextModel m) {
        return followSynapse(
                this,
                getNeuron().getOutputSynapse(m.getTokenCategory().getProvider())
        );
    }

    private <A extends Activation> A followSynapse(Activation<?> fromAct, Synapse s) {
        Link link = fromAct.getOutputLinks(s)
                .findFirst()
                .orElse(null);

        if(link == null)
            return null;

        return (A) link.getOutput();
    }

    protected Field initNet() {
        return new ValueSortedQueueField(this, "net", 10.0);
    }

    public boolean isInput() {
        return true;
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;

        TextModel model = prev.getModel();

        next.linkPrimaryInput(model.getRelNTPrimaryInputSyn(), prev.getRelNTBindingActivation(model));
        prev.linkPrimaryInput(model.getRelPTPrimaryInputSyn(), next.getRelPTBindingActivation(model));
    }

    private void linkPrimaryInput(PrimaryInputSynapse relSynNext, BindingActivation toAct) {
        BindingSignal fromBS = getCategoryActivation(toAct.getModel()).getBindingSignal(this);
        BindingSignal toBS = fromBS.propagate(relSynNext);
        toBS.init(toAct);
        relSynNext.createLink(fromBS.getActivation(), (BindingActivation) toBS.getActivation());
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
