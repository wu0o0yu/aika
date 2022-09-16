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
public class ABCDTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();
        coords.put(0, new double[]{-0.683, -0.476});
        coords.put(1, new double[]{-0.595, -0.266});
        coords.put(2, new double[]{-0.677, 0.214});
        coords.put(3, new double[]{-0.914, -0.044});
        coords.put(4, new double[]{-0.425, -0.083});
        coords.put(5, new double[]{-0.414, 0.638});
        coords.put(6, new double[]{-0.242, 0.219});
        coords.put(7, new double[]{0.381, -0.533});
        coords.put(8, new double[]{-0.076, -0.283});
        coords.put(9, new double[]{0.381, 0.219});
        coords.put(10, new double[]{-0.154, -0.066});
        coords.put(11, new double[]{0.515, -0.109});
        coords.put(12, new double[]{0.636, 0.665});
        coords.put(13, new double[]{0.886, 0.23});
        coords.put(14, new double[]{0.204, 0.635});
        coords.put(15, new double[]{-0.078, 0.222});
        coords.put(16, new double[]{-0.062, 0.641});
        coords.put(17, new double[]{0.177, 0.222});
        coords.put(18, new double[]{0.886, -0.541});
        coords.put(19, new double[]{0.978, -0.31});
        coords.put(20, new double[]{0.765, -0.083});
        coords.put(21, new double[]{1.147, -0.066});
        coords.put(22, new double[]{0.899, 1.293});
        coords.put(23, new double[]{1.227, 1.74});
        coords.put(24, new double[]{1.735, 1.302});
        coords.put(25, new double[]{1.69, -0.571});
        coords.put(26, new double[]{1.689, -0.266});
        coords.put(27, new double[]{1.53, -0.051});
        coords.put(28, new double[]{1.835, -0.05});
        coords.put(29, new double[]{-0.677, 0.314});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();
        coords.put(1l, new double[]{1.942, -2.738});
        coords.put(2l, new double[]{4.879, -1.981});
        coords.put(3l, new double[]{1.739, -1.882});
        coords.put(4l, new double[]{-1.274, -3.501});
        coords.put(5l, new double[]{0.802, -3.547});
        coords.put(6l, new double[]{3.915, -3.588});
        coords.put(7l, new double[]{5.852, -3.531});
        coords.put(8l, new double[]{-1.257, -0.365});
        coords.put(9l, new double[]{-0.034, -0.312});
        coords.put(10l, new double[]{-0.661, 1.384});
        coords.put(11l, new double[]{1.565, -0.24});
        coords.put(12l, new double[]{3.894, -0.249});
        coords.put(13l, new double[]{0.868, 1.283});
        coords.put(14l, new double[]{2.593, 1.36});
        coords.put(15l, new double[]{3.865, 2.807});
        coords.put(16l, new double[]{6.174, 2.821});
        coords.put(17l, new double[]{4.95, 4.607});

        return coords;
    }

    /**
     *
     */
    @Test
    public void testABCD() throws InterruptedException {
        SimpleTemplateGraph t = new SimpleTemplateGraph();
        Model m = new Model();
        m.setTemplateGraph(t);

        PatternNeuron a_IN = t.TOKEN_TEMPLATE.lookupToken("a");

        PatternNeuron b_IN = t.TOKEN_TEMPLATE.lookupToken("b");
        PatternNeuron c_IN = t.TOKEN_TEMPLATE.lookupToken("c");
        PatternNeuron d_IN = t.TOKEN_TEMPLATE.lookupToken("d");

        // Pattern ab
        BindingNeuron a_abBN = createNeuron(t.BINDING_TEMPLATE, "a (ab)");
        BindingNeuron b_abBN = createNeuron(t.BINDING_TEMPLATE, "b (ab)");

        LatentRelationNeuron relPT = t.TOKEN_POSITION_RELATION_TEMPLATE.lookupRelation(-1, -1);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, b_abBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, a_abBN, b_abBN, 11.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, a_IN, a_abBN, 10.0);
        updateBias(a_abBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_abBN, 10.0);

        PatternNeuron abPattern = initPatternLoop(t, "ab", a_abBN, b_abBN);
        updateBias(abPattern, 3.0);

        // Pattern bc
        BindingNeuron b_bcBN = createNeuron(t.BINDING_TEMPLATE, "b (bc)");
        BindingNeuron c_bcBN = createNeuron(t.BINDING_TEMPLATE, "c (bc)");

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, c_bcBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, b_bcBN, c_bcBN, 11.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_bcBN, 10.0);
        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-b"), false, b_abBN, b_bcBN);
        updateBias(b_abBN, 3.0);
        updateBias(b_bcBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, c_IN, c_bcBN, 10.0);
        updateBias(c_bcBN, 3.0);

        PatternNeuron bcPattern = initPatternLoop(t, "bc", b_bcBN, c_bcBN);
        updateBias(bcPattern, 3.0);

        // Pattern bcd
        BindingNeuron bc_bcdBN = createNeuron(t.BINDING_TEMPLATE, "bc (bcd)");
        BindingNeuron d_bcdBN = createNeuron(t.BINDING_TEMPLATE, "d (bcd)");
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, c_bcBN, bc_bcdBN, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, relPT, d_bcdBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, bc_bcdBN, d_bcdBN, 11.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, bcPattern, bc_bcdBN, 10.0);
        updateBias(bc_bcdBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, d_IN, d_bcdBN, 10.0);
        updateBias(d_bcdBN, 3.0);

        PatternNeuron bcdPattern = initPatternLoop(t, "bcd", bc_bcdBN, d_bcdBN);
        updateBias(bcdPattern, 3.0);


        Document doc = new Document(m, "abcd");

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true);
        doc.setConfig(c);

        AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);
        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(2.95);
        camera.setViewCenter(0.451, 0.579, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.2);
        camera.setViewCenter(1.978, 0.47, 0);

        processTokens(t, doc, List.of("a", "b", "c", "d"));

        doc.postProcessing();
        doc.updateModel();

        Thread.sleep(100);
    }
}
