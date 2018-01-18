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


import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

/**
 *
 * @author Lukas Molzberger
 */
public class SynapseSignificance {


    public static double sig(Synapse s) {
        return sig(s.input.get(), s.output.get());
    }


    public static double sig(Activation iAct, Activation oAct) {
        return sig(iAct.key.node.neuron.get(), oAct.key.node.neuron.get());
    }


    public static double sig(INeuron in, INeuron on) {
        if(on.type == INeuron.Type.INHIBITORY) {
            return 1.0;
        }

        int iFreq = Math.max(1, ((NeuronStatistic) in.statistic).frequency);
        int oFreq = Math.max(1, ((NeuronStatistic) on.statistic).frequency);
        return Math.pow(iFreq * oFreq, -0.2);
    }
}
