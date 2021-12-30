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

import network.aika.neuron.Neuron;
import network.aika.neuron.Templates;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import org.junit.jupiter.api.Test;

import static network.aika.utils.TestUtils.*;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternTest {

    @Test
    public void testPatternPos() {
        TextModel m = initModel();

        Document doc = new Document(m, "ABC");

        TokenActivation refA = doc.addToken("A", 0, 1);
        TokenActivation refB = doc.addToken("B", 1, 2);
        TokenActivation refC = doc.addToken("C", 2, 3);
        TokenActivation.addRelation(refA, refB);
        TokenActivation.addRelation(refB, refC);

        System.out.println(doc);

        doc.process();
        doc.updateModel();

        System.out.println(doc);
    }


    @Test
    public void testPatternNeg() {
        TextModel m = initModel();

        Document doc = new Document(m, "ABC");

        TokenActivation refA = doc.addToken("A", 0, 1);
        TokenActivation refB = doc.addToken("B", 1, 2);
        TokenActivation.addRelation(refA, refB);

        doc.process();
        doc.updateModel();

        System.out.println(doc);
    }

    public TextModel initModel() {
        TextModel m = new TextModel();
        Templates t = new Templates(m);

        PatternNeuron nA = m.lookupToken("A");
        PatternNeuron nB = m.lookupToken("B");
        PatternNeuron nC = m.lookupToken( "C");

        BindingNeuron eA = createNeuron(t.SAME_BINDING_TEMPLATE, "E A");
        BindingNeuron eB = createNeuron(t.SAME_BINDING_TEMPLATE, "E B");
        BindingNeuron eC = createNeuron(t.SAME_BINDING_TEMPLATE, "E C");

        PatternNeuron out = createNeuron(t.SAME_PATTERN_TEMPLATE, "OUT");
        out.setTokenLabel("ABC");

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_TEMPLATE, nA, eA, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE, out, eA, 10.0);
        updateBias(eA, 4.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_TEMPLATE, nB, eB, 10.0);
        createSynapse( t.SAME_PATTERN_SYNAPSE_TEMPLATE, eA, eB, 10.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE, lookupBindingNeuronPT(m, nB), eB, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE, out, eB, 10.0);
        updateBias(eB, 4.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_TEMPLATE, nC, eC, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, eB, eC, 10.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE, lookupBindingNeuronPT(m, nC), eC, 10.0);
        createSynapse(t.POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE, out, eC, 10.0);

        updateBias(eC, 4.0);

        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eA, out, 10.0);
        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eB, out, 10.0);
        createSynapse(t.PATTERN_SYNAPSE_TEMPLATE, eC, out, 10.0);

        updateBias(out,4.0);

        return m;
    }

    public BindingNeuron lookupBindingNeuronPT(TextModel tm, PatternNeuron pn) {
        return (BindingNeuron) pn.getOutputSynapses()
                .map(s -> s.getOutput())
                .filter(n -> isPTNeuron(tm, n))
                .findAny()
                .orElse(null);
    }

    private boolean isPTNeuron(TextModel tm, Neuron<?, ?> n) {
        return n.getOutputSynapses()
                .map(s -> s.getOutput())
                .anyMatch(in -> in == tm.getPrevTokenInhib());
    }
}
