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
import network.aika.neuron.disjunctive.CategoryNeuron;
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
public class TheBlackCatTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.061, 0.005});
        coords.put(1, new double[]{0.053, 0.205});
        coords.put(2, new double[]{-0.024, 0.53});
        coords.put(3, new double[]{0.301, 0.743});
        coords.put(4, new double[]{-0.167, 0.405});
        coords.put(5, new double[]{0.112, 0.4});
        coords.put(6, new double[]{0.338, -0.008});
        coords.put(7, new double[]{0.336, 0.196});
        coords.put(8, new double[]{0.505, 0.871});
        coords.put(9, new double[]{0.688, 1.078});
        coords.put(10, new double[]{0.272, 0.405});
        coords.put(11, new double[]{0.505, 0.396});
        coords.put(12, new double[]{0.78, -0.023});
        coords.put(13, new double[]{0.776, 0.177});
        coords.put(14, new double[]{0.686, 0.532});
        coords.put(15, new double[]{0.901, 0.862});
        coords.put(16, new double[]{0.686, 0.732});
        coords.put(17, new double[]{0.901, 1.062});
        coords.put(18, new double[]{0.628, 0.394});
        coords.put(19, new double[]{0.964, 0.383});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{2.087, 0.379});
        coords.put(2l, new double[]{0.225, 0.877});
        coords.put(3l, new double[]{3.554, 0.733});
        coords.put(4l, new double[]{0.628, -0.12});
        coords.put(5l, new double[]{1.62, -0.151});
        coords.put(6l, new double[]{2.731, -0.173});
        coords.put(7l, new double[]{0.636, 1.511});
        coords.put(8l, new double[]{3.134, 1.505});
        coords.put(9l, new double[]{1.29, 2.76});
        coords.put(10l, new double[]{1.632, 1.52});
        coords.put(11l, new double[]{2.571, 1.491});
        coords.put(12l, new double[]{2.324, 2.755});

        return coords;
    }

    @Test
    public void testJacksonCook()  {
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
        PatternNeuron blackIN = m.lookupToken("black");
        PatternNeuron catIN = m.lookupToken("cat");

        BindingNeuron theBN = createNeuron(t.BINDING_TEMPLATE, "the (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, theIN, theBN, 10.0);
        BindingNeuron catBN1 = createNeuron(t.BINDING_TEMPLATE, "cat (the cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN1, 10.0);
        PatternNeuron theCat = initPatternLoop(t, "the cat", theBN, catBN1);
        updateBias(theCat, 3.0);

        BindingNeuron blackBN = createNeuron(t.BINDING_TEMPLATE, "black (black cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, blackIN, blackBN, 10.0);
        BindingNeuron catBN2 = createNeuron(t.BINDING_TEMPLATE, "cat (black cat)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, catIN, catBN2, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, m.getPreviousTokenRelationBindingNeuron(), catBN2, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, blackBN, catBN2, 10.0);

        PatternNeuron blackCat = initPatternLoop(t, "black cat", blackBN, catBN2);
        updateBias(blackCat, 3.0);

        updateBias(theBN, 3.0);
        updateBias(blackBN, 3.0);
        updateBias(catBN1, 3.0);
        updateBias(catBN2, 3.0);


//        initInhibitoryLoop(t, "jackson", jacksonForenameBN, jacksonCityBN);


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

        doc.processTokens(List.of("the", "black", "cat"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
