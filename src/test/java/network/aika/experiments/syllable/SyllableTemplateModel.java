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
package network.aika.experiments.syllable;

import network.aika.Model;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;

import java.util.function.IntToDoubleFunction;

import static network.aika.elements.neurons.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.elements.synapses.Scope.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class SyllableTemplateModel {

    Model model;

    CategoryNeuron letterCategory;

    TokenNeuron letterPN;
    LatentRelationNeuron relPT;
    LatentRelationNeuron relNT;

    InhibitoryNeuron inhibitoryN;

    CategoryNeuron inhibCat;

    PatternNeuron syllablePatternN;

    CategoryNeuron syllableCategory;


    public SyllableTemplateModel(Model m) {
        model = m;

        relPT = TokenPositionRelationNeuron.lookupRelation(model, -1, -1);
        relNT = TokenPositionRelationNeuron.lookupRelation(model, 1, 1);

        letterCategory = new PatternCategoryNeuron()
                .init(model, "Letter Category");

        letterPN = new TokenNeuron()
                .init(model, "Abstract Letter");

        new PatternCategoryInputSynapse()
                .setWeight(1.0)
                .init(letterCategory, letterPN);

        letterPN.setBias(3.0);
    }


    public void initMeta(
            double patternNetTarget,
            IntToDoubleFunction bindingNetTarget,
            IntToDoubleFunction patternSynWeight,
            IntToDoubleFunction propagateBias,
            IntToDoubleFunction posFeedbackSynWeight
    ) {
        // Abstract
        syllablePatternN = new PatternNeuron()
                .init(model, "Syllable");

        syllableCategory = new PatternCategoryNeuron()
                .init(model, "Syllable Category");

        inhibitoryN =new InhibitoryNeuron()
                .init(model, "I");

        inhibCat = new InhibitoryCategoryNeuron()
                .init(model, "Inhib. Category");

        BindingNeuron sylBeginBN = createBindingNeuron(
                patternNetTarget,
                bindingNetTarget,
                patternSynWeight,
                propagateBias,
                posFeedbackSynWeight,
                0,
                null,
                null
        );

        expandContinueBindingNeurons(
                patternNetTarget,
                bindingNetTarget,
                patternSynWeight,
                propagateBias,
                posFeedbackSynWeight,
                sylBeginBN,
                5,
                1
        );

        expandContinueBindingNeurons(
                patternNetTarget,
                bindingNetTarget,
                patternSynWeight,
                propagateBias,
                posFeedbackSynWeight,
                sylBeginBN,
                5,
                -1
        );

        new PatternCategoryInputSynapse()
                .setWeight(1.0)
                .init(syllableCategory, syllablePatternN);

        syllablePatternN.setBias(patternNetTarget);

        new InhibitoryCategoryInputSynapse()
                .setWeight(1.0)
                .init(inhibCat, inhibitoryN);
    }

    private void expandContinueBindingNeurons(
            double patternNetTarget,
            IntToDoubleFunction bindingNetTarget,
            IntToDoubleFunction patternSynWeight,
            IntToDoubleFunction propagateBias,
            IntToDoubleFunction posFeedbackSynWeight,
            BindingNeuron sylBeginBN,
            int length,
            int dir
    ) {
        BindingNeuron lastSylBN = sylBeginBN;
        int lastPos = 0;
        for(int pos = 1; pos <= length; pos++) {
            lastSylBN = createBindingNeuron(
                    patternNetTarget,
                    bindingNetTarget,
                    patternSynWeight,
                    propagateBias,
                    posFeedbackSynWeight,
                    dir * pos,
                    dir * lastPos,
                    lastSylBN
            );
            lastPos = pos;
        }
    }

    private BindingNeuron createBindingNeuron(
            double patternNetTarget,
            IntToDoubleFunction bindingNetTarget,
            IntToDoubleFunction patternSynWeight,
            IntToDoubleFunction propagateBias,
            IntToDoubleFunction posFeedbackSynWeight,
            int pos,
            Integer lastPos,
            BindingNeuron lastBN
    ) {
        double patternValueTarget = RECTIFIED_HYPERBOLIC_TANGENT
                .f(patternNetTarget);

        double netTarget = bindingNetTarget.applyAsDouble(pos);
        double valueTarget = RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        CategoryNeuron catN = new BindingCategoryNeuron()
                .init(model, "Cat. Pos:" + pos);

        BindingNeuron bn = new BindingNeuron()
                .init(model, "Abstract Pos:" + pos);

        System.out.println(bn.getLabel());

        System.out.println("patternNetTarget: " + patternNetTarget + " patternValueTarget: " + patternValueTarget);
        System.out.println("netTarget: " + netTarget + " valueTarget: " + valueTarget);

        new PrimaryInhibitorySynapse()
                .setWeight(1.0)
                .init(letterPN, inhibitoryN);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(bn, inhibitoryN);

        new NegativeFeedbackSynapse()
                .setWeight(-20.0)
                .init(inhibitoryN, bn)
                .adjustBias();

        if(lastPos == null || lastBN == null) {
            bn.setCallActivationCheckCallback(true);
        } else {
            double prevNetTarget = bindingNetTarget.applyAsDouble(lastPos);
            double prevValueTarget = RECTIFIED_HYPERBOLIC_TANGENT
                    .f(prevNetTarget);

            System.out.print("prevNetTarget: " + prevNetTarget + " prevValueTarget: " + prevValueTarget);
            System.out.print(" adjustPropagate: " + propagateBias.applyAsDouble(pos));

            if (pos > 0) {
                new RelationInputSynapse()
                        .setWeight(5.0)
                        .init(relPT, bn)
                        .adjustBias();
            } else {
                new RelationInputSynapse()
                        .setWeight(5.0)
                        .init(relNT, bn)
                        .adjustBias();
            }

            new SamePatternSynapse()
                    .setWeight(10.0)
                    .init(lastBN, bn)
                    .adjustBias(propagateBias.applyAsDouble(pos));
        }

        new PatternSynapse()
                .setWeight(patternSynWeight.applyAsDouble(pos))
                .setOptional(true)
                .init(bn, syllablePatternN)
                .adjustBias(valueTarget);

        new PositiveFeedbackSynapse()
                .setWeight(posFeedbackSynWeight.applyAsDouble(pos))
                .init(syllablePatternN, bn)
                .adjustBias(patternValueTarget);

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(letterPN, bn)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .setWeight(1.0)
                .init(catN, bn);

        bn.setBias(netTarget);

        System.out.println();
        System.out.println();

        return bn;
    }

    public TokenNeuron lookupInputToken(String label) {
        return model.lookupNeuronByLabel(label, l -> {
                    TokenNeuron n = letterPN.instantiateTemplate();

                    n.setTokenLabel(l);
                    n.setLabel(l);
                    n.setAllowTraining(false);

                    return n;
                }
        );
    }
}
