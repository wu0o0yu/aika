package org.aika.training;


import org.aika.corpus.Document;
import org.aika.lattice.NodeActivation;

import java.util.Collection;

public class PatternDiscovery {



    public static class DiscoveryConfig {
        public Document.PatternEvaluation checkValidPattern;
        public Document.ActivationEvaluation checkExpandable;
        public Document.Counter counter;


        public DiscoveryConfig setCheckValidPattern(Document.PatternEvaluation checkValidPattern) {
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
        public DiscoveryConfig setCheckExpandable(Document.ActivationEvaluation checkExpandable) {
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
        public DiscoveryConfig setCounter(Document.Counter counter) {
            this.counter = counter;
            return this;
        }
    }


    public static void discover(Document doc, DiscoveryConfig discoveryConfig) {
        Collection<NodeActivation> allActs = doc.getAllNodeActivations();
        allActs.forEach(act -> discoveryConfig.counter.count(act));

        allActs.stream()
                .filter(act -> discoveryConfig.checkExpandable.evaluate(act))
                .forEach(act -> act.key.node.discover(doc, act, discoveryConfig));
    }

}
