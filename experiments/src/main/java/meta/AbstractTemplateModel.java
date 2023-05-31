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
package meta;

import network.aika.Model;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractTemplateModel {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Model model;

    protected NeuronProvider inputTokenCategory;
    protected NeuronProvider inputToken;
    protected NeuronProvider relPT;
    protected NeuronProvider relNT;

    protected NeuronProvider inhibitoryN;

    protected NeuronProvider inhibCat;

    protected NeuronProvider patternN;

    protected NeuronProvider patternCategory;

    protected double inputPatternNetTarget = 5.0;
    protected double inputPatternValueTarget;

    protected double patternNetTarget = 0.7;
    protected double patternValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
            .f(patternNetTarget);

    protected static double POS_MARGIN = 1.0;
    protected static double NEG_MARGIN = 1.1;

    protected static double PASSIVE_SYNAPSE_WEIGHT = 0.0;

    protected List<NeuronProvider> abstractNeurons = new ArrayList<>();

    public AbstractTemplateModel(Model m) {
        model = m;

        relPT = TokenPositionRelationNeuron.lookupRelation(model, -1, -1)
                .getProvider(true);

        relNT = TokenPositionRelationNeuron.lookupRelation(model, 1, 1)
                .getProvider(true);

        initInputCategoryNeuron();

        inputToken = new TokenNeuron()
                .init(model, "Abstract Input Token")
                .getProvider(true);

        abstractNeurons.add(inputToken);

        PatternCategoryInputSynapse pCatInputSyn = new PatternCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(inputTokenCategory.getNeuron(), inputToken.getNeuron());

        inputPatternValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(inputPatternNetTarget);

        log.info("Input Token: netTarget:" + inputPatternNetTarget + " valueTarget:" + inputPatternValueTarget);

        inputToken.getNeuron().setBias(inputPatternNetTarget - pCatInputSyn.getWeight().getCurrentValue());
    }

    protected void initInputCategoryNeuron() {
        inputTokenCategory = new PatternCategoryNeuron()
                .init(model, "Input Token Category")
                .getProvider(true);
    }

    public NeuronProvider getInputToken() {
        return inputToken;
    }

    public boolean evaluatePrimaryBindingActs(Activation act) {
        return false;
    }

    public double getInputPatternNetTarget() {
        return inputPatternNetTarget;
    }

    public void setTokenInputNet(List<TokenActivation> tokenActs) {
        for(TokenActivation tAct: tokenActs) {
            tAct.setNet(inputPatternNetTarget);
        }
    }

    public abstract String getPatternType();

    public void initTemplates() {
        // Abstract
        patternN = new PatternNeuron()
                .init(model, getPatternType())
                .getProvider(true);

        abstractNeurons.add(patternN);

        patternCategory = new PatternCategoryNeuron()
                .init(model, getPatternType() + " Category")
                .getProvider(true);

        inhibitoryN =new InhibitoryNeuron()
                .init(model, "I")
                .getProvider(true);

        inhibCat = new InhibitoryCategoryNeuron()
                .init(model, "Inhib. Category")
                .getProvider(true);

        log.info(getPatternType() + " Pattern: netTarget:" + patternNetTarget + " valueTarget:" + patternValueTarget);

        initTemplateBindingNeurons();

        new PatternCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(patternCategory.getNeuron(), patternN.getNeuron());

        patternN.getNeuron().setBias(patternNetTarget);

        new InhibitoryCategoryInputSynapse()
                .setWeight(1.0)
                .init(inhibCat.getNeuron(), inhibitoryN.getNeuron());
    }

    protected abstract void initTemplateBindingNeurons();

    protected void expandContinueBindingNeurons(
            double patternValueTarget,
            int optionalStart,
            BindingNeuron sylBeginBN,
            int length,
            int dir
    ) {
        BindingNeuron lastSylBN = sylBeginBN;
        int lastPos = 0;
        for(int pos = 1; pos <= length; pos++) {
            if(pos < 2) {
                lastSylBN = createStrongBindingNeuron(
                        patternValueTarget,
                        pos >= optionalStart,
                        dir * pos,
                        dir * lastPos,
                        lastSylBN
                );
            } else {
                lastSylBN = createWeakBindingNeuron(
                        patternValueTarget,
                        dir * pos,
                        lastSylBN
                );
            }
            lastPos = pos;
        }
    }

    protected BindingNeuron createStrongBindingNeuron(
            double patternValueTarget,
            boolean isOptional,
            int pos,
            Integer lastPos,
            BindingNeuron lastBN
    ) {
        double netTarget = 2.5;
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        log.info("Strong Binding-Neuron: netTarget:" + netTarget + " valueTarget:" + valueTarget);

        CategoryNeuron catN = new BindingCategoryNeuron()
                .init(model, "Cat. Pos:" + pos);

        BindingNeuron bn = new BindingNeuron()
                .init(model, "Abstract (S) Pos:" + pos);

        abstractNeurons.add(bn.getProvider());

        new PrimaryInhibitorySynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(inputToken.getNeuron(), inhibitoryN.getNeuron());

        new InhibitorySynapse(Scope.INPUT)
                .setWeight(1.0)
                .init(bn, inhibitoryN.getNeuron());

        new NegativeFeedbackSynapse()
                .setWeight(NEG_MARGIN * -netTarget)
                .init(inhibitoryN.getNeuron(), bn);

        if(lastPos == null || lastBN == null) {
            bn.setCallActivationCheckCallback(true);
        } else {
            double prevNetTarget = lastBN.getBias().getCurrentValue();
            double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                    .f(prevNetTarget);

            if (pos > 0) {
                new RelationInputSynapse()
                        .setWeight(5.0)
                        .init(relPT.getNeuron(), bn)
                        .adjustBias();
            } else {
                new RelationInputSynapse()
                        .setWeight(5.0)
                        .init(relNT.getNeuron(), bn)
                        .adjustBias();
            }

            SamePatternSynapse spSyn = new SamePatternSynapse()
                    .setWeight(10.0)
                    .init(lastBN, bn)
                    .adjustBias(prevValueTarget);

            System.out.println("  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getCurrentValue());
        }

        PatternSynapse pSyn = new PatternSynapse()
                .setWeight(2.5)
                .setOptional(isOptional)
                .init(bn, patternN.getNeuron())
                .adjustBias(valueTarget);

        System.out.println("  " + pSyn + " targetNetContr:" + -pSyn.getSynapseBias().getCurrentValue());


        PositiveFeedbackSynapse posFeedSyn = new PositiveFeedbackSynapse()
                .setWeight(POS_MARGIN * (netTarget / patternValueTarget))
                .init(patternN.getNeuron(), bn)
                .adjustBias(patternValueTarget);

        System.out.println("  " + posFeedSyn + " targetNetContr:" + -posFeedSyn.getSynapseBias().getCurrentValue());


        new InputPatternSynapse()
                .setWeight(10.0)
                .init(inputToken.getNeuron(), bn)
                .adjustBias(inputPatternValueTarget);

        new BindingCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(catN, bn);

        bn.setBias(netTarget);

        log.info("");
        log.info("");

        return bn;
    }

    protected BindingNeuron createWeakBindingNeuron(
            double patternValueTarget,
            int pos,
            BindingNeuron lastBN
    ) {
        double netTarget = 0.5;
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        log.info("Weak Binding-Neuron: netTarget:" + netTarget + " valueTarget:" + valueTarget);


        CategoryNeuron catN = new BindingCategoryNeuron()
                .init(model, "Cat. Pos:" + pos);

        BindingNeuron bn = new BindingNeuron()
                .init(model, "Abstract (W) Pos:" + pos);

        abstractNeurons.add(bn.getProvider());

        new PrimaryInhibitorySynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(inputToken.getNeuron(), inhibitoryN.getNeuron());

        new InhibitorySynapse(Scope.INPUT)
                .setWeight(1.0)
                .init(bn, inhibitoryN.getNeuron());

        new NegativeFeedbackSynapse()
                .setWeight(NEG_MARGIN * -netTarget)
                .init(inhibitoryN.getNeuron(), bn)
                .adjustBias();

        if (pos > 0) {
            new RelationInputSynapse()
                    .setWeight(5.0)
                    .init(relPT.getNeuron(), bn)
                    .adjustBias();
        } else {
            new RelationInputSynapse()
                    .setWeight(5.0)
                    .init(relNT.getNeuron(), bn)
                    .adjustBias();
        }

        double prevNetTarget = lastBN.getBias().getCurrentValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(5.0)
                .init(lastBN, bn)
                .adjustBias(prevValueTarget);

        log.info("  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getCurrentValue());

        new PatternSynapse()
                .setWeight(0.5)
                .setOptional(true)
                .init(bn, patternN.getNeuron())
                .adjustBias(valueTarget);

        PositiveFeedbackSynapse posFeedSyn = new PositiveFeedbackSynapse()
                .setWeight(POS_MARGIN * (netTarget / patternValueTarget))
                .init(patternN.getNeuron(), bn)
                .adjustBias(patternValueTarget);

        log.info("  " + posFeedSyn + " targetNetContr:" + -posFeedSyn.getSynapseBias().getCurrentValue());

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(inputToken.getNeuron(), bn)
                .adjustBias(inputPatternValueTarget);

        new BindingCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(catN, bn);

        bn.setBias(netTarget);

        return bn;
    }

    public TokenNeuron lookupInputToken(String label) {
        return model.lookupNeuronByLabel(label, l -> {
                    TokenNeuron inputTokenN = (TokenNeuron) inputToken.getNeuron();
                    TokenNeuron n = inputTokenN.instantiateTemplate()
                            .init(model, label);

                    n.setTokenLabel(label);
                    n.setAllowTraining(false);

                    return n;
                }
        );
    }

    public void disableAbstractNeurons() {
        abstractNeurons.forEach(n ->
                n.getNeuron().getBias().setValue(-1000.0)
        );
    }
}