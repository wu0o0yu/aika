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
package network;

import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.text.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateTest {

    @Test
    public void testPropagation() {
        Model m = new TextModel();

        PatternNeuron in = new PatternNeuron(m, "A", true);
        in.setDescriptionLabel("IN");
        PatternPartNeuron out = new PatternPartNeuron(m, false);
        out.setDescriptionLabel("OUT");

        ExcitatorySynapse s = new ExcitatorySynapse(in, out, false, false, true, false);

        s.linkInput();
        s.linkOutput();
        s.addWeight(10.0);
        out.addConjunctiveBias(-10.0, false);
        out.setBias(1.0);

        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.setValue(1.0);
        act.propagateInput();

        System.out.println(doc.activationsToString());
    }
}
