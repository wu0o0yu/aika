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

import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;
import network.aika.text.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class JacksonCookTest {

    @Test
    public void testJacksonCook()  {
        setupJacksonCookTest();
    }

    public void setupJacksonCookTest() {
        Model m = new Model();

        TokenNeuron jacksonIN = lookupToken(m, "Jackson");
        TokenNeuron cookIN = lookupToken(m, "Cook");

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, -1, -1);

        BindingNeuron forenameBN = new BindingNeuron().init(m, "forename (person name)");
        BindingNeuron surnameBN = new BindingNeuron().init(m, "surname (person name)");


        BindingNeuron jacksonForenameBN = forenameBN.instantiateTemplate().init(m, "jackson (forename)");
        BindingNeuron jacksonJCBN = jacksonForenameBN.instantiateTemplate().init(m, "jackson (jackson cook)");
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(jacksonIN, jacksonJCBN)
                .adjustBias();

        CategoryNeuron jacksonForenameCN = new BindingCategoryNeuron()
                .init(m, "jackson (forename)");
        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(jacksonJCBN, jacksonForenameCN);

        new BindingCategoryInputSynapse()
                .setWeight(10.0)
                .init(jacksonForenameCN, jacksonForenameBN);

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(jacksonIN, jacksonForenameBN)
                .adjustBias();

        CategoryNeuron forenameCN = new BindingCategoryNeuron()
                .init(m, "forename");

        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(jacksonForenameBN, forenameCN);

        BindingNeuron jacksonCityBN = new BindingNeuron()
                .init(m, "jackson (city)");
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(jacksonIN, jacksonCityBN)
                .adjustBias();

        CategoryNeuron cityCN = new BindingCategoryNeuron()
                .init(m, "city");

        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(jacksonCityBN, cityCN);

        BindingNeuron cookSurnameBN =  surnameBN.init(m, "cook (surname)");
        BindingNeuron cookJCBN =  cookSurnameBN.init(m, "cook (jackson cook)");
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(cookIN, cookJCBN)
                .adjustBias();

        CategoryNeuron cookSurnameCN = new BindingCategoryNeuron()
                .init(m, "cook (surname)");
        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(cookJCBN, cookSurnameCN);

        new BindingCategoryInputSynapse()
                .setWeight(10.0)
                .init(cookSurnameCN, cookSurnameBN);

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(cookIN, cookSurnameBN)
                .adjustBias();

        CategoryNeuron surnameCN =  new BindingCategoryNeuron()
                .init(m, "surname");
        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(cookSurnameBN, surnameCN);

        BindingNeuron cookProfessionBN =  new BindingNeuron()
                .init(m, "cook (profession)");
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(cookIN, cookProfessionBN)
                .adjustBias();

        CategoryNeuron professionCN = new BindingCategoryNeuron()
                .init(m, "profession");
        new BindingCategorySynapse()
                .setWeight(10.0)
                .init(cookProfessionBN, professionCN);

        addInhibitoryLoop(new InhibitoryNeuron(Scope.SAME).init(m, "I-jackson"), false, jacksonForenameBN, jacksonCityBN);
        addInhibitoryLoop(new InhibitoryNeuron(Scope.SAME).init(m, "I-cook"), false, cookSurnameBN, cookProfessionBN);

        setBias(jacksonJCBN, 2.0);
        setBias(jacksonForenameBN, 2.0);
        setBias(jacksonCityBN, 3.0);
        setBias(cookJCBN, 2.0);
        setBias(cookSurnameBN, 2.0);
        setBias(cookProfessionBN, 3.0);

        new BindingCategoryInputSynapse()
                .setWeight(10.0)
                .init(forenameCN, forenameBN);

        new BindingCategoryInputSynapse()
                .setWeight(10.0)
                .init(surnameCN, surnameBN);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(relPT, surnameBN)
                .adjustBias();

        new SamePatternSynapse()
                .setWeight(10.0)
                .init(forenameBN, surnameBN)
                .adjustBias();

        setBias(forenameBN, 2.0);
        setBias(surnameBN, 2.0);

        PatternNeuron jacksonCookPattern = initPatternLoop(m, "jackson cook", jacksonJCBN, cookJCBN);
        setBias(jacksonCookPattern, 3.0);

        PatternNeuron personNamePattern = initPatternLoop(m, "person name", forenameBN, surnameBN);
        setBias(personNamePattern, 3.0);

        Document doc = new Document(m, "Jackson Cook");

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(0.01)
                .setTrainingEnabled(true);
        doc.setConfig(c);

        processTokens(m, doc, List.of("Jackson", "Cook"));

        doc.postProcessing();
        doc.updateModel();
    }
}
