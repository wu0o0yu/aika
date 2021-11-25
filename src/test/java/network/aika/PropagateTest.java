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

import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateTest {

    @Test
    public void testPropagation() {
        TextModel m = new TextModel();
        Templates t = new Templates(m);

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("A");
        in.setInputNeuron(true);
        in.setLabel("IN");
        BindingNeuron out = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        out.setLabel("OUT");

        Synapse s = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(in, out);

        s.linkInput();
        s.linkOutput();
        s.getWeight().add(10.0);
        out.getBias().add(-9.0);

        Document doc = new Document(m, "test");

        doc.addToken(in, 0, 4);

        System.out.println(doc);
    }
}
