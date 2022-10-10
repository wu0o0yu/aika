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

import network.aika.neuron.conjunctive.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.conjunctive.text.TokenPositionRelationNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {

    public static void initPatternTheCat(Model m, InhibitoryNeuron inhibNThe, InhibitoryNeuron inhibNCat, int variant) {
        PatternNeuron theIN = TokenNeuron.lookupToken(m, "the");
        PatternNeuron catIN = TokenNeuron.lookupToken(m, "cat");

        int relFrom = variant < 2 ? -5 : 1;
        int relTo = variant < 2 ? -1 : 5;

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, relFrom, relTo);

        BindingNeuron theBN = createNeuron(new BindingNeuron(), "the (the cat)");
        new PrimaryInputSynapse()
                .init(theIN, theBN, 10.0)
                .adjustBias();

        BindingNeuron catBN = createNeuron(new BindingNeuron(), "cat (the cat)");
        new PrimaryInputSynapse()
                .init(catIN, catBN, variant == 0  || variant == 2 ? 10.0 : 5.0)
                .adjustBias();

        if(variant < 2) {
            new RelatedInputSynapse()
                    .init(relPT, catBN, 5.0)
                    .adjustBias();
            new SamePatternSynapse()
                    .init(theBN, catBN, variant == 1 || variant == 3 ? 10.0 : 5.0)
                    .adjustBias();
        } else {
            new RelatedInputSynapse()
                    .init(relPT, theBN, 5.0)
                    .adjustBias();
            new SamePatternSynapse()
                    .init(catBN, theBN, variant == 1 || variant == 3 ? 10.0 : 5.0)
                    .adjustBias();
        }

        PatternNeuron theCatP = initPatternLoop(m, "the cat", theBN, catBN);

        addInhibitoryLoop(m, inhibNThe, false, theBN);
        addInhibitoryLoop(m, createNeuron(new InhibitoryNeuron(), "I-the (tc)"), true, theBN);

        updateBias(theCatP, 3.0);

        updateBias(theBN, 3.0);
        updateBias(catBN, 3.0);
    }

    public static void initPatternBlackCat(Model m) {
        PatternNeuron blackIN = TokenNeuron.lookupToken(m, "black");
        PatternNeuron catIN = TokenNeuron.lookupToken(m, "cat");

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, -1, -1);

        BindingNeuron blackBN = createNeuron(new BindingNeuron(), "black (black cat)");
        new PrimaryInputSynapse()
                .init(blackIN, blackBN, 10.0)
                .adjustBias();

        BindingNeuron catBN = createNeuron(new BindingNeuron(), "cat (black cat)");
        new PrimaryInputSynapse()
                .init(catIN, catBN, 20.0)
                .adjustBias();

        new RelatedInputSynapse()
                .init(relPT, catBN, 5.0)
                .adjustBias();

        new SamePatternSynapse()
                .init(blackBN, catBN, 5.0)
                .adjustBias();

        PatternNeuron blackCat = initPatternLoop(m, "black cat", blackBN, catBN);
        updateBias(blackCat, 3.0);

        updateBias(blackBN, 3.0);
        updateBias(catBN, 3.0);
    }

    public static void initPatternTheDog(Model m, InhibitoryNeuron inhibNThe, InhibitoryNeuron inhibNDog, int variant) {
        PatternNeuron theIN = TokenNeuron.lookupToken(m, "the");
        PatternNeuron dogIN = TokenNeuron.lookupToken(m, "dog");

        int relFrom = variant < 2 ? -5 : 1;
        int relTo = variant < 2 ? -1 : 5;

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, relFrom, relTo);

        BindingNeuron theBN = createNeuron(new BindingNeuron(), "the (the dog)");
        new PrimaryInputSynapse()
                .init(theIN, theBN, 10.0)
                .adjustBias();

        BindingNeuron dogBN = createNeuron(new BindingNeuron(), "dog (the dog)");
        new PrimaryInputSynapse()
                .init(dogIN, dogBN, variant == 0  || variant == 2 ? 10.0 : 5.0)
                .adjustBias();

        if(variant < 2) {
            new RelatedInputSynapse()
                    .init(relPT, dogBN, 5.0)
                    .adjustBias();
            new SamePatternSynapse()
                    .init(theBN, dogBN, variant == 1 || variant == 3 ? 10.0 : 5.0)
                    .adjustBias();
        } else {
            new RelatedInputSynapse()
                    .init(relPT, theBN, 5.0)
                    .adjustBias();
            new SamePatternSynapse()
                    .init(dogBN, theBN, variant == 1 || variant == 3 ? 10.0 : 5.0)
                    .adjustBias();
        }

        PatternNeuron theDogP = initPatternLoop(m, "the dog", theBN, dogBN);

        addInhibitoryLoop(m, inhibNThe, false, theBN);
        addInhibitoryLoop(m, createNeuron(new InhibitoryNeuron(), "I-the (tg)"), true, theBN);

        updateBias(theDogP, 3.0);

        updateBias(theBN, 3.0);
        updateBias(dogBN, 3.0);
    }
}
