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
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

/**
 *
 * @author Lukas Molzberger
 */
public class FinalLinking extends RankedImpl implements ActivationPhase {

    @Override
    public Ranked getPreviousRank() {
        return PREPARE_FINAL_LINKING;
    }

    public boolean isFinal() {
        return true;
    }

    @Override
    public void process(Activation act) {
        act.updateOutgoingLinks();
    }

    public String toString() {
        return "Final Linking";
    }
    @Override
    public int compare(Activation act1, Activation act2) {
        return act1.getFired().compareTo(act2.getFired());
    }
}
