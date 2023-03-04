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
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.neurons.TokenNeuron;
import network.aika.elements.neurons.InhibitoryNeuron;
import network.aika.elements.synapses.InhibitorySynapse;
import network.aika.elements.synapses.InputPatternSynapse;
import network.aika.elements.synapses.NegativeFeedbackSynapse;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import static network.aika.TestUtils.*;
import static network.aika.elements.synapses.Scope.INPUT;
import static network.aika.steps.Phase.INFERENCE;
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

        TokenNeuron in = new TokenNeuron().init(m, "I");
        BindingNeuron na = new BindingNeuron().init(m, "A");
        BindingNeuron nb = new BindingNeuron().init(m, "B");
        BindingNeuron nc = new BindingNeuron().init(m, "C");
        InhibitoryNeuron inhib = new InhibitoryNeuron().init(m, "I");

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, na)
                .adjustBias();
        new NegativeFeedbackSynapse()
                .setWeight(-100.0)
                .init(inhib, na);

        TestUtils.setBias(na, 1.0);

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, nb)
                .adjustBias();

        new NegativeFeedbackSynapse()
                .setWeight(-100.0)
                .init(inhib, nb);

        TestUtils.setBias(nb, 1.5);

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, nc)
                .adjustBias();

        new NegativeFeedbackSynapse()
                .setWeight(-100.0)
                .init(inhib, nc);

        TestUtils.setBias(nc, 1.2);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(na, inhib);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(nb, inhib);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(nc, inhib);


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

        SortedSet<BindingActivation> nbActs = nb.getActivations(doc);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue().getCurrentValue() > 0.38);
    }


    @Test
    public void testPropagationWithPrimaryLink() {
        Model m = new Model();

        TokenNeuron in = new TokenNeuron().init(m, "I");
        BindingNeuron na = new BindingNeuron().init(m, "A");
        BindingNeuron nb = new BindingNeuron().init(m, "B");
        InhibitoryNeuron inhib =new InhibitoryNeuron().init(m, "I");

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, na)
                .adjustBias();

        new NegativeFeedbackSynapse()
                .setWeight(-20.0)
                .init(inhib, na)
                .adjustBias();

        setBias(na, 1.0);
        PatternNeuron pa = initPatternLoop(m, "A", na);
        setBias(pa, 3.0);


        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, nb)
                .adjustBias();

        new NegativeFeedbackSynapse()
                .setWeight(-20.0)
                .init(inhib, nb);

        setBias(nb, 1.5);
        PatternNeuron pb = initPatternLoop(m, "B", nb);
        setBias(pb, 3.0);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(na, inhib);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(nb, inhib);



        Document doc = new Document(m, "test");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.011)
                        .setTrainingEnabled(true)
        );

        AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);

        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
//        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.0);
        camera.setViewCenter(0.2, 0.2, 0);

//        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.75);
        camera.setViewCenter(0.64, 0.375, 0);


        TokenActivation tAct = doc.addToken(in, 0, 0, 4);
        tAct.setNet(10.0);
        doc.process(INFERENCE);

        doc.anneal();

        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
        System.out.println();
    }
}
