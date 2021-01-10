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
import network.aika.neuron.phase.RankedImpl;

/**
 *
 * @author Lukas Molzberger
 */
public class PrepareFinalLinking extends RankedImpl implements ActivationPhase {

    public PrepareFinalLinking(int rank) {
        super(rank);
    }

    @Override
    public void process(Activation act) {
        if(act.isActive()) {
            act.updateForFinalPhase();
        }
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    public String toString() {
        return "Prepare Final Linking";
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
