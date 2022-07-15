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
import static network.aika.TestHelper.initPatternTheDog;
import static network.aika.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class TheDogAndCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.327, -0.032});
        coords.put(1, new double[]{-0.1, -0.032});
        coords.put(2, new double[]{0.111, -0.036});
        coords.put(3, new double[]{0.345, -0.039});
        coords.put(4, new double[]{0.583, -0.047});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(8l, new double[]{1.564, 0.923});
        coords.put(9l, new double[]{2.818, 0.856});
        coords.put(11l, new double[]{1.62, -0.031});
        coords.put(11l, new double[]{1.62, -0.031});
        coords.put(13l, new double[]{2.089, -0.041});
        coords.put(13l, new double[]{2.089, -0.041});
        coords.put(15l, new double[]{2.854, -0.02});
        coords.put(15l, new double[]{2.854, -0.02});
        coords.put(16l, new double[]{1.814, 0.304});
        coords.put(17l, new double[]{2.196, 0.304});
        coords.put(18l, new double[]{2.523, 0.959});
        coords.put(20l, new double[]{2.574, -0.058});
        coords.put(20l, new double[]{2.574, -0.058});
        coords.put(21l, new double[]{1.435, 0.336});
        coords.put(22l, new double[]{2.658, 0.304});
        coords.put(23l, new double[]{2.133, 0.938});
        coords.put(25l, new double[]{3.2, 0.0});
        coords.put(25l, new double[]{3.2, 0.0});

        return coords;
    }

    @Test
    public void testTheDogAndTheCat()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
        );

        debugger.setCurrentTestCase(() ->
                setupTheDogAndTheCatTest(debugger)
        );
        debugger.run();
    }

    public void setupTheDogAndTheCatTest(AIKADebugger debugger) {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        m.setTemplateGraph(t);

        InhibitoryNeuron inhibNThe = initInhibitoryLoop(t, "the");
        InhibitoryNeuron inhibNCat = initInhibitoryLoop(t, "cat");
        initPatternTheCat(t, inhibNThe, inhibNCat, 0);
        initPatternTheDog(t, inhibNThe);

        Document doc = new Document(m, "the dog and the cat");
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
        camera.setViewPercent(1.8);
        camera.setViewCenter(0.385, 0.461, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.35);
        camera.setViewCenter(2.595, 0.808, 0);

        processTokens(t, doc, List.of("the", "dog", "and", "the", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
