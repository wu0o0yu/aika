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
package network.aika.neuron.activation;

import network.aika.neuron.Synapse;
import network.aika.neuron.TSynapse;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public enum Direction {
    INPUT(
            Comparator.comparing(s -> s.getInput()),
            (ts, iAct, oAct) -> ts.updateCountValue(iAct, oAct),
            (ts, alpha, iAct, oAct) -> ts.updateFrequencies(alpha, iAct, oAct)
    ),

    OUTPUT(
            Comparator.comparing(s -> s.getOutput()),
            (ts, iAct, oAct) -> ts.updateCountValue(oAct, iAct),
            (ts, alpha, iAct, oAct) -> ts.updateFrequencies(alpha, oAct, iAct)
    );

    Comparator<Synapse> synapseComparator;
    UpdateCounts updateCounts;
    UpdateFrequencies updateFrequencies;

    Direction(Comparator<Synapse> synComp, UpdateCounts uc, UpdateFrequencies uf) {
        synapseComparator = synComp;
        updateCounts = uc;
        updateFrequencies = uf;
    }

    public Comparator<Synapse> getSynapseComparator() {
        return synapseComparator;
    }

    public UpdateCounts getUpdateCounts() {
        return updateCounts;
    }

    public UpdateFrequencies getUpdateFrequencies() {
        return updateFrequencies;
    }

    public interface UpdateCounts {
        void updateCounts(TSynapse ts, Activation iAct, Activation oAct);
    }

    public interface UpdateFrequencies {
        void updateFrequencies(TSynapse ts, double alpha, Activation iAct, Activation oAct);
    }

}