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
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestHelper.initPatternTheCat;
import static network.aika.utils.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class TheCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.061, 0.005});
        coords.put(1, new double[]{0.59, -0.015});
        coords.put(2, new double[]{0.092, 0.205});
        coords.put(3, new double[]{-0.053, 0.649});
        coords.put(4, new double[]{0.338, 1.016});
        coords.put(5, new double[]{0.147, 0.387});
        coords.put(6, new double[]{0.435, 0.194});
        coords.put(7, new double[]{0.617, 0.647});
        coords.put(8, new double[]{0.357, 0.39});
        coords.put(9, new double[]{0.476, 0.311});
        coords.put(10, new double[]{0.272, 0.847});
        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{1.766, 0.523});
        coords.put(2l, new double[]{1.775, 0.778});
        coords.put(3l, new double[]{0.722, -0.135});
        coords.put(4l, new double[]{2.772, -0.127});
        coords.put(5l, new double[]{0.755, 1.227});
        coords.put(6l, new double[]{2.791, 1.215});
        coords.put(7l, new double[]{1.8, 2.093});

        return coords;
    }

    @Test
    public void testTheBlackCat()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
        );

        debugger.setCurrentTestCase(() ->
                setupTheCatTest(debugger)
        );
        debugger.run();
    }

    public void setupTheCatTest(AIKADebugger debugger) {
        TextModel m = new TextModel();

        m.init();
        Templates t = m.getTemplates();

        initPatternTheCat(m, t);

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
        camera.setViewPercent(1.85);
        camera.setViewCenter(0.385, 0.461, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.3);
        camera.setViewCenter(1.809, 0.923, 0);

        doc.processTokens(List.of("the", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
