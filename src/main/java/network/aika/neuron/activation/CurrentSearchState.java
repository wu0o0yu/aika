package network.aika.neuron.activation;

import static network.aika.neuron.activation.Decision.UNKNOWN;

public class CurrentSearchState {
    SearchNode currentSearchNode;

    /**
     * The cached decision is used to avoid having to explore the same currentSearchState twice even though nothing that
     * influences this currentSearchState has changed.
     */
    Decision cachedDecision = UNKNOWN;
    boolean repeat = false;
    double alternativeCachedWeightSum;

    /**
     * The cached search node is used to avoid having to recompute the activation values and weights that are associated
     * with this search node.
     */
    SearchNode cachedSearchNode;
    SearchNode bestChildNode;

    int[] debugCounts = new int[3];
    int[] debugDecisionCounts = new int[3];
    int[] debugComputed = new int[3];


    Decision decision = UNKNOWN;


    public String toString() {
        return " CD:" + cachedDecision +
                " REPEAT:" + repeat +
                " LIMITED:" + debugCounts[SearchNode.DebugState.LIMITED.ordinal()] +
                " CACHED:" + debugCounts[SearchNode.DebugState.CACHED.ordinal()] +
                " EXPLORE:" + debugCounts[SearchNode.DebugState.EXPLORE.ordinal()] +
                " SELECTED:" + debugDecisionCounts[0] +
                " EXCLUDED:" + debugDecisionCounts[1] +
                " SIM-CACHED:" + debugComputed[0] +
                " SIM-COMPUTED:" + debugComputed[1] +
                " MODIFIED:" + debugComputed[2];
    }
}