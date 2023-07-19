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

import network.aika.elements.synapses.InhibitorySynapse;
import network.aika.elements.synapses.Synapse;
import network.aika.enums.Scope;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.meta.NetworkMotivs.*;

/**
 *
 * @author Lukas Molzberger
 */
public class TypedTextSectionModel extends TextSectionModel {

    private static final Logger log = LoggerFactory.getLogger(TypedTextSectionModel.class);

    protected NeuronProvider textSectionHeadlinePrimaryInputBN;

    protected NeuronProvider textSectionHeadlineBN;

    protected NeuronProvider textSectionHeadlinePattern;

    protected NeuronProvider textSectionHintBN;

    protected NeuronProvider tsBeginInhibitoryN;

    protected NeuronProvider tsEndInhibitoryN;

    protected NeuronProvider tsInhibitoryN;

    protected double headlineInputPatternNetTarget = 5.0;


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

        textSectionHeadlinePattern = instantiatePatternWithBindingNeurons()
                .getProvider(true);

        textSectionHintBN = new BindingNeuron()
                .init(model, "Text-Section-Hint")
                .getProvider(true);

        double netTarget = 2.5;

        textSectionHeadlineBN = addBindingNeuron(
                textSectionHeadlinePattern.getNeuron(),
                "Text-Section-Headline",
                10.0,
                headlineInputPatternNetTarget,
                netTarget
        ).getProvider(true);

        addPositiveFeedbackLoop(
                textSectionHeadlineBN.getNeuron(),
                textSectionPatternN.getNeuron(),
                2.5,
                patternNetTarget,
                bindingNetTarget,
                0.0,
                false
        );

        addRelation(
                textSectionHeadlineBN.getNeuron(),
                textSectionBeginBN.getNeuron(),
                phraseModel.relPT.getNeuron(),
                5.0,
                10.0
        );

        textSectionHintBN = new BindingNeuron()
                .init(model, "Abstract Text-Section Hint")
                .getProvider(true);

        addPositiveFeedbackLoop(
                textSectionHintBN.getNeuron(),
                textSectionPatternN.getNeuron(),
                2.5,
                patternNetTarget,
                bindingNetTarget,
                0.0,
                false
        );

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

    private PatternNeuron instantiatePatternWithBindingNeurons() {
        PatternNeuron tpn = phraseModel.patternN.getNeuron();
        PatternNeuron pn = instantiatePatternNeuron(tpn);

        InhibitoryNeuron tInhibN = phraseModel.inhibitoryN.getNeuron();
        InhibitoryNeuron inhibN = instantiateInhibitoryNeuron(tInhibN);

        Map<NeuronProvider, Neuron> templateMapping = new TreeMap<>();
        pn.getInputSynapsesByType(PatternSynapse.class)
                .forEach(s -> {
                            BindingNeuron[] bn = instantiateBindingNeuron(s);
                            templateMapping.put(bn[0].getProvider(), bn[1]);
                        }
                );

        pn.getInputSynapsesByType(PatternSynapse.class)
                .map(Synapse::getInput)
                .forEach(tbn -> {
                            BindingNeuron bn = (BindingNeuron) templateMapping.get(tbn.getProvider());
                            instantiatePatternSynapse(tpn, pn, tbn, bn);
                            instantiateInhibitorySynapse(tInhibN, inhibN, tbn, bn);
                            instantiateBindingNeuronSynapses(np -> templateMapping.get(np), tbn.getProvider(), bn);
                        }
                );
        return pn;
    }

    private PatternNeuron instantiatePatternNeuron(PatternNeuron tpn) {
        PatternNeuron pn;
        pn = tpn
                .instantiateTemplate()
                .init(model, "Text-Section-Headline");

        makeAbstract(pn);
        return pn;
    }

    private InhibitoryNeuron instantiateInhibitoryNeuron(InhibitoryNeuron tInhibN) {
        InhibitoryNeuron inhibN;
        inhibN = tInhibN
                .instantiateTemplate()
                .init(model, "Inhib. TS-Headline");

        makeAbstract(inhibN);
        return inhibN;
    }

    private static void instantiatePatternSynapse(PatternNeuron tpn, PatternNeuron pn, BindingNeuron tbn, BindingNeuron bn) {
        PatternSynapse ps = (PatternSynapse) tpn.getInputSynapse(tbn.getProvider());
        ps.instantiateTemplate(bn, pn);
    }

    private static void instantiateInhibitorySynapse(InhibitoryNeuron tInhibN, InhibitoryNeuron inhibN, BindingNeuron tbn, BindingNeuron bn) {
        InhibitorySynapse inhibS = (InhibitorySynapse) tInhibN.getInputSynapse(tbn.getProvider());
        inhibS.instantiateTemplate(bn, inhibN);
    }

    private static void instantiateBindingNeuronSynapses(Function<NeuronProvider, Neuron> resolver, NeuronProvider tbn, BindingNeuron bn) {
        tbn.getInputSynapses()
                .forEach(ts -> {
                    Neuron in = resolver.apply(ts.getPInput());
                    ts.instantiateTemplate(
                            in != null ?
                                in :
                                ts.getInput(),
                            bn
                    );
                });
    }

    private BindingNeuron[] instantiateBindingNeuron(PatternSynapse tps) {
        BindingNeuron tbn = tps.getInput();
        BindingNeuron bn = tbn
                .instantiateTemplate()
                .init(model, tbn.getLabel() + " TS-Headline");

        makeAbstract(bn);

        if(tbn.getId().longValue() == phraseModel.primaryBN.getId().longValue())
            textSectionHeadlinePrimaryInputBN = bn.getProvider(true);

        return new BindingNeuron[] {tbn, bn};
    }

    private void sectionHintRelations(BindingNeuron fromBN, LatentRelationNeuron relN) {
        addRelation(
                fromBN,
                textSectionHintBN.getNeuron(),
                relN,
                5.0,
                10.0
        );
    }
}
