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

    enum DeleteMode {
        NONE,
        DELETE,
        DELETE_IF_SIGN_CHANGES,
        DELETE_NEGATIVES,
        DELETE_POSITIVES;

        public void checkIfDelete(Synapse s) {
            double ow = s.weight;
            double nw = s.getNewWeight();

            switch(this) {
                case DELETE:
                    s.toBeDeleted = true;
                    break;
                case DELETE_IF_SIGN_CHANGES:
                    if(nw == 0.0 || (ow != 0.0 && nw > 0.0 != ow > 0.0)) s.toBeDeleted = true;
                    break;
                case DELETE_NEGATIVES:
                    if(nw <= 0.0) s.toBeDeleted = true;
                    break;
                case DELETE_POSITIVES:
                    if(nw >= 0.0) s.toBeDeleted = true;
                    break;
            }
        }
    }

    class Result {
        public Result(Synapse.Key synapseKey, double significance, DeleteMode deleteMode) {
            this.synapseKey = synapseKey;
            this.significance = significance;
            this.deleteMode = deleteMode;
        }

        public Synapse.Key synapseKey;
        public double significance;
        public DeleteMode deleteMode;
    }
}
