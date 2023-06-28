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

import network.aika.callbacks.ActivationCheckCallback;
import network.aika.callbacks.InstantiationCallback;
import network.aika.elements.activations.Activation;
import network.aika.text.Document;

import static network.aika.meta.LabelUtil.generateTemplateInstanceLabels;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class TrainingParser extends Parser implements ActivationCheckCallback, InstantiationCallback {

    @Override
    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        Document doc = super.initDocument(txt, context, phase);
        doc.setActivationCheckCallback(this);
        doc.setInstantiationCallback(this);

        return doc;
    }

    @Override
    public boolean check(Activation iAct) {
        return getTemplateModel().evaluatePrimaryBindingActs(iAct);
    }

    public void onInstantiation(Activation act) {
        generateTemplateInstanceLabels(act);
    }

    @Override
    public Document process(String txt, Context context, ParserPhase phase) {
        Document doc = initDocument(txt, context, phase);

        infer(doc, context, phase);
        anneal(doc);
        train(doc);

        doc.disconnect();

        return doc;
    }

    protected void train(Document doc) {
        waitForClick(debugger);

        doc.instantiateTemplates();

        waitForClick(debugger);

        doc.train();

        waitForClick(debugger);

        doc.postProcessing();
        doc.updateModel();
    }
}
