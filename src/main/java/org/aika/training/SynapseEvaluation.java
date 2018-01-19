package org.aika.training;

import org.aika.neuron.Activation;
import org.aika.neuron.Synapse;

public interface SynapseEvaluation {


    /**
     * Determines whether a synapse should be created between two neurons during training.
     *
     * @param s  is null if the synapse has not been created yet.
     * @param iAct
     * @param oAct
     * @return
     */
    Result evaluate(Synapse s, Activation iAct, Activation oAct);


    class Result {
        public Result(Synapse.Key synapseKey, double significance, boolean deleteIfNull) {
            this.synapseKey = synapseKey;
            this.significance = significance;
            this.deleteIfNull = deleteIfNull;
        }

        public Synapse.Key synapseKey;
        public double significance;
        public boolean deleteIfNull;
    }
}
