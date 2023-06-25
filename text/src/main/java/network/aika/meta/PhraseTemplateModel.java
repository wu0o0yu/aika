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
package network.aika.meta;

import network.aika.Model;
import network.aika.elements.activations.*;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.sign.Sign;

import java.util.Comparator;
import java.util.List;


/**
 *
 * @author Lukas Molzberger
 */
public class PhraseTemplateModel extends AbstractTemplateModel {

    PatternActivation maxSurprisalAct = null;

    public PhraseTemplateModel(Model m) {
        super(m);
    }

    @Override
    public String getPatternType() {
        return "Phrase";
    }

    @Override
    protected void initTemplateBindingNeurons() {
        BindingNeuron primaryBN = createStrongBindingNeuron(
                patternValueTarget,
                false,
                0,
                null,
                null
        );

        expandContinueBindingNeurons(
                patternValueTarget,
                1,
                primaryBN,
                5,
                1
        );

        expandContinueBindingNeurons(
                patternValueTarget,
                1,
                primaryBN,
                5,
                -1
        );
    }

    private PatternActivation initMaxSurprisalTokenAct(List<TokenActivation> tokenActs) {
        return tokenActs.stream()
                .filter(PatternActivation.class::isInstance)
                .map(PatternActivation.class::cast)
                .max(Comparator.comparingDouble(act ->
                        act.getSurprisal(Sign.POS)
                ))
                .orElse(null);
    }

    @Override
    public void setTokenInputNet(List<TokenActivation> tokenActs) {
        maxSurprisalAct = initMaxSurprisalTokenAct(tokenActs);
        super.setTokenInputNet(tokenActs);
    }

    @Override
    public boolean evaluatePrimaryBindingActs(Activation act) {
        return maxSurprisalAct == act.getActiveTemplateInstance();
    }
}
