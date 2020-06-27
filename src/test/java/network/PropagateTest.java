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

import network.aika.text.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
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

        PatternNeuron in = new PatternNeuron(m, "IN", true);
        PatternPartNeuron out = new PatternPartNeuron(m, "OUT", false);

        PatternPartSynapse s = new PatternPartSynapse(in, out);
        s.setPropagate(true);

        s.link();
        s.updateWeight(10.0);
        out.setDirectConjunctiveBias(-10.0);
        out.setBias(1.0);

        Document doc = new Document("test");

        Activation act = new Activation(doc, in);
        act.propagateInput();

        System.out.println(doc.activationsToString());
    }
}
