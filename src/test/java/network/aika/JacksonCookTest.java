package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
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
        coords.put(1, new double[]{-0.081, 0.197});
        coords.put(2, new double[]{0.639, 0.637});
        coords.put(3, new double[]{-0.585, 0.534});
        coords.put(4, new double[]{-0.886, 0.398});
        coords.put(5, new double[]{0.714, 0.417});
        coords.put(6, new double[]{0.633, 0.866});
        coords.put(7, new double[]{-0.598, 0.832});
        coords.put(8, new double[]{0.631, 1.066});
        coords.put(9, new double[]{0.804, 1.069});
        coords.put(10, new double[]{0.233, 0.8});
        coords.put(11, new double[]{-0.807, 1.078});
        coords.put(12, new double[]{-0.544, 1.08});
        coords.put(13, new double[]{-0.157, 0.806});
        coords.put(14, new double[]{0.63, 1.588});
        coords.put(15, new double[]{-0.18, 0.502});
        coords.put(16, new double[]{0.196, 0.502});
        coords.put(17, new double[]{0.98, 1.899});
        coords.put(18, new double[]{1.269, 1.588});
        coords.put(19, new double[]{1.797, -0.012});
        coords.put(20, new double[]{1.8, 0.217});
        coords.put(21, new double[]{1.269, 0.637});
        coords.put(22, new double[]{2.4, 0.627});
        coords.put(23, new double[]{1.141, 0.417});
        coords.put(24, new double[]{2.616, 0.424});
        coords.put(25, new double[]{1.266, 0.889});
        coords.put(26, new double[]{2.397, 0.876});
        coords.put(27, new double[]{1.269, 1.099});
        coords.put(28, new double[]{1.571, 1.089});
        coords.put(29, new double[]{1.666, 0.893});
        coords.put(30, new double[]{2.394, 1.08});
        coords.put(31, new double[]{2.171, 1.076});
        coords.put(32, new double[]{1.987, 0.893});
        coords.put(33, new double[]{1.974, 0.604});
        coords.put(34, new double[]{1.623, 0.634});
        coords.put(35, new double[]{1.069, 1.335});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{2.188, -1.929});
        coords.put(2l, new double[]{2.84, -1.299});
        coords.put(3l, new double[]{1.623, -1.317});
        coords.put(4l, new double[]{0.053, -2.408});
        coords.put(5l, new double[]{4.429, -2.397});
        coords.put(6l, new double[]{2.218, 2.116});
        coords.put(7l, new double[]{2.84, 0.341});
        coords.put(8l, new double[]{0.995, -0.355});
        coords.put(9l, new double[]{0.991, 0.864});
        coords.put(10l, new double[]{1.001, 2.139});
        coords.put(11l, new double[]{-0.901, -0.411});
        coords.put(12l, new double[]{-0.974, 0.875});
        coords.put(13l, new double[]{-0.953, 2.08});
        coords.put(14l, new double[]{3.624, -0.371});
        coords.put(15l, new double[]{3.606, 0.927});
        coords.put(16l, new double[]{3.597, 2.107});
        coords.put(17l, new double[]{5.642, -0.461});
        coords.put(18l, new double[]{5.633, 1.017});
        coords.put(19l, new double[]{5.579, 2.044});
        coords.put(20l, new double[]{0.008, 0.854});
        coords.put(21l, new double[]{4.651, 0.936});
        coords.put(22l, new double[]{1.011, 3.432});
        coords.put(23l, new double[]{3.597, 3.459});
        coords.put(24l, new double[]{2.523, 4.281});

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
        m.setTemplateGraph(t);

        PatternNeuron jacksonIN = t.TOKEN_TEMPLATE.lookupToken("Jackson");
        PatternNeuron cookIN = t.TOKEN_TEMPLATE.lookupToken("Cook");

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(-1, -1);

        CategoryNeuron entityCN = createNeuron(t.CATEGORY_TEMPLATE, "entity");
        BindingNeuron relPrevEntityBN = createNeuron(t.BINDING_TEMPLATE, "Rel Prev. Entity");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE, entityCN, relPrevEntityBN, 9.0);
        createSynapse(t.REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE, entityCN, relPrevEntityBN, 9.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, relPrevEntityBN, 11.0);

        BindingNeuron jacksonForenameBN = createNeuron(t.BINDING_TEMPLATE, "jackson (forename)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonForenameBN, 10.0);
        PatternNeuron jacksonForenameEntity = initPatternLoop(t, "Entity: jackson (forename)", jacksonForenameBN);
        updateBias(jacksonForenameEntity, 3.0);
        CategoryNeuron forenameCN = createNeuron(t.CATEGORY_TEMPLATE, "forename");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonForenameEntity, forenameCN, 10.0);

        BindingNeuron jacksonCityBN = createNeuron(t.BINDING_TEMPLATE, "jackson (city)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonCityBN, 10.0);
        PatternNeuron jacksonCityEntity = initPatternLoop(t, "Entity: jackson (city)", jacksonCityBN);
        updateBias(jacksonCityEntity, 3.0);
        CategoryNeuron cityCN = createNeuron(t.CATEGORY_TEMPLATE, "city");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonCityEntity, cityCN, 10.0);

        BindingNeuron cookSurnameBN = createNeuron(t.BINDING_TEMPLATE, "cook (surname)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookSurnameBN, 10.0);
//        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, cookSurnameBN, relPrevEntityBN, 9.0);
        PatternNeuron cookSurnameEntity = initPatternLoop(t, "Entity: cook (surname)", cookSurnameBN);
        updateBias(cookSurnameEntity, 3.0);
        CategoryNeuron surnameCN = createNeuron(t.CATEGORY_TEMPLATE, "surname");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookSurnameEntity, surnameCN, 10.0);

        BindingNeuron cookProfessionBN = createNeuron(t.BINDING_TEMPLATE, "cook (profession)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookProfessionBN, 10.0);
        PatternNeuron cookProfessionEntity = initPatternLoop(t, "Entity: cook (profession)", cookProfessionBN);
        updateBias(cookProfessionEntity, 3.0);
        CategoryNeuron professionCN = createNeuron(t.CATEGORY_TEMPLATE, "profession");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookProfessionEntity, professionCN, 10.0);

        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonForenameEntity, entityCN, 10.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonCityEntity, entityCN, 10.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookSurnameEntity, entityCN, 10.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookProfessionEntity, entityCN, 10.0);

        initInhibitoryLoop(t, "jackson", jacksonForenameBN, jacksonCityBN);
        initInhibitoryLoop(t, "cook", cookSurnameBN, cookProfessionBN);

        updateBias(relPrevEntityBN, 3.0);
        updateBias(jacksonForenameBN, 2.0);
        updateBias(jacksonCityBN, 3.0);
        updateBias(cookSurnameBN, 2.0);
        updateBias(cookProfessionBN, 3.0);

        BindingNeuron forenameBN = createNeuron(t.BINDING_TEMPLATE, "forename (person name)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, forenameCN, forenameBN, 10.0);
        BindingNeuron surnameBN = createNeuron(t.BINDING_TEMPLATE, "surname (person name)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, surnameCN, surnameBN, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPrevEntityBN, surnameBN, 10.0);

        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, forenameBN, surnameBN, 10.0);

        updateBias(forenameBN, 2.0);
        updateBias(surnameBN, 2.0);

        PatternNeuron personNamePattern = initPatternLoop(t, "person name", forenameBN, surnameBN);
        updateBias(personNamePattern, 3.0);



        Document doc = new Document(m, "Jackson Cook");
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
        camera.setViewPercent(3.85);
        camera.setViewCenter(0.84, 1.109, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.25);
        camera.setViewCenter(1.921, 1.449, 0);

        processTokens(t, doc, List.of("Jackson", "Cook"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
