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
package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

/**
 *
 * @author Lukas Molzberger
 */
public class Training implements ActivationPhase {


    @Override
    public void process(Activation act) {
        if(act.getNeuron().isInputNeuron()) {
            return;
        }

        act.train();
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public void tryToLink(Activation iAct, Activation oAct, Visitor v) {
    }

    @Override
    public void propagate(Activation act, Visitor v) {
    }

    @Override
    public int getRank() {
        return 5;
    }

    @Override
    public int compare(Activation act1, Activation act2) {
        return 0;
    }
}
