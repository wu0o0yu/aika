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
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestHelper.initPatternTheCat;
import static network.aika.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class TheCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.048, 0.008});
        coords.put(1, new double[]{0.688, -0.004});
        coords.put(2, new double[]{-0.035, 0.49});
        coords.put(3, new double[]{0.314, 0.791});
        coords.put(4, new double[]{-0.254, 0.756});
        coords.put(5, new double[]{0.289, 0.003});
        coords.put(6, new double[]{0.687, 0.486});
        coords.put(7, new double[]{0.949, 0.734});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{0.223, 2.184});
        coords.put(2l, new double[]{-0.155, 1.125});
        coords.put(3l, new double[]{1.257, 2.85});
        coords.put(4l, new double[]{0.457, 1.111});
        coords.put(5l, new double[]{1.18, 2.231});
        coords.put(6l, new double[]{-0.556, 2.189});
        coords.put(7l, new double[]{3.484, 1.506});
        coords.put(8l, new double[]{1.4, 0.0});
        coords.put(9l, new double[]{1.4, 0.0});
        coords.put(10l, new double[]{2.768, -0.037});
        coords.put(11l, new double[]{2.768, -0.037});
        coords.put(12l, new double[]{2.04, -0.021});
        coords.put(13l, new double[]{2.04, -0.021});
        coords.put(14l, new double[]{1.397, 0.772});
        coords.put(15l, new double[]{2.766, 0.805});
        coords.put(16l, new double[]{2.261, 1.627});
        coords.put(17l, new double[]{1.028, 1.656});

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
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        TextModel m = new TextModel();
        m.setTemplateGraph(t);

        InhibitoryNeuron inhibNCat = initInhibitoryLoop(t, "cat");
        initPatternTheCat(m, t, inhibNCat, 2);

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
        camera.setViewPercent(1.5);
        camera.setViewCenter(1.528, 0.828, 0);

        doc.processTokens(List.of("the", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
