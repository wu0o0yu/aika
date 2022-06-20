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

import network.aika.callbacks.FSSuspensionCallback;
import network.aika.debugger.AIKADebugger;
import network.aika.debugger.stepmanager.StepMode;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class TestFsModel {

    @Test
    public void testOpenModel() throws IOException {

        TextModel m = new TextModel(
                new FSSuspensionCallback(new File("/Users/lukas.molzberger/models").toPath(), "AIKA-2.0-10", true)
        );

        m.open(false);
        m.init();

        {
            Document doc = generateDocument(m, "arbeit fair arbeitsvermittlung ", true);

            AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);
            debugger.setStepMode(StepMode.ACT);

            doc.processFinalMode();
            doc.postProcessing();
            doc.updateModel();
        }


        {
            Document doc = generateDocument(m, "arbeit fair arbeitsvermittlung ", false);

            AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);
            debugger.setStepMode(StepMode.ACT);

            doc.processFinalMode();
            doc.postProcessing();
            doc.updateModel();
        }

        m.close();
    }

    private Document generateDocument(TextModel m, String txt, boolean train) {
        Document doc = new Document(m, txt);

        Config c = TestUtils.getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setTrainingEnabled(train);
        doc.setConfig(c);

        int i = 0;
        int pos = 0;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            doc.addToken(t, pos++, i, j);
            i = j + 1;
        }
        return doc;
    }
}
