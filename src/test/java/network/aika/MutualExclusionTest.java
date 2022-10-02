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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.NegativeFeedbackSynapse;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.disjunctive.CategorySynapse;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.neuron.disjunctive.InhibitorySynapse;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static network.aika.TestUtils.*;
import static network.aika.neuron.bindingsignal.State.INPUT;
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.PATTERN;
import static network.aika.steps.Phase.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {


    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{0.0, 0.0});
        coords.put(1, new double[]{-0.078, 0.2});
        coords.put(2, new double[]{0.12, 0.2});
        coords.put(3, new double[]{0.276, 0.4});
        coords.put(4, new double[]{0.12, 0.3});
        coords.put(5, new double[]{-0.236, 0.4});
        coords.put(6, new double[]{-0.078, 0.3});
        return coords;
    }


    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(8l, new double[]{0.693, -0.146});
        coords.put(9l, new double[]{0.363, 0.348});
        coords.put(10l, new double[]{0.982, 0.375});
        coords.put(11l, new double[]{0.648, 0.895});
        coords.put(12l, new double[]{0.026, 0.807});
        coords.put(13l, new double[]{1.372, 0.85});

        return coords;
    }

    @Test
    public void testPropagation() {
        Model m = new Model();

        TokenNeuron in = createNeuron(new PatternNeuron(), "I", true);
        BindingNeuron na = createNeuron(new BindingNeuron(), "A");
        BindingNeuron nb = createNeuron(new BindingNeuron(), "B");
        BindingNeuron nc = createNeuron(new BindingNeuron(), "C");
        InhibitoryNeuron inhib = createNeuron(new InhibitoryNeuron(), "I");

        createSynapse(new PrimaryInputSynapse(), in, na, 10.0);
        createSynapse(new NegativeFeedbackSynapse(), inhib, na, -100.0);
        TestUtils.updateBias(na, 1.0);

        createSynapse(new PrimaryInputSynapse(), in, nb, 10.0);
        createSynapse(new NegativeFeedbackSynapse(), inhib, nb, -100.0);
        TestUtils.updateBias(nb, 1.5);

        createSynapse(new PrimaryInputSynapse(), in, nc, 10.0);
        createSynapse(new NegativeFeedbackSynapse(), inhib, nc, -100.0);
        TestUtils.updateBias(nc, 1.2);

        createSynapse(new InhibitorySynapse(INPUT), na, inhib, 1.0);
        createSynapse(new InhibitorySynapse(INPUT), nb, inhib, 1.0);
        createSynapse(new InhibitorySynapse(INPUT), nc, inhib, 1.0);


        Document doc = new Document(m, "test");

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setTrainingEnabled(true);
        doc.setConfig(c);

        doc.addToken(in, 0, 0, 4);

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);

        Set<BindingActivation> nbActs = nb.getActivations(doc);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue(true).getCurrentValue() > 0.38);
    }


    @Test
    public void testPropagationWithPrimaryLink() {
        Model m = new Model();

        TokenNeuron in = createNeuron(new TokenNeuron(), "I", true);
        BindingNeuron na = createNeuron(new BindingNeuron(), "A");
        BindingNeuron nb = createNeuron(new BindingNeuron(), "B");
        InhibitoryNeuron inhib = createNeuron(new InhibitoryNeuron(), "I");

        createSynapse(new PrimaryInputSynapse(), in, na, 10.0);
        createSynapse(new NegativeFeedbackSynapse(), inhib, na, -20.0);
        updateBias(na, 1.0);
        PatternNeuron pa = initPatternLoop(m, "A", na);
        updateBias(pa, 3.0);


        createSynapse(new PrimaryInputSynapse(), in, nb, 10.0);
        createSynapse(new NegativeFeedbackSynapse(), inhib, nb, -20.0);
        updateBias(nb, 1.5);
        PatternNeuron pb = initPatternLoop(m, "B", nb);
        updateBias(pb, 3.0);

        createSynapse(new InhibitorySynapse(INPUT), na, inhib, 1.0);
        createSynapse(new InhibitorySynapse(INPUT), nb, inhib, 1.0);



        Document doc = new Document(m, "test");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.011)
                        .setInductionThreshold(0.1)
                        .setTrainingEnabled(true)
        );

        AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);

        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.0);
        camera.setViewCenter(0.2, 0.2, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.75);
        camera.setViewCenter(0.64, 0.375, 0);


        TokenActivation tAct = doc.addToken(in, 0, 0, 4);
        tAct.setNet(10.0);

        doc.process(PROCESSING);

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

        System.out.println(doc);
        System.out.println();
    }
}
