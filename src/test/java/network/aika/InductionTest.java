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

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.text.Document;
import network.aika.neuron.activation.text.TokenActivation;
import org.junit.jupiter.api.Test;

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        t.init(m);

        TokenNeuron in = t.TOKEN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("A");
        in.setNetworkInput(true);
        in.setLabel("IN");

        in.setFrequency(12);

        in.getSampleSpace().setN(200);
        m.setN(200);

        Document doc = new Document(m, "");

        doc.addToken(in, 0, 0, 1);

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }

    @Test
    public void initialGradientTest() {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        t.init(m);

        PatternNeuron inA = createNeuron(t.PATTERN_TEMPLATE, "IN-A");
        PatternNeuron inB = createNeuron(t.PATTERN_TEMPLATE, "IN-B");
        BindingNeuron targetN = createNeuron(t.BINDING_TEMPLATE, "OUT-Target");

        targetN.getBias().setValue(0.0);

        Synapse sA = createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, inA, targetN, 0.1);
        Synapse sB = createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, inB, targetN, 0.0);


        m.setN(100);
        inA.setFrequency(10.0);
        inB.setFrequency(10.0);

        targetN.getSampleSpace().setN(100);
        targetN.setFrequency(0.0);

        System.out.println();

        // -----------------------------------------

        Document doc = new Document(m, "");

        Activation actA = inA.createActivation(doc);
        Activation actB = inB.createActivation(doc);
        Activation actTarget = targetN.createActivation(doc);

 //       sA.createLink(actA, actTarget, false);
 //       sB.createLink(actB, actTarget, false);

     //   actTarget.updateEntropyGradient();
 //       actTarget.computeInitialLinkGradients();
    }

    @Test
    public void inductionTest() throws InterruptedException {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model model = new Model();
        t.init(model);

        String phrase = "der Hund";

        Document doc = new Document(model, phrase);
        doc.setConfig(
                TestUtils.getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );
        System.out.println("  " + phrase);

        TokenActivation actDer = addToken(t, doc, "der", 0, 0, 4);
        TokenActivation actHund = addToken(t, doc, "Hund", 1, 4, 8);

        model.setN(1000);
        actDer.getNeuron().setFrequency(50);
        actDer.getNeuron().getSampleSpace().setN(1000);
        actHund.getNeuron().setFrequency(10);
        actHund.getNeuron().getSampleSpace().setN(1000);

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }
}
