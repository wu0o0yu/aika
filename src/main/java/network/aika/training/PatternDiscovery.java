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
package network.aika.training;


import network.aika.Document;
import network.aika.lattice.AndNode;
import network.aika.lattice.NodeActivation;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternDiscovery {

    public interface RefinementFactory {

        /**
         * Check if <code>node</code> is an interesting pattern that might be considered for further processing.
         *
         * This property is required to be monotonic over the size of the pattern. In other words, if a pattern is
         * interesting, then all its sub patterns also need to be interesting.
         *
         * @param act
         * @return
         */

        AndNode.Refinement create(NodeActivation act, NodeActivation secondAct);
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
        public RefinementFactory refinementFactory;
        public Counter counter;


        public Config setRefinementFactory(RefinementFactory refinementFactory) {
            this.refinementFactory = refinementFactory;
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
        doc.createV = doc.visitedCounter++;

        doc.getAllActivationsStream().forEach(act -> config.counter.count(act));

        doc.addedNodeActivations.clear();

        doc.getAllActivationsStream()
                .forEach(act -> act.node.discover(act, config));

        doc.propagate();

        doc.addedNodeActivations.forEach(act -> config.counter.count(act));
    }

}
