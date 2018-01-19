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
package org.aika.training;

import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Relation;
import org.aika.neuron.Activation;
import org.aika.neuron.Synapse;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class TrainingTest {


    @Test
    public void testTraining() {
        Model m = new Model();

        Neuron in = m.createNeuron("InputNeuron");
        Neuron out = m.createNeuron("OutputNeuron");

        Document doc = m.createDocument("Bla");

        in.addInput(doc, 0, 3, 0, doc.bottom, 1.0);
        Activation targetAct = out.addInput(doc, 0, 3, 0, doc.bottom, 0.0);

        targetAct.errorSignal = 1.0 - targetAct.getFinalState().value;

        doc.supervisedTraining.train(out.get(), targetAct, 0.01,
                (s, iAct, oAct) -> new SynapseEvaluation.Result(
                        new Synapse.Key(
                                false,
                                0,
                                null,
                                Relation.EQUALS,
                                Range.Output.DIRECT
                        )
                        , 1.0
                        , false
                )
        );

        doc.clearActivations();

        doc = m.createDocument("Bla");
        in.addInput(doc, 0, 3, 0, doc.bottom, 1.0);
    }


    @Test
    public void testTraining1() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron outC = m.createNeuron("C");

        {
            Document doc = m.createDocument("Bla");
            inA.addInput(doc, 0, 3, 1.0, null);
            inB.addInput(doc, 0, 3, 1.0, null);

            doc.process();


            outC.addInput(doc, 0, 3, 0.0, 1.0);

            doc.supervisedTraining.train(
                    new SupervisedTraining.TrainConfig()
                            .setLearnRate(2.0)
                            .setPerformBackpropagation(false)
                            .setSynapseEvaluation((s, iAct, oAct) ->
                                    new SynapseEvaluation.Result(new Synapse.Key(
                                            false,
                                            0,
                                            null,
                                            Relation.EQUALS,
                                            Range.Output.DIRECT
                                    ),
                                            1.0,
                                            false
                                    )
                            )
            );

            doc.commit();

            doc.clearActivations();
        }

        {
            Document doc = m.createDocument("Bla");
            inA.addInput(doc, 0, 3, 1.0, null);
            inB.addInput(doc, 0, 3, 1.0, null);

            doc.process();

            System.out.println(doc.neuronActivationsToString(true, false, true));
            Assert.assertFalse(outC.getFinalActivations(doc).isEmpty());

            doc.clearActivations();
        }
    }
}
