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
package network.aika.parser;

import network.aika.Config;
import network.aika.debugger.AIKADebugger;
import network.aika.text.Document;

import static network.aika.meta.LabelUtil.generateTemplateInstanceLabels;
import static network.aika.parser.ParserPhase.TRAINING;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class TrainingParser extends Parser {

    @Override
    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        Document doc = new Document(getTemplateModel().getModel(), txt);

        Config conf = new Config()
                .setAlpha(null)
                .setLearnRate(0.01)
                .setTrainingEnabled(phase == TRAINING)
                .setMetaInstantiationEnabled(phase == TRAINING)
                .setCountingEnabled(true);

        doc.setConfig(conf);

        return doc;
    }

    @Override
    public Document process(String txt, Context context, ParserPhase phase) {
        Document doc = super.process(txt, context, phase);

        doc.setInstantiationCallback(act ->
                generateTemplateInstanceLabels(act)
        );

        waitForClick(debugger);

        doc.instantiateTemplates();

        waitForClick(debugger);

        doc.train();

        waitForClick(debugger);

        doc.postProcessing();
        doc.updateModel();
        doc.disconnect();

        return doc;
    }
}
