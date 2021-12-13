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
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.RelatedBNSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import static network.aika.linker.LinkingTask.addNextLinkerSteps;


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

    private static void addLink(Synapse s, Activation iAct, Activation oAct) {
        Link nl = oAct.addLink(s, iAct, false);

        addNextLinkerSteps(nl);
    }

    private static RelatedBNSynapse getRelSynapse(BindingNeuron n, InhibitoryNeuron relInhib) {
        return (RelatedBNSynapse) n.getInputSynapse(relInhib.getProvider());
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;

        TextModel model = (TextModel) prev.getModel();

        InhibitoryActivation inhibActNext = prev.getInhibTokenAct(model.getNextTokenInhib());
        BindingActivation relActNext = getRelAct(inhibActNext);
        RelatedBNSynapse relSynNext = getRelSynapse(relActNext.getNeuron(), model.getPrevTokenInhib());

        InhibitoryActivation inhibActPrev = next.getInhibTokenAct(model.getPrevTokenInhib());
        BindingActivation relActPrev = getRelAct(inhibActPrev);
        RelatedBNSynapse relSynPrev = getRelSynapse(relActPrev.getNeuron(), model.getNextTokenInhib());

        addLink(relSynNext, inhibActPrev, relActNext);
        addLink(relSynPrev, inhibActNext, relActPrev);
    }

    private static BindingActivation getRelAct(InhibitoryActivation inhibAct) {
        return (BindingActivation) inhibAct
                .getInputLinks()
                .findFirst()
                .orElse(null)
                .getInput();
    }

    private InhibitoryActivation getInhibTokenAct(InhibitoryNeuron inhibitoryNeuron) {
        return (InhibitoryActivation) reverseBindingSignals
                .values()
                .stream()
                .filter(bs -> bs.getScope() == 0)
                .map(bs -> bs.getCurrentAct())
                .filter(act -> act.getNeuron() == inhibitoryNeuron)
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
