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
public class TheDogAndCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{0.0, 0.0});
        coords.put(1, new double[]{0.026, 0.212});
        coords.put(2, new double[]{-0.132, 0.519});
        coords.put(3, new double[]{0.232, 0.532});
        coords.put(4, new double[]{-0.179, 0.728});
        coords.put(5, new double[]{0.206, 0.732});
        coords.put(6, new double[]{-0.034, 0.412});
        coords.put(7, new double[]{0.086, 0.427});
        coords.put(8, new double[]{-0.023, 0.897});
        coords.put(9, new double[]{0.305, 0.864});
        coords.put(10, new double[]{0.459, -0.001});
        coords.put(11, new double[]{0.448, 0.227});
        coords.put(12, new double[]{0.586, 0.552});
        coords.put(13, new double[]{0.574, 0.848});
        coords.put(14, new double[]{0.502, 0.414});
        coords.put(15, new double[]{0.622, 0.414});
        coords.put(16, new double[]{0.764, 0.0});
        coords.put(17, new double[]{0.764, 0.2});
        coords.put(18, new double[]{0.704, 0.427});
        coords.put(19, new double[]{0.824, 0.405});
        coords.put(20, new double[]{0.995, -0.002});
        coords.put(21, new double[]{1.025, 0.205});
        coords.put(22, new double[]{0.822, 0.62});
        coords.put(23, new double[]{0.983, 0.606});
        coords.put(24, new double[]{0.879, 0.9});
        coords.put(25, new double[]{1.065, 0.904});
        coords.put(26, new double[]{0.904, 0.412});
        coords.put(27, new double[]{1.024, 0.412});
        coords.put(28, new double[]{0.798, 0.908});
        coords.put(29, new double[]{0.935, 0.949});
        coords.put(30, new double[]{1.195, 0.0});
        coords.put(31, new double[]{1.195, 0.2});
        coords.put(32, new double[]{1.135, 0.405});
        coords.put(33, new double[]{1.255, 0.4});
        coords.put(34, new double[]{1.203, 0.612});
        coords.put(35, new double[]{1.195, 0.812});

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
        TextModel m = new TextModel();

        m.init();
        Templates t = m.getTemplates();


        PatternNeuron theIN = m.lookupToken("the");
        PatternNeuron andIN = m.lookupToken("and");
        PatternNeuron dogIN = m.lookupToken("dog");
        PatternNeuron catIN = m.lookupToken("cat");

        BindingNeuron theBN1 = createNeuron(t.BINDING_TEMPLATE, "the (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN1, 10.0);
        BindingNeuron dogBN = createNeuron(t.BINDING_TEMPLATE, "dog (the dog)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, dogIN, dogBN, 10.0);
        PatternNeuron theDog = initPatternLoop(t, "the dog", theBN1, dogBN);
        updateBias(theDog, 3.0);

        BindingNeuron theBN2 = createNeuron(t.BINDING_TEMPLATE, "the (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN2, 10.0);
        BindingNeuron catBN = createNeuron(t.BINDING_TEMPLATE, "cat (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN, 20.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, m.getPreviousTokenRelation(), catBN, 5.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, theBN2, catBN, 5.0);

        PatternNeuron theCat = initPatternLoop(t, "the cat", theBN2, catBN);
        updateBias(theCat, 3.0);

        updateBias(theBN1, 3.0);
        updateBias(dogBN, 3.0);
        updateBias(theBN2, 3.0);
        updateBias(catBN, 3.0);

        initInhibitoryLoop(t, "the", theBN1, theBN2);

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

        doc.processTokens(List.of("the", "dog", "and", "the", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
