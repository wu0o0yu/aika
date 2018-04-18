package org.aika.training;

import org.aika.neuron.activation.Activation;
import org.aika.neuron.Synapse;
import org.aika.neuron.activation.Range;

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

        public void checkIfDelete(Synapse s, boolean delete) {
            double ow = s.weight;
            double nw = s.getNewWeight();

            switch(this) {
                case DELETE:
                    deleteOrInactivate(s, delete);
                    break;
                case DELETE_IF_SIGN_CHANGES:
                    if(nw == 0.0 || (ow != 0.0 && nw > 0.0 != ow > 0.0)) deleteOrInactivate(s, delete);
                    break;
                case DELETE_NEGATIVES:
                    if(nw <= 0.0) deleteOrInactivate(s, delete);
                    break;
                case DELETE_POSITIVES:
                    if(nw >= 0.0) deleteOrInactivate(s, delete);
                    break;
            }
        }

        private void deleteOrInactivate(Synapse s, boolean delete) {
            if(delete) {
                s.toBeDeleted = true;
            } else {
                s.inactive = true;
            }
        }
    }

    class Result {
        public Result(Range.Output rangeOutput, double significance, DeleteMode deleteMode) {
            this.rangeOutput = rangeOutput;
            this.significance = significance;
            this.deleteMode = deleteMode;
        }

        public Range.Output rangeOutput;
        public double significance;
        public DeleteMode deleteMode;
    }
}
