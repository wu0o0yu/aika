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

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.text.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static network.aika.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPatternPos() {
        SimpleTemplateGraph tg = new SimpleTemplateGraph();
        Model m = initModel();
        tg.init(m);

        Document doc = new Document(m, "ABC");

        doc.setConfig(
                TestUtils.getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.011)
                        .setTrainingEnabled(false)
        );

        AIKADebugger.createAndShowGUI(doc);

        processTokens(tg.TOKEN_TEMPLATE, doc, List.of("A", "B", "C"));

        System.out.println(doc);

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }

    @Test
    public void testPatternNeg() {
        SimpleTemplateGraph tg = new SimpleTemplateGraph();
        Model m = initModel();
        tg.init(m);

        Document doc = new Document(m, "ABC");

        addToken(tg.TOKEN_TEMPLATE, doc, "A", 0, 0, 1);
        addToken(tg.TOKEN_TEMPLATE, doc, "B", 1, 1, 2);

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }

    public Model initModel() {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        t.init(m);

        TokenNeuron nA = t.TOKEN_TEMPLATE.lookupToken("A");
        TokenNeuron nB = t.TOKEN_TEMPLATE.lookupToken("B");
        TokenNeuron nC = t.TOKEN_TEMPLATE.lookupToken( "C");

        BindingNeuron eA = createNeuron(t.BINDING_TEMPLATE, "E A");
        BindingNeuron eB = createNeuron(t.BINDING_TEMPLATE, "E B");
        BindingNeuron eC = createNeuron(t.BINDING_TEMPLATE, "E C");

        PatternNeuron out = createNeuron(t.PATTERN_TEMPLATE, "OUT");
        out.setTokenLabel("ABC");

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(-1, -1);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, nA, eA, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE, out, eA, 10.0);
        updateBias(eA, 4.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, nB, eB, 10.0);
        createSynapse( t.SAME_PATTERN_SYNAPSE_TEMPLATE, eA, eB, 10.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, eB, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE, out, eB, 10.0);
        updateBias(eB, 4.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, nC, eC, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, eB, eC, 10.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, eC, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_FROM_PATTERN_TEMPLATE, out, eC, 10.0);

        updateBias(eC, 4.0);

        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eA, out, 10.0);
        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eB, out, 10.0);
        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eC, out, 10.0);

        updateBias(out,4.0);

        return m;
    }
}
