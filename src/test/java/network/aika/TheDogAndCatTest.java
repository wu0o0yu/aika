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

        coords.put(1l, new double[]{2.674, 0.407});
        coords.put(2l, new double[]{2.354, 0.613});
        coords.put(3l, new double[]{3.216, 0.526});
        coords.put(4l, new double[]{2.389, -0.067});
        coords.put(5l, new double[]{2.939, -0.073});
        coords.put(6l, new double[]{1.81, -0.061});
        coords.put(7l, new double[]{3.521, -0.087});
        coords.put(8l, new double[]{1.822, 1.096});
        coords.put(9l, new double[]{2.309, 1.04});
        coords.put(10l, new double[]{1.848, 1.804});
        coords.put(11l, new double[]{2.974, 1.043});
        coords.put(12l, new double[]{3.505, 1.043});
        coords.put(13l, new double[]{3.278, 1.754});
        coords.put(14l, new double[]{2.487, 1.925});

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

        InhibitoryNeuron inhibNCat = initInhibitoryLoop(t, "cat");
        initPatternTheCat(t, inhibNCat, 0);
        initPatternTheDog(t);

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
