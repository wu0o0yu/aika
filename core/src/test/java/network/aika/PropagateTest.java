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

import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.synapses.InputPatternSynapse;
import network.aika.elements.neurons.TokenNeuron;
import network.aika.text.Document;
import org.junit.jupiter.api.Test;

import static network.aika.TestUtils.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PropagateTest {

    @Test
    public void testPropagation() {
        Model m = new Model();

        Config c = new Config();

        TokenNeuron in = new TokenNeuron().init(m, "IN");
        BindingNeuron out = new BindingNeuron().init(m, "OUT");

        new InputPatternSynapse()
                .setWeight(10.0)
                .init(in, out);

        setBias(out, 1.0);

        Document doc = new Document(m, "test");
        doc.setConfig(c);
        doc.addToken(in, 0, 0, 4);
        System.out.println(doc);
    }
}
