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
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.disjunctive.InhibitoryNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static network.aika.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Lukas Molzberger
 */
public class MutualExclusionTest {

    @Test
    public void testPropagation() {
        TextModel m = new TextModel();
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron in = createNeuron(t.PATTERN_TEMPLATE, "I", true);
        BindingNeuron na = createNeuron(t.BINDING_TEMPLATE, "A");
        BindingNeuron nb = createNeuron(t.BINDING_TEMPLATE, "B");
        BindingNeuron nc = createNeuron(t.BINDING_TEMPLATE, "C");
        InhibitoryNeuron inhib = createNeuron(t.INHIBITORY_TEMPLATE, "I");

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, in, na, 10.0);
        createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhib, na, -100.0);
        TestUtils.updateBias(na, 1.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, in, nb, 10.0);
        createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhib, nb, -100.0);
        TestUtils.updateBias(nb, 1.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, in, nc, 10.0);
        createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhib, nc, -100.0);
        TestUtils.updateBias(nc, 1.2);

        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, na, inhib, 1.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, nb, inhib, 1.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, nc, inhib, 1.0);


        Document doc = new Document(m, "test");

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setTrainingEnabled(true);
        doc.setConfig(c);

        doc.addToken(in, 0, 4);

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);

        Set<BindingActivation> nbActs = nb.getActivations(doc);
        Activation nbAct = nbActs.iterator().next();

        assertTrue(nbAct.getValue().getCurrentValue() > 0.38);
    }


    @Test
    public void testPropagationWithPrimaryLink() {
        TextModel m = new TextModel();
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron in = createNeuron(t.PATTERN_TEMPLATE, "I", true);
        BindingNeuron na = createNeuron(t.BINDING_TEMPLATE, "A");
        BindingNeuron nb = createNeuron(t.BINDING_TEMPLATE, "B");
        InhibitoryNeuron inhib = createNeuron(t.INHIBITORY_TEMPLATE, "I");

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, in, na, 10.0);
        createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhib, na, -100.0);
        updateBias(na, 1.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, in, nb, 10.0);
        createSynapse(t.NEGATIVE_FEEDBACK_SYNAPSE_TEMPLATE, inhib, nb, -100.0);
        updateBias(nb, 1.5);

        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, na, inhib, 1.0);
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, nb, inhib, 1.0);

        Document doc = new Document(m, "test");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.011)
                        .setTrainingEnabled(true)
        );
/*
TODO: Counting Mode
        doc.setQueueFilter(s ->
                s.getStepType() == StepType.TEMPLATE || s.getStepType() == StepType.TRAINING
        );
*/
        AIKADebugger.createAndShowGUI(doc);

        doc.addToken(in, 0, 4);
        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
        System.out.println();
    }
}
