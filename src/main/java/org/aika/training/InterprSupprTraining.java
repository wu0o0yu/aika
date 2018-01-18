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


import org.aika.corpus.Document;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;


/**
 *
 * @author Lukas Molzberger
 */
public class InterprSupprTraining {


    public static double LEARN_RATE = -0.1;


    public static void train(Document doc, double learnRate) {
        for(INeuron n: doc.activatedNeurons) {
            for(Activation act: n.getActivations(doc)) {
                if(!act.isFinalActivation() && act.maxActValue > 0.0 && n.type != INeuron.Type.META) {
                    act.errorSignal += learnRate * act.maxActValue;
                }
            }
        }
    }
}
