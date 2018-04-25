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
                0,
                new Synapse.Key(
                        false,
                        Range.Output.DIRECT
                )
        );
        s.link();

        Activation iAct0 = in.get().node.get().processActivation(new Activation(10, doc, new Range(1, 4), in.get().node.get()));
        Activation iAct1 = in.get().node.get().processActivation(new Activation(11, doc, new Range(6, 7), in.get().node.get()));
        Activation iAct2 = in.get().node.get().processActivation(new Activation(11, doc, new Range(10, 18), in.get().node.get()));
        Activation oAct = on.get().node.get().processActivation(new Activation(11, doc, new Range(6, 7), on.get().node.get()));

        Linker.link(oAct);

        boolean f = false;
        for(Activation.SynapseActivation sa: oAct.neuronInputs.values()) {
            if(Activation.SynapseActivation.INPUT_COMP.compare(sa, new Activation.SynapseActivation(s, iAct1, oAct)) == 0) f = true;
        }

        Assert.assertTrue(f);
    }

}
