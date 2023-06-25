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
import network.aika.elements.activations.TokenActivation;
import network.aika.meta.AbstractTemplateModel;
import network.aika.text.Document;
import network.aika.tokenizer.Tokenizer;


import static network.aika.parser.ParserPhase.TRAINING;
import static network.aika.steps.Phase.ANNEAL;
import static network.aika.steps.Phase.INFERENCE;
import static network.aika.steps.keys.QueueKey.MAX_ROUND;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Parser {

    public abstract Tokenizer getTokenizer();

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

    protected abstract AbstractTemplateModel getTemplateModel();

    protected AIKADebugger debugger = null;

    public Document process(String txt, Context context, ParserPhase phase) {
        Document doc = initDocument(txt, context, phase);

        infer(doc, context, phase);

        doc.disconnect();

        return doc;
    }

    protected void infer(Document doc, Context context, ParserPhase phase) {
        if(phase == ParserPhase.TRAINING) {
    //        debugger = AIKADebugger.createAndShowGUI(doc);
        }

        doc.setFeedbackTriggerRound();

        getTokenizer().tokenize(doc, context, (n, pos, begin, end) -> {
            TokenActivation tAct = doc.addToken(n, pos, begin, end);
            tAct.setNet(getTemplateModel().getInputPatternNetTarget());
        });

        doc.process(MAX_ROUND, INFERENCE);

        doc.anneal();

        doc.process(MAX_ROUND, ANNEAL);
    }


    protected static void waitForClick(AIKADebugger debugger) {
        if(debugger != null)
            debugger.getStepManager().waitForClick();
    }
}
