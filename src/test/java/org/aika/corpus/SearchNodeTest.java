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
package org.aika.corpus;

import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Range.Relation;
import org.aika.neuron.INeuron;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class SearchNodeTest {


    @Test
    public void testJoergZimmermann() {
        Model m = new Model();

        Neuron wJoerg = m.createNeuron("W-Joerg");
        Neuron wZimmermann = m.createNeuron("W-Zimmermann");

        Neuron eJoergForename = m.createNeuron("E-Joerg (Forename)");
        Neuron eJoergSurname = m.createNeuron("E-Joerg (Surname)");
        Neuron eZimmermannSurname = m.createNeuron("E-Zimmermann (Surname)");
        Neuron eZimmermannCompany = m.createNeuron("E-Zimmermann (Company)");

        Neuron suppr = m.createNeuron("SUPPR");

        m.initNeuron(eJoergSurname, 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(wJoerg)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(false)
        );
        m.initNeuron(eZimmermannCompany, 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(wZimmermann)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(false)
        );

        m.initNeuron(eJoergForename, 6.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(wJoerg)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eZimmermannSurname)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(1)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.NONE)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(false)
        );

        m.initNeuron(eZimmermannSurname, 6.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(wZimmermann)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJoergForename)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRelativeRid(-1)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.NONE)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(false)
        );


        m.initNeuron(suppr, 0.0, INeuron.Type.INHIBITORY,
                new Input()
                        .setNeuron(eJoergForename)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eJoergSurname)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eZimmermannCompany)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(eZimmermannSurname)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("Joerg Zimmermann");

        wJoerg.addInput(doc, 0, 6, 0);
        wZimmermann.addInput(doc, 6, 16, 1);

        doc.process();

        System.out.println(doc.neuronActivationsToString(true));

        Assert.assertTrue(eZimmermannCompany.getFinalActivations(doc).isEmpty());
        Assert.assertFalse(eZimmermannSurname.getFinalActivations(doc).isEmpty());

        doc.clearActivations();

        doc = m.createDocument("Joerg Zimmermann Joerg Zimmermann");

        wJoerg.addInput(doc, 0, 6, 0);
        wZimmermann.addInput(doc, 6, 17, 1);
        wJoerg.addInput(doc, 17, 23, 2);
        wZimmermann.addInput(doc, 23, 33, 3);

        doc.process();

        System.out.println(doc.neuronActivationsToString(true));

        Assert.assertEquals(0, eZimmermannCompany.getFinalActivations(doc).size());
        Assert.assertEquals(2, eZimmermannSurname.getFinalActivations(doc).size());

        doc.clearActivations();
    }


    @Test
    public void testBackwardReferencingSynapses() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron inhib = m.createNeuron("INHIB");

        Neuron nF = m.createNeuron("F");

        Neuron nC = m.initNeuron(m.createNeuron("C"), 6.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        Neuron nD = m.initNeuron(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nF)
                        .setWeight(2.0)
                        .setBias(0.0)
                        .setRangeMatch(Range.Relation.NONE),
                new Input()
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        Neuron nE = m.initNeuron(m.createNeuron("E"), 5.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );

        m.initNeuron(nF, 6.0, INeuron.Type.EXCITATORY,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Range.Relation.EQUALS)
        );


        m.initNeuron(inhib, 0.0, INeuron.Type.INHIBITORY,
                new Input()
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nE)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new Input()
                        .setNeuron(nF)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("aaaa bbbb ");

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        doc.process();

        System.out.println(doc.neuronActivationsToString(true, true, true));

        Assert.assertFalse(nD.getFinalActivations(doc).isEmpty());
    }

}
