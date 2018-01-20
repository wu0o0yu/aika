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
import org.aika.lattice.Node;
import org.aika.lattice.NodeActivation;

import java.util.Collection;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternDiscovery {

    public interface PatternEvaluation {

        /**
         * Check if <code>node</code> is an interesting pattern that might be considered for further processing.
         *
         * This property is required to be monotonic over the size of the pattern. In other words, if a pattern is
         * interesting, then all its sub patterns also need to be interesting.
         *
         * @param n
         * @return
         */

        boolean evaluate(Node n);
    }


    public interface ActivationEvaluation {

        /**
         * Check if <code>node</code> is an interesting pattern that might be considered for further processing.
         *
         * This property is required to be monotonic over the size of the pattern. In other words, if a pattern is
         * interesting, then all its sub patterns also need to be interesting.
         *
         * @param act
         * @return
         */

        boolean evaluate(NodeActivation act);
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
        public PatternEvaluation checkValidPattern;
        public ActivationEvaluation checkExpandable;
        public Counter counter;


        public Config setCheckValidPattern(PatternEvaluation checkValidPattern) {
            this.checkValidPattern = checkValidPattern;
            return this;
        }


        /**
         * This callback checks whether the current pattern might be refined to an even larger pattern.
         * If frequency is the criterion, then infrequent are not expandable.
         *
         * @param checkExpandable
         * @return
         */
        public Config setCheckExpandable(ActivationEvaluation checkExpandable) {
            this.checkExpandable = checkExpandable;
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
    }


    public static void discover(Document doc, Config config) {
        Collection<NodeActivation> allActs = doc.getAllNodeActivations();
        allActs.forEach(act -> config.counter.count(act));

        allActs.stream()
                .filter(act -> config.checkExpandable.evaluate(act))
                .forEach(act -> act.key.node.discover(doc, act, config));
    }

}
