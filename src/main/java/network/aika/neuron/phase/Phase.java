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
package network.aika.neuron.phase;

import network.aika.Config;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public interface Phase extends Comparator<Activation> {
    Phase INITIAL_LINKING = new Linking();
    Phase PREPARE_FINAL_LINKING = new PrepareFinalLinking();
    Phase FINAL_LINKING = new FinalLinking();
    Phase SOFTMAX = new Softmax();
    Phase COUNTING = new Counting();
    Phase TRAINING = new Training();
    Phase GRADIENTS = new Gradients();
    Phase UPDATE_WEIGHTS = new UpdateWeights();
    Phase FINAL = new Final();

    void process(Activation act);

    Phase nextPhase(Config c);

    boolean isFinal();

    void tryToLink(Activation iAct, Activation oAct, Visitor c);

    void propagate(Activation act);

    int getRank();
}
