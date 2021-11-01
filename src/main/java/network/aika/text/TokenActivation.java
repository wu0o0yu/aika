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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.Optional;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private int rangeBegin;
    private int rangeEnd;
    private TokenActivation previousToken;
    private TokenActivation nextToken;


    public TokenActivation(int id, int begin, int end, Document doc, PatternNeuron patternNeuron) {
        super(id, doc, patternNeuron);
        rangeBegin = begin;
        rangeEnd = end;
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;

        TextModel model = (TextModel) prev.getModel();
        InhibitoryActivation inhibActPrev = next.getInhibTokenAct(model.getPrevTokenInhib());
        InhibitoryActivation inhibActNext = prev.getInhibTokenAct(model.getNextTokenInhib());


    }


    private InhibitoryActivation getInhibTokenAct(InhibitoryNeuron inhibitoryNeuron) {
        return (InhibitoryActivation) reverseBindingSignals
                .values()
                .stream()
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
    public int[] getRange() {
        return new int[]{rangeBegin, rangeEnd};
    }
}
