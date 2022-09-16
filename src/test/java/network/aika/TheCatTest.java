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
public class TheCatTest {

    public Map<Integer, double[]> getActCoordinateMap(int variant) {
        Map<Integer, double[]> coords = new TreeMap<>();

        switch (variant) {
            case 0: {
                coords.put(0, new double[]{-0.048, 0.008});
                coords.put(1, new double[]{0.688, -0.004});
                coords.put(2, new double[]{-0.052, 0.532});
                coords.put(3, new double[]{0.314, 0.964});
                coords.put(4, new double[]{-0.188, 0.954});
                coords.put(5, new double[]{0.337, 0.732});
                coords.put(6, new double[]{0.688, 0.545});
                coords.put(7, new double[]{0.289, 0.0});
                break;
            }
            case 1: {
                coords.put(0, new double[]{-0.048, 0.008});
                coords.put(1, new double[]{0.688, -0.004});
                coords.put(2, new double[]{-0.035, 0.49});
                coords.put(3, new double[]{0.314, 0.791});
                coords.put(4, new double[]{0.303, 0.007});
                coords.put(5, new double[]{-0.19, 0.728});
                coords.put(6, new double[]{0.687, 0.486});
                coords.put(7, new double[]{0.949, 0.734});
                break;
            }
            case 2: {
                coords.put(0, new double[]{-0.048, 0.008});
                coords.put(1, new double[]{0.688, -0.004});
                coords.put(2, new double[]{0.323, -0.004});
                coords.put(3, new double[]{-0.039, 0.538});
                coords.put(4, new double[]{0.69, 0.514});
                coords.put(5, new double[]{0.325, 0.771});
                coords.put(6, new double[]{0.89, 0.736});
                coords.put(7, new double[]{-0.252, 0.792});
                break;
            }
            case 3: {
                coords.put(0, new double[]{-0.048, 0.008});
                coords.put(1, new double[]{0.688, -0.004});
                coords.put(2, new double[]{0.69, 0.45});
                coords.put(3, new double[]{0.314, 0.791});
                coords.put(4, new double[]{0.327, 0.013});
                coords.put(5, new double[]{0.921, 0.703});
                coords.put(6, new double[]{-0.046, 0.448});
                coords.put(7, new double[]{-0.295, 0.818});
                break;
            }
        }

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap(int variant) {
        Map<Long, double[]> coords = new TreeMap<>();
        switch (variant) {
            case 0: {
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
                break;
            }
            case 1: {
                coords.put(1l, new double[]{0.223, 2.184});
                coords.put(2l, new double[]{-0.155, 1.125});
                coords.put(3l, new double[]{1.257, 2.85});
                coords.put(4l, new double[]{0.457, 1.111});
                coords.put(5l, new double[]{1.18, 2.231});
                coords.put(6l, new double[]{-0.556, 2.189});
                coords.put(7l, new double[]{0.213, 2.956});
                coords.put(8l, new double[]{4.135, 1.43});
                coords.put(9l, new double[]{1.878, -0.052});
                coords.put(10l, new double[]{1.878, -0.052});
                coords.put(11l, new double[]{3.542, -0.067});
                coords.put(12l, new double[]{3.542, -0.067});
                coords.put(13l, new double[]{2.639, -0.086});
                coords.put(14l, new double[]{2.639, -0.086});
                coords.put(15l, new double[]{1.872, 0.798});
                coords.put(16l, new double[]{3.557, 0.838});
                coords.put(17l, new double[]{2.777, 1.612});
                coords.put(18l, new double[]{1.334, 1.422});
                break;
            }
            case 2: {
                coords.put(1l, new double[]{0.223, 2.184});
                coords.put(2l, new double[]{-0.155, 1.125});
                coords.put(3l, new double[]{1.257, 2.85});
                coords.put(4l, new double[]{0.457, 1.111});
                coords.put(5l, new double[]{1.18, 2.231});
                coords.put(6l, new double[]{-0.556, 2.189});
                coords.put(7l, new double[]{-0.042, 2.634});
                coords.put(8l, new double[]{3.45, 1.704});
                coords.put(9l, new double[]{1.611, -0.098});
                coords.put(10l, new double[]{1.611, -0.098});
                coords.put(11l, new double[]{2.891, -0.083});
                coords.put(12l, new double[]{2.891, -0.083});
                coords.put(13l, new double[]{2.235, -0.085});
                coords.put(14l, new double[]{2.235, -0.085});
                coords.put(15l, new double[]{1.594, 1.179});
                coords.put(16l, new double[]{2.873, 1.127});
                coords.put(17l, new double[]{2.321, 1.714});
                coords.put(18l, new double[]{1.027, 1.694});
                break;
            }
            case 3: {
                coords.put(1l, new double[]{0.236, 2.207});
                coords.put(2l, new double[]{-0.155, 1.125});
                coords.put(3l, new double[]{1.257, 2.85});
                coords.put(4l, new double[]{0.457, 1.111});
                coords.put(5l, new double[]{1.18, 2.231});
                coords.put(6l, new double[]{-0.556, 2.189});
                coords.put(7l, new double[]{0.076, 2.68});
                coords.put(8l, new double[]{3.81, 1.456});
                coords.put(9l, new double[]{1.597, -0.01});
                coords.put(10l, new double[]{1.597, -0.01});
                coords.put(11l, new double[]{3.247, -0.015});
                coords.put(12l, new double[]{3.247, -0.015});
                coords.put(13l, new double[]{2.386, -0.037});
                coords.put(14l, new double[]{2.386, -0.037});
                coords.put(15l, new double[]{1.619, 1.093});
                coords.put(16l, new double[]{3.251, 1.071});
                coords.put(17l, new double[]{2.523, 1.657});
                coords.put(18l, new double[]{1.315, 1.649});
                break;
            }
        }
        return coords;
    }

    @Test
    public void testTheBlackCat()  {
        for(int variant = 0; variant < 4; variant++) {
            performTest(variant);
        }
    }

    private void performTest(int variant) {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
        );

        debugger.setCurrentTestCase(() ->
                setupTheCatTest(debugger, variant)
        );
        debugger.run();
    }

    public void setupTheCatTest(AIKADebugger debugger, int variant) {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        m.setTemplateGraph(t);

        InhibitoryNeuron inhibNThe = createNeuron(t.INHIBITORY_TEMPLATE, "I-the");
        InhibitoryNeuron inhibNCat = createNeuron(t.INHIBITORY_TEMPLATE, "I-cat");
        initPatternTheCat(t, inhibNThe, inhibNCat, variant);

        Document doc = new Document(m, "the cat");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap(variant);
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap(variant);
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.85);
        camera.setViewCenter(0.385, 0.461, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.5);
        camera.setViewCenter(1.528, 0.828, 0);

        processTokens(t, doc, List.of("the", "cat"));

        doc.postProcessing();
        doc.updateModel();
    }
}
