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

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {

    public static void initPatternTheCat(SimpleTemplateGraph t, InhibitoryNeuron inhibNThe, InhibitoryNeuron inhibNCat, int variant) {
        PatternNeuron theIN = t.TOKEN_TEMPLATE.lookupToken("the");
        PatternNeuron catIN = t.TOKEN_TEMPLATE.lookupToken("cat");

        int relFrom = variant < 2 ? -5 : 1;
        int relTo = variant < 2 ? -1 : 5;

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(relFrom, relTo);

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);

        BindingNeuron catBN = createNeuron(t.BINDING_TEMPLATE, "cat (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN, variant == 0  || variant == 2 ? 10.0 : 5.0);

        if(variant < 2) {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, catBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, theBN, catBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        } else {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, theBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, catBN, theBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        }

        PatternNeuron theCatP = initPatternLoop(t, "the cat", theBN, catBN);

        addInhibitoryLoop(t, inhibNThe, false, theBN);
        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-the (tc)"), true, theBN);

        updateBias(theCatP, 3.0);

        updateBias(theBN, 3.0);
        updateBias(catBN, 3.0);
    }

    public static void initPatternBlackCat(SimpleTemplateGraph t) {
        PatternNeuron blackIN = t.TOKEN_TEMPLATE.lookupToken("black");
        PatternNeuron catIN = t.TOKEN_TEMPLATE.lookupToken("cat");

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(-1, -1);

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
    }

    public static void initPatternTheDog(SimpleTemplateGraph t, InhibitoryNeuron inhibNThe, InhibitoryNeuron inhibNDog, int variant) {
        PatternNeuron theIN = t.TOKEN_TEMPLATE.lookupToken("the");
        PatternNeuron dogIN = t.TOKEN_TEMPLATE.lookupToken("dog");

        int relFrom = variant < 2 ? -5 : 1;
        int relTo = variant < 2 ? -1 : 5;

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(relFrom, relTo);

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);

        BindingNeuron dogBN = createNeuron(t.BINDING_TEMPLATE, "dog (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, dogIN, dogBN, variant == 0  || variant == 2 ? 10.0 : 5.0);

        if(variant < 2) {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, dogBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, theBN, dogBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        } else {
            createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, theBN, 5.0);
            createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, dogBN, theBN, variant == 1 || variant == 3 ? 10.0 : 5.0);
        }

        PatternNeuron theDogP = initPatternLoop(t, "the dog", theBN, dogBN);

        addInhibitoryLoop(t, inhibNThe, false, theBN);
        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-the (tg)"), true, theBN);

        updateBias(theDogP, 3.0);

        updateBias(theBN, 3.0);
        updateBias(dogBN, 3.0);
    }
}
