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
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.utils.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class TheCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.061, 0.005});
        coords.put(1, new double[]{0.053, 0.205});
        coords.put(2, new double[]{-0.036, 0.862});
        coords.put(3, new double[]{0.116, 1.07});
        coords.put(4, new double[]{-0.167, 0.405});
        coords.put(5, new double[]{0.112, 0.405});
        coords.put(6, new double[]{0.338, -0.008});
        coords.put(7, new double[]{0.336, 0.196});
        coords.put(8, new double[]{0.403, 0.595});
        coords.put(9, new double[]{0.577, 0.799});
        coords.put(10, new double[]{0.272, 0.405});
        coords.put(11, new double[]{0.505, 0.396});
        coords.put(12, new double[]{0.78, -0.023});
        coords.put(13, new double[]{0.731, 0.188});
        coords.put(14, new double[]{0.873, 0.835});
        coords.put(15, new double[]{0.901, 1.067});
        coords.put(16, new double[]{0.621, 0.396});
        coords.put(17, new double[]{0.927, 0.388});
        coords.put(18, new double[]{0.713, 0.596});
        coords.put(19, new double[]{0.476, 1.029});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{2.087, 0.379});
        coords.put(2l, new double[]{0.201, 0.546});
        coords.put(3l, new double[]{3.554, 0.733});
        coords.put(4l, new double[]{0.628, -0.12});
        coords.put(5l, new double[]{1.62, -0.151});
        coords.put(6l, new double[]{2.946, -0.166});
        coords.put(7l, new double[]{0.653, 2.31});
        coords.put(8l, new double[]{3.204, 2.28});
        coords.put(9l, new double[]{1.651, 3.336});
        coords.put(10l, new double[]{1.632, 1.52});
        coords.put(11l, new double[]{2.76, 1.513});
        coords.put(12l, new double[]{2.17, 2.305});
        coords.put(13l, new double[]{0.671, 3.305});

        return coords;
    }

    @Test
    public void testTheBlackCat()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
        );

        debugger.setCurrentTestCase(() ->
                setupTheBlackCatTest(debugger)
        );
        debugger.run();
    }

    public void setupTheBlackCatTest(AIKADebugger debugger) {
        TextModel m = new TextModel();

        m.init();
        Templates t = m.getTemplates();

        PatternNeuron theIN = m.lookupToken("the");
        PatternNeuron catIN = m.lookupToken("cat");

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);
        BindingNeuron catBN1 = createNeuron(t.BINDING_TEMPLATE, "cat (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN1, 10.0);
        PatternNeuron theCat = initPatternLoop(t, "the cat", theBN, catBN1);
        updateBias(theCat, 3.0);


        updateBias(theBN, 3.0);
        updateBias(catBN1, 3.0);


//        initInhibitoryLoop(t, "jackson", jacksonForenameBN, jacksonCityBN);


        Document doc = new Document(m, "the cat");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true)
                .setTemplatesEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.65);
        camera.setViewCenter(0.385, 0.461, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.3);
        camera.setViewCenter(1.921, 1.449, 0);

        doc.processTokens(List.of("the", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
