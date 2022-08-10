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
import network.aika.neuron.activation.BindingActivation;
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
import static network.aika.steps.Phase.PROCESSING;


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
        coords.put(5, new double[]{-0.104, 0.386});
        coords.put(6, new double[]{-0.217, 0.629});
        coords.put(7, new double[]{-0.326, 0.384});
        coords.put(8, new double[]{-0.107, 0.535});
        coords.put(9, new double[]{-0.217, -0.036});
        coords.put(10, new double[]{-0.333, 0.527});
        coords.put(11, new double[]{0.587, 0.305});
        coords.put(12, new double[]{0.456, 0.514});
        coords.put(13, new double[]{0.252, 0.379});
        coords.put(14, new double[]{0.344, 0.3});
        coords.put(15, new double[]{0.711, 0.492});
        coords.put(16, new double[]{-0.008, -0.026});
        coords.put(17, new double[]{0.23, -0.026});

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
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        m.setTemplateGraph(t);

        InhibitoryNeuron inhibNThe = initInhibitoryLoop(t, "the");
        InhibitoryNeuron inhibNCat = initInhibitoryLoop(t, "cat");
        InhibitoryNeuron inhibNDog = initInhibitoryLoop(t, "dog");
        initPatternTheCat(t, inhibNThe, inhibNCat, 3);
        initPatternTheDog(t, inhibNThe, inhibNDog, 3);

        Document doc = new Document(m, "the dog and the cat");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true)
                .setTemplatesEnabled(false);
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

        for(double x = 1.0; x >= 0.0; x -= 0.05) {
            final double xFinal = x;
            doc.getActivations().stream()
                    .filter(act -> act instanceof BindingActivation)
                    .map(act -> (BindingActivation)act)
                    .forEach(act -> act.getIsOpen().setValue(xFinal));

            doc.process(PROCESSING);
        }

        doc.postProcessing();
        doc.updateModel();
    }
}
