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
package network.aika;

import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.text.TextModel;

import static network.aika.TestUtils.*;
import static network.aika.TestUtils.updateBias;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {

    public static void initPatternTheCat(TextModel m, SimpleTemplateGraph t, InhibitoryNeuron inhibNCat, int variant) {
        PatternNeuron theIN = m.lookupToken("the");
        PatternNeuron catIN = m.lookupToken("cat");

        int variantDir = variant < 2 ? -1 : 1;

        LatentRelationNeuron relPT = m.lookupRelation(variantDir, variantDir);

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);

        BindingNeuron catBN = createNeuron(t.BINDING_TEMPLATE, "cat (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN, variant == 0  || variant == 2 ? 10.0 : 5.0);

        if(variantDir < 0) {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, catBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, theBN, catBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        } else {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, theBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, catBN, theBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        }

        PatternNeuron theCatP = initPatternLoop(t, "the cat", theBN, catBN);

        initInhibitoryLoop(t, "the", theBN);
        addInhibitoryLoop(t, inhibNCat, catBN);

        updateBias(theCatP, 3.0);

        updateBias(theBN, 3.0);
        updateBias(catBN, 3.0);
    }

    public static void initPatternBlackCat(TextModel m, SimpleTemplateGraph t) {
        PatternNeuron blackIN = m.lookupToken("black");
        PatternNeuron catIN = m.lookupToken("cat");

        LatentRelationNeuron relPT = m.lookupRelation(-1, -1);

        BindingNeuron blackBN = createNeuron(t.BINDING_TEMPLATE, "black (black cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, blackIN, blackBN, 10.0);
        BindingNeuron catBN = createNeuron(t.BINDING_TEMPLATE, "cat (black cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN, 20.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, catBN, 5.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, blackBN, catBN, 5.0);

        PatternNeuron blackCat = initPatternLoop(t, "black cat", blackBN, catBN);
        updateBias(blackCat, 3.0);

        updateBias(blackBN, 3.0);
        updateBias(catBN, 3.0);

        //        initInhibitoryLoop(t, "jackson", jacksonForenameBN, jacksonCityBN);
    }

    public static void initPatternTheDog(TextModel m, SimpleTemplateGraph t) {
        PatternNeuron theIN = m.lookupToken("the");
        PatternNeuron dogIN = m.lookupToken("dog");

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);
        BindingNeuron dogBN = createNeuron(t.BINDING_TEMPLATE, "dog (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, dogIN, dogBN, 10.0);
        PatternNeuron theDog = initPatternLoop(t, "the dog", theBN, dogBN);
        updateBias(theDog, 3.0);

        updateBias(theBN, 3.0);
        updateBias(dogBN, 3.0);

        initInhibitoryLoop(t, "the", theBN);
    }
}
