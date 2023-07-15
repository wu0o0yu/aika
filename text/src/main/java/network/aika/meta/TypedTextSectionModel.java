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

import network.aika.Scope;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.InputPatternSynapse;
import network.aika.elements.synapses.RelationInputSynapse;
import network.aika.elements.synapses.SamePatternSynapse;
import network.aika.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static network.aika.meta.NetworkUtils.addNegativeFeedbackLoop;


/**
 *
 * @author Lukas Molzberger
 */
public class TypedTextSectionModel extends TextSectionModel {

    private static final Logger log = LoggerFactory.getLogger(TypedTextSectionModel.class);

    protected NeuronProvider textSectionHeadlineBN;

    protected NeuronProvider textSectionHintBN;

    protected NeuronProvider textSectionPatternCategory;

    protected NeuronProvider tsBeginInhibitoryN;

    protected NeuronProvider tsEndInhibitoryN;

    protected NeuronProvider tsInhibitoryN;

    protected double headlineInputPatternNetTarget = 5.0;

    protected double headlineInputPatternValueTarget;


    public TypedTextSectionModel(PhraseTemplateModel phraseModel) {
        super(phraseModel);
    }

    public void addTargetTSHeadline(Document doc, Set<String> headlineLabels, int begin, int end) {
        log.info(doc.getContent() + " : " + headlineLabels.stream().collect(Collectors.joining(", ")));
    }

    public void addTargetTextSections(Document doc, Set<String> tsLabels) {
        log.info(doc.getContent() + " : " + tsLabels.stream().collect(Collectors.joining(", ")));
    }

    protected void initTextSectionTemplates() {
        super.initTextSectionTemplates();

        log.info("Typed Text-Section");

        textSectionHintBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-Hint")
                .getProvider(true);

        double netTarget = 2.5;
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        textSectionHeadlineBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-Headline")
                .getProvider(true);

        phraseModel.abstractNeurons.add(textSectionHeadlineBN);

        headlineInputPatternValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(headlineInputPatternNetTarget);

        //     new BindingCategoryInputSynapse()
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(phraseModel.patternCategory.getNeuron(), textSectionHeadlineBN.getNeuron())
                .adjustBias(headlineInputPatternValueTarget);


        headlineToSectionBeginRelation();


        textSectionHintBN = new BindingNeuron()
                .init(model, "Abstract Text-Section Hint")
                .getProvider(true);

        sectionHintRelations(textSectionBeginBN.getNeuron(), textSectionRelationPT.getNeuron());
        sectionHintRelations(textSectionEndBN.getNeuron(), textSectionRelationNT.getNeuron());



        tsBeginInhibitoryN = new InhibitoryNeuron(Scope.INPUT)
                .init(model, "I TS Begin")
                .getProvider(true);


        addNegativeFeedbackLoop(
                textSectionBeginBN.getNeuron(),
                tsBeginInhibitoryN.getNeuron(),
                NEG_MARGIN_TS_BEGIN * -netTarget
        );

        tsEndInhibitoryN = new InhibitoryNeuron(Scope.INPUT)
                .init(model, "I TS End")
                .getProvider(true);

        addNegativeFeedbackLoop(
                textSectionEndBN.getNeuron(),
                tsBeginInhibitoryN.getNeuron(),
                NEG_MARGIN_TS_END * -netTarget
        );

        tsInhibitoryN = new InhibitoryNeuron(Scope.SAME)
                .init(model, "I TS")
                .getProvider(true);

        addNegativeFeedbackLoop(
                textSectionHintBN.getNeuron(),
                tsInhibitoryN.getNeuron(),
                NEG_MARGIN_TS * -netTarget
        );

        addNegativeFeedbackLoop(
                textSectionBeginBN.getNeuron(),
                tsInhibitoryN.getNeuron(),
                NEG_MARGIN_TS * -netTarget
        );

        addNegativeFeedbackLoop(
                textSectionEndBN.getNeuron(),
                tsInhibitoryN.getNeuron(),
                NEG_MARGIN_TS * -netTarget
        );
    }

    private void headlineToSectionBeginRelation() {
        double prevNetTarget = textSectionHeadlineBN.getNeuron().getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(phraseModel.relPT.getNeuron(), textSectionBeginBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(textSectionHeadlineBN.getNeuron(), textSectionBeginBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  HeadlineToSectionBeginRelation:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }

    private void sectionHintRelations(BindingNeuron fromBN, LatentRelationNeuron relN) {
        double prevNetTarget = fromBN.getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(relN, textSectionHintBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(fromBN, textSectionHintBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  SectionHintRelations:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }
}
