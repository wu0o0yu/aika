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
        coords.put(1, new double[]{-0.023, -0.028});
        coords.put(2, new double[]{0.219, -0.035});
        coords.put(3, new double[]{0.345, -0.039});
        coords.put(4, new double[]{0.583, -0.047});
        coords.put(5, new double[]{-0.104, 0.386});
        coords.put(6, new double[]{-0.248, 0.623});
        coords.put(7, new double[]{-0.339, 0.388});
        coords.put(8, new double[]{-0.224, -0.028});
        coords.put(9, new double[]{-0.38, 0.636});
        coords.put(10, new double[]{-0.53, 0.607});
        coords.put(11, new double[]{0.587, 0.305});
        coords.put(12, new double[]{0.582, 0.587});
        coords.put(13, new double[]{0.137, 0.401});
        coords.put(14, new double[]{0.344, 0.3});
        coords.put(15, new double[]{-0.122, -0.03});
        coords.put(16, new double[]{0.114, -0.03});
        coords.put(17, new double[]{0.002, 0.649});
        coords.put(18, new double[]{0.176, 0.653});
        coords.put(19, new double[]{0.471, 0.644});
        coords.put(20, new double[]{0.331, 0.664});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(8l, new double[]{0.818, 1.482});
        coords.put(9l, new double[]{2.56, 1.447});
        coords.put(10l, new double[]{1.938, 0.764});
        coords.put(12l, new double[]{0.6, 0.0});
        coords.put(12l, new double[]{0.6, 0.0});
        coords.put(14l, new double[]{2.323, -0.061});
        coords.put(14l, new double[]{2.323, -0.061});
        coords.put(16l, new double[]{1.007, -0.037});
        coords.put(16l, new double[]{1.007, -0.037});
        coords.put(17l, new double[]{0.6, 1.032});
        coords.put(18l, new double[]{2.337, 1.032});
        coords.put(19l, new double[]{1.605, 1.496});
        coords.put(21l, new double[]{1.612, -0.082});
        coords.put(21l, new double[]{1.612, -0.082});
        coords.put(22l, new double[]{1.007, 0.447});
        coords.put(23l, new double[]{1.626, 0.427});
        coords.put(24l, new double[]{1.371, 0.681});
        coords.put(26l, new double[]{3.4, 0.0});
        coords.put(26l, new double[]{3.4, 0.0});

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
        Model m = new Model();

        InhibitoryNeuron inhibNThe = new InhibitoryNeuron()
                .init(m, "I-the");

        InhibitoryNeuron inhibNCat = new InhibitoryNeuron()
                .init(m, "I-cat");

        InhibitoryNeuron inhibNDog = new InhibitoryNeuron()
                .init(m, "I-dog");

        initPatternTheCat(m, inhibNThe, inhibNCat, 3);
        initPatternTheDog(m, inhibNThe, inhibNDog, 3);

        Document doc = new Document(m, "the dog and the cat");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setTrainingEnabled(false);
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

        processTokens(m, doc, List.of("the", "dog", "and", "the", "cat"));


        doc.postProcessing();
        doc.updateModel();
    }
}
