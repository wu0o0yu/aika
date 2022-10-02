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
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class JacksonCookTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.084, -0.003});
        coords.put(1, new double[]{2.492, -0.001});
        coords.put(2, new double[]{0.436, 0.571});
        coords.put(3, new double[]{-0.396, 0.558});
        coords.put(4, new double[]{-0.813, 1.197});
        coords.put(5, new double[]{-0.443, 1.202});
        coords.put(6, new double[]{0.875, 1.183});
        coords.put(7, new double[]{0.415, 1.189});
        coords.put(8, new double[]{0.879, 1.837});
        coords.put(9, new double[]{1.188, 2.087});
        coords.put(10, new double[]{2.005, 0.562});
        coords.put(11, new double[]{3.026, 0.558});
        coords.put(12, new double[]{3.531, 1.143});
        coords.put(13, new double[]{3.014, 1.168});
        coords.put(14, new double[]{1.622, 1.181});
        coords.put(15, new double[]{1.992, 1.185});
        coords.put(16, new double[]{1.602, 1.827});
        coords.put(17, new double[]{1.04, -0.055});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{-16.014, 16.317});
        coords.put(2l, new double[]{-11.343, 20.822});
        coords.put(3l, new double[]{-18.834, 20.237});
        coords.put(4l, new double[]{-7.751, 16.925});
        coords.put(5l, new double[]{-17.996, 12.545});
        coords.put(6l, new double[]{-22.238, 15.136});
        coords.put(7l, new double[]{-12.898, 13.173});
        coords.put(9l, new double[]{-4.209, -2.352});
        coords.put(9l, new double[]{-4.209, -2.352});
        coords.put(11l, new double[]{10.361, -2.811});
        coords.put(11l, new double[]{10.361, -2.811});
        coords.put(13l, new double[]{1.461, -2.43});
        coords.put(13l, new double[]{1.461, -2.43});
        coords.put(14l, new double[]{0.189, 13.082});
        coords.put(15l, new double[]{5.911, 13.167});
        coords.put(16l, new double[]{-2.142, 7.149});
        coords.put(17l, new double[]{-2.099, 0.919});
        coords.put(18l, new double[]{-2.197, 3.833});
        coords.put(19l, new double[]{-2.142, 10.158});
        coords.put(20l, new double[]{-7.525, 6.628});
        coords.put(21l, new double[]{-9.388, 9.721});
        coords.put(22l, new double[]{7.183, 6.81});
        coords.put(23l, new double[]{7.225, 1.046});
        coords.put(24l, new double[]{7.267, 3.801});
        coords.put(25l, new double[]{7.225, 10.116});
        coords.put(26l, new double[]{12.675, 5.113});
        coords.put(27l, new double[]{15.058, 7.536});
        coords.put(28l, new double[]{-4.433, 11.659});
        coords.put(29l, new double[]{10.066, 9.785});
        coords.put(30l, new double[]{3.241, 17.871});

        return coords;
    }

    @Test
    public void testJacksonCook()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
                118l,
                441l
        );

        debugger.setCurrentTestCase(() ->
                setupJacksonCookTest(debugger)
        );
        debugger.run();
    }

    public void setupJacksonCookTest(AIKADebugger debugger) {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        t.init(m);

        TokenNeuron jacksonIN = t.TOKEN_TEMPLATE.lookupToken("Jackson");
        TokenNeuron cookIN = t.TOKEN_TEMPLATE.lookupToken("Cook");

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(-1, -1);

        BindingNeuron forenameBN = createNeuron(t.BINDING_TEMPLATE, "forename (person name)");
        BindingNeuron surnameBN = createNeuron(t.BINDING_TEMPLATE, "surname (person name)");


        BindingNeuron jacksonForenameBN = createNeuron(forenameBN, "jackson (forename)");
        BindingNeuron jacksonJCBN = createNeuron(jacksonForenameBN, "jackson (jackson cook)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonJCBN, 10.0);
        CategoryNeuron jacksonForenameCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "jackson (forename)");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, jacksonJCBN, jacksonForenameCN, 10.0);

        createSynapse(t.B_CATEGORY_INPUT_SYNAPSE_TEMPLATE, jacksonForenameCN, jacksonForenameBN, 10.0);
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonForenameBN, 10.0);
        CategoryNeuron forenameCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "forename");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, jacksonForenameBN, forenameCN, 10.0);

        BindingNeuron jacksonCityBN = createNeuron(t.BINDING_TEMPLATE, "jackson (city)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonCityBN, 10.0);
        CategoryNeuron cityCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "city");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, jacksonCityBN, cityCN, 10.0);

        BindingNeuron cookSurnameBN = createNeuron(surnameBN, "cook (surname)");
        BindingNeuron cookJCBN = createNeuron(cookSurnameBN, "cook (jackson cook)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookJCBN, 10.0);
        CategoryNeuron cookSurnameCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "cook (surname)");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, cookJCBN, cookSurnameCN, 10.0);

        createSynapse(t.B_CATEGORY_INPUT_SYNAPSE_TEMPLATE, cookSurnameCN, cookSurnameBN, 10.0);
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookSurnameBN, 10.0);
        CategoryNeuron surnameCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "surname");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, cookSurnameBN, surnameCN, 10.0);

        BindingNeuron cookProfessionBN = createNeuron(t.BINDING_TEMPLATE, "cook (profession)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookProfessionBN, 10.0);
        CategoryNeuron professionCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "profession");
        createSynapse(t.BINDING_CATEGORY_SYNAPSE_TEMPLATE, cookProfessionBN, professionCN, 10.0);

        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-jackson"), false, jacksonForenameBN, jacksonCityBN);
        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-cook"), false, cookSurnameBN, cookProfessionBN);

        updateBias(jacksonJCBN, 2.0);
        updateBias(jacksonForenameBN, 2.0);
        updateBias(jacksonCityBN, 3.0);
        updateBias(cookJCBN, 2.0);
        updateBias(cookSurnameBN, 2.0);
        updateBias(cookProfessionBN, 3.0);

        createSynapse(t.B_CATEGORY_INPUT_SYNAPSE_TEMPLATE, forenameCN, forenameBN, 10.0);
        createSynapse(t.B_CATEGORY_INPUT_SYNAPSE_TEMPLATE, surnameCN, surnameBN, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, surnameBN, 5.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, forenameBN, surnameBN, 10.0);

        updateBias(forenameBN, 2.0);
        updateBias(surnameBN, 2.0);

        PatternNeuron jacksonCookPattern = initPatternLoop(t, "jackson cook", jacksonJCBN, cookJCBN);
        updateBias(jacksonCookPattern, 3.0);

        PatternNeuron personNamePattern = initPatternLoop(t, "person name", forenameBN, surnameBN);
        updateBias(personNamePattern, 3.0);



        Document doc = new Document(m, "Jackson Cook");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(4.7);
        camera.setViewCenter(1.293, 1.279, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.5);
        camera.setViewCenter(1.702, 2.272, 0);

        processTokens(t.TOKEN_TEMPLATE, doc, List.of("Jackson", "Cook"));

        doc.postProcessing();
        doc.updateModel();
    }
}
