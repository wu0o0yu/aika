package network.aika.training;

import network.aika.DistanceFunction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;
import network.aika.neuron.relation.Relation;

import java.util.Map;

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
        public Result(boolean isRecurrent, int rangeInput, Range.Output rangeOutput, boolean identity, Map<Integer, Relation> relations, DistanceFunction distanceFunction, double significance, DeleteMode deleteMode) {
            this.isRecurrent = isRecurrent;
            this.rangeInput = rangeInput;
            this.rangeOutput = rangeOutput;
            this.identity = identity;
            this.relations = relations;
            this.distanceFunction = distanceFunction;
            this.significance = significance;
            this.deleteMode = deleteMode;
        }

        public boolean isRecurrent;
        public int rangeInput;
        public Range.Output rangeOutput;
        public boolean identity;
        public Map<Integer, Relation> relations;
        public DistanceFunction distanceFunction;
        public double significance;
        public DeleteMode deleteMode;
    }
}
