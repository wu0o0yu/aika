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
package org.aika;

import org.aika.lattice.Node;
import org.aika.neuron.Activation;
import org.aika.neuron.Synapse;

/**
 *
 * @author Lukas Molzberger
 */
public class TrainConfig {
    public PatternEvaluation patternEvaluation;
    public SynapseEvaluation synapseEvaluation;
    public double learnRate;


    public TrainConfig setPatternEvaluation(PatternEvaluation patternEvaluation) {
        this.patternEvaluation = patternEvaluation;
        return this;
    }


    public TrainConfig setSynapseEvaluation(SynapseEvaluation synapseEvaluation) {
        this.synapseEvaluation = synapseEvaluation;
        return this;
    }

    public TrainConfig setLearnRate(double learnRate) {
        this.learnRate = learnRate;
        return this;
    }


    public interface PatternEvaluation {

        /**
         * Check if <code>n</code> is an interesting pattern that might be considered for further processing.
         *
         * This property is required to be monotonic over the size of the pattern. In other words, if a pattern is
         * interesting, then all its sub patterns also need to be interesting.
         *
         * @param n
         * @return
         */

        boolean evaluate(Node n);
    }


    public interface SynapseEvaluation {

        /**
         * Determines whether a synapse should be created between two neurons during training.
         *
         * @param iAct
         * @param oAct
         * @return
         */
        Synapse.Key evaluate(Activation iAct, Activation oAct);
    }



}



