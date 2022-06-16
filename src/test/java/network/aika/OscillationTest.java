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
import network.aika.neuron.Templates;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

import static network.aika.utils.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class OscillationTest {

    @Test
    public void oscillationTest() {
        TextModel m = new TextModel();
        Templates t = m.getTemplates();

        m.setN(912);

        Document doc = new Document(m, "A ");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );

        PatternNeuron nA = createNeuron(t.PATTERN_TEMPLATE, "P-A");

        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPosition(899l);

        BindingNeuron nPPA = createNeuron(t.BINDING_TEMPLATE, "B-A");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, nA, nPPA, 0.3);

        AIKADebugger.createAndShowGUI(doc);

        doc.addToken(nA, 0, 1);
        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println();
    }
}
