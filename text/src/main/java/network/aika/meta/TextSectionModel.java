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
import network.aika.Scope;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;
import network.aika.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.meta.NetworkUtils.addNegativeFeedbackLoop;

/**
 *
 * @author Lukas Molzberger
 */
public class TextSectionModel {

    private static final Logger log = LoggerFactory.getLogger(TextSectionModel.class);

    protected static double NEG_MARGIN_TS_BEGIN = 1.1;
    protected static double NEG_MARGIN_TS_END = 1.1;

    protected static double NEG_MARGIN_TS = 1.1;

    protected PhraseTemplateModel phraseModel;


    protected Model model;

    protected NeuronProvider textSectionRelationPT;
    protected NeuronProvider textSectionRelationNT;

    protected NeuronProvider textSectionPatternN;

    protected NeuronProvider textSectionBeginBN;

    protected NeuronProvider textSectionEndBN;

    protected NeuronProvider textSectionPatternCategory;


    public TextSectionModel(PhraseTemplateModel phraseModel) {
        this.phraseModel = phraseModel;
        model = phraseModel.getModel();
    }

    protected void initTextSectionTemplates() {
        log.info("Text-Section");

        textSectionRelationPT = TokenPositionRelationNeuron.lookupRelation(model, -1, -300)
                .getProvider(true);

        textSectionRelationNT = TokenPositionRelationNeuron.lookupRelation(model, 300, 1)
                .getProvider(true);

        double netTarget = 2.5;
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        textSectionPatternN = new PatternNeuron()
                .init(model, "Abstract Text-Section")
                .getProvider(true);

        textSectionBeginBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-Begin")
                .getProvider(true);

        textSectionEndBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-End")
                .getProvider(true);

        sectionBeginToSectionEndRelation();
    }

    public PatternActivation addTextSection(Document doc, int begin, int end) {
        return new PatternActivation(doc.createActivationId(), doc, (PatternNeuron) textSectionPatternN.getNeuron());
    }

    protected void sectionBeginToSectionEndRelation() {
        double prevNetTarget = textSectionBeginBN.getNeuron().getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(textSectionRelationPT.getNeuron(), textSectionEndBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(textSectionBeginBN.getNeuron(), textSectionEndBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  SectionBeginToSectionEndRelation:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }
}
