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
package network.aika;


import network.aika.Document;
import network.aika.lattice.AndNode;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.Relation;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternDiscovery {

    public interface CandidateCheck {

        /**
         * Check if <code>node</code> is an interesting pattern that might be considered for further processing.
         *
         * This property is required to be monotonic over the size of the pattern. In other words, if a pattern is
         * interesting, then all its sub patterns also need to be interesting.
         *
         * @param act
         * @return
         */

        boolean check(NodeActivation act, NodeActivation secondAct);
    }


    public interface PatternCheck {

        boolean check(AndNode andNode);
    }


    public interface CandidateRelations {

        List<Relation> getRelations(Activation act1, Activation act2);
    }


    public interface Counter {

        /**
         * Updates the statistics of this node
         *
         * @param act
         * @return
         */
        void count(NodeActivation act);
    }


    public static class Config {
        public CandidateCheck candidateCheck;
        public PatternCheck patternCheck;
        public Counter counter;
        public CandidateRelations candidateRelations;


        public Config setCandidateCheck(CandidateCheck candidateCheck) {
            this.candidateCheck = candidateCheck;
            return this;
        }

        public Config setPatternCheck(PatternCheck patternCheck) {
            this.patternCheck = patternCheck;
            return this;
        }

        /**
         * The counter callback function should implement a customized counting function.
         * The counting function should modify the custom meta object stored in the node.
         * The NodeStatisticFactory is used to instantiate the custom meta object for a node.
         *
         * @param counter
         * @return
         */
        public Config setCounter(Counter counter) {
            this.counter = counter;
            return this;
        }

        public Config setCandidateRelations(CandidateRelations candidateRelations) {
            this.candidateRelations = candidateRelations;
            return this;
        }
    }


    public static void discover(Document doc, Config config) {
        doc.createV = doc.visitedCounter++;

        doc.getAllActivationsStream().forEach(act -> config.counter.count(act));

        ArrayList<NodeActivation> activations = new ArrayList<>(doc.addedNodeActivations);
        doc.addedNodeActivations.clear();

        activations.forEach(act -> act.node.discover(act, config));

//        doc.propagate();

//        doc.addedNodeActivations.forEach(act -> config.counter.count(act));
    }

}
