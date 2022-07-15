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
public class TheBlackCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.061, 0.005});
        coords.put(1, new double[]{0.336, 0.001});
        coords.put(2, new double[]{0.768, -0.015});
        coords.put(3, new double[]{-0.046, 0.617});
        coords.put(4, new double[]{0.331, 0.855});
        coords.put(5, new double[]{-0.194, 0.728});
        coords.put(6, new double[]{0.78, 0.617});
        coords.put(7, new double[]{0.124, 0.018});
        coords.put(8, new double[]{0.979, 0.757});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(8l, new double[]{3.377, 1.989});
        coords.put(10l, new double[]{1.507, 0.08});
        coords.put(10l, new double[]{1.507, 0.08});
        coords.put(12l, new double[]{3.112, 0.053});
        coords.put(12l, new double[]{3.112, 0.053});
        coords.put(14l, new double[]{1.805, 0.097});
        coords.put(14l, new double[]{1.805, 0.097});
        coords.put(15l, new double[]{1.512, 1.412});
        coords.put(16l, new double[]{3.08, 1.468});
        coords.put(17l, new double[]{2.374, 2.162});
        coords.put(18l, new double[]{1.254, 1.985});
        coords.put(20l, new double[]{2.161, 0.072});
        coords.put(20l, new double[]{2.161, 0.072});
        coords.put(22l, new double[]{2.581, 0.072});
        coords.put(22l, new double[]{2.581, 0.072});
        coords.put(23l, new double[]{2.438, 0.69});
        coords.put(24l, new double[]{2.899, 0.696});
        coords.put(25l, new double[]{2.679, 1.315});

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
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        m.setTemplateGraph(t);

        InhibitoryNeuron inhibNCat = initInhibitoryLoop(t, "cat");
        initPatternTheCat(t, inhibNCat, 0);
 //       initPatternBlackCat(t);

        Document doc = new Document(m, "the black cat");
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

        processTokens(t, doc, List.of("the", "black", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
