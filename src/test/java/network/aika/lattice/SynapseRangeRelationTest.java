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
package network.aika.lattice;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.lattice.NodeActivation.Key;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 *
 * @author Lukas Molzberger
 */
public class SynapseRangeRelationTest {


    @Test
    public void testSynapseRangeRelation() {
        Model m = new Model();
        Document doc = m.createDocument("                        ", 0);

        Neuron in = m.createNeuron();
        Neuron on = m.createNeuron();

        Synapse s = new Synapse(
                in,
                on,
                new Synapse.Key(
                        false,
                        null,
                        null,
                        Range.Relation.CONTAINS,
                        Range.Output.DIRECT
                )
        );
        s.link();

        Activation iAct0 = in.get().node.get().processActivation(doc, new Key(in.get().node.get(), new Range(1, 4), null), Collections.emptyList());
        Activation iAct1 = in.get().node.get().processActivation(doc, new Key(in.get().node.get(), new Range(6, 7), null), Collections.emptyList());
        Activation iAct2 = in.get().node.get().processActivation(doc, new Key(in.get().node.get(), new Range(10, 18), null), Collections.emptyList());
        Activation oAct = on.get().node.get().processActivation(doc, new Key(on.get().node.get(), new Range(6, 7), null), Collections.emptyList());

        Linker.link(oAct);

        boolean f = false;
        for(Activation.SynapseActivation sa: oAct.neuronInputs) {
            if(Activation.SynapseActivation.INPUT_COMP.compare(sa, new Activation.SynapseActivation(s, iAct1, oAct)) == 0) f = true;
        }

        Assert.assertTrue(f);
    }

}
