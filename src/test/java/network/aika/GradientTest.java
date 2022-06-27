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
import network.aika.neuron.Neuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static network.aika.TestUtils.getConfig;
import static network.aika.TestUtils.setStatistic;

/**
 *
 * @author Lukas Molzberger
 */
public class GradientTest {

    @Test
    public void gradientAndInduction2() {
        TextModel m = new TextModel();

        m.setN(912);

        Document doc = new Document(m, "A B ");
        doc.setConfig(
                getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setTrainingEnabled(true)
        );
/*
        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setOffset(899l);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setOffset(739l);
*/

        AIKADebugger.createAndShowGUI(doc);

        processDoc(doc);

        doc.processFinalMode();
        doc.postProcessing();

        System.out.println();
    }


    @Test
    public void gradientAndInduction3() {
        TextModel m = new TextModel();
        m.setN(912);

        Document doc = new Document(m, "A B C ");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );

        processDoc(doc);

        Neuron nA = m.getNeuron("A");
        setStatistic(nA, 53.0,299,899l);

        Neuron nB = m.getNeuron("B");
        setStatistic(nB, 10.0, 121, 739l);

        Neuron nC = m.getNeuron("C");
        setStatistic(nC, 30.0, 234, 867l);


        AIKADebugger.createAndShowGUI(doc);

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println();
    }


    @Test
    public void gradientAndInduction2With2Docs() {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        TextModel m = new TextModel();
        m.setTemplateGraph(t);

        m.setN(912);
        t.BINDING_TEMPLATE.getBias().receiveUpdate(-0.32);

        Document doc1 = new Document(m, "A B ");
        doc1.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );
        processDoc(doc1);

        Neuron nA = m.getNeuron("A");
        setStatistic(nA, 53.0, 299, 899l);

        Neuron nB = m.getNeuron("B");
        setStatistic(nB, 10.0, 121, 739l);

        AIKADebugger.createAndShowGUI(doc1);

        doc1.processFinalMode();
        doc1.postProcessing();
        doc1.updateModel();

        Document doc2 = new Document(m, "A C ");
        doc2.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );
        processDoc(doc2);

        Neuron nC = m.getNeuron("C");
        setStatistic(nC, 30.0, 234, 867l);

        AIKADebugger.createAndShowGUI(doc2);

        doc2.processFinalMode();
        doc2.postProcessing();
        doc2.updateModel();

        System.out.println();
    }

    private void processDoc(Document doc) {
        doc.processTokens(Arrays.asList(doc.getContent().split(" ")));
    }
}
