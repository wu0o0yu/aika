package network.aika.neuron.activation;


public class Candidate  implements Comparable<Candidate> {
    public SearchNode currentSearchNode;

    /**
     * The cached decision is used to avoid having to explore the same candidate twice even though nothing that
     * influences this candidate has changed.
     */
    public SearchNode.Decision cachedDecision = SearchNode.Decision.UNKNOWN;
    public boolean repeat = false;

    /**
     * The cached search node is used to avoid having to recompute the activation values and weights that are associated
     * with this search node.
     */
    public SearchNode cachedSearchNode;
    public SearchNode bestChildNode;

    public Activation activation;

    public int[] debugCounts = new int[3];
    public int[] debugDecisionCounts = new int[3];
    public int[] debugComputed = new int[3];

    public int id;
    int sequence = 0;

    public Candidate(Activation act, int id) {
        this.activation = act;
        this.id = id;
        act.candidate = this;
        sequence = act.getSequence();
    }


    public boolean isConflicting() {
        return !activation.getConflicts().isEmpty();
    }


    public boolean checkDependenciesSatisfied(long v) {
        for (Activation.Link l : activation.neuronInputs.values()) {
            if (l.input.markedHasCandidate != v && !l.synapse.key.isRecurrent && l.input.upperBound > 0.0) return false;
        }
        return true;
    }


    public String toString() {
        return " ACT-ID:" + activation.id +
                " " + activation.range +
                " " + activation.getLabel() +
                " CID:" + id +
                " CD:" + cachedDecision +
                " REPEAT:" + repeat +
                " CONFLICT:" + isConflicting() +
                " LIMITED:" + debugCounts[SearchNode.DebugState.LIMITED.ordinal()] +
                " CACHED:" + debugCounts[SearchNode.DebugState.CACHED.ordinal()] +
                " EXPLORE:" + debugCounts[SearchNode.DebugState.EXPLORE.ordinal()] +
                " SELECTED:" + debugDecisionCounts[0] +
                " EXCLUDED:" + debugDecisionCounts[1] +
                " SIM-CACHED:" + debugComputed[0] +
                " SIM-COMPUTED:" + debugComputed[1] +
                " MODIFIED:" + debugComputed[2];
    }


    @Override
    public int compareTo(Candidate c) {
        if(!isConflicting() && c.isConflicting()) return -1;
        if(isConflicting() && !c.isConflicting()) return 1;

        int r = Integer.compare(activation.range.begin, c.activation.range.begin);
        if (r != 0) return r;
        r = Integer.compare(activation.range.end, c.activation.range.end);
        if (r != 0) return r;

        r = Integer.compare(sequence, c.sequence);
        if (r != 0) return r;

        return Integer.compare(id, c.id);
    }
}