package network.aika.neuron.activation;

import java.util.Iterator;
import java.util.Map;

public class Candidate  implements Comparable<Candidate> {
    public SearchNode currentSearchNode;

    /**
     * The cached decision is used to avoid having to explore the same candidate twice even though nothing that
     * influences this candidate has changed.
     */
    public SearchNode.Decision cachedDecision = SearchNode.Decision.UNKNOWN;
    public boolean repeat = false;
    public double alternativeCachedWeightExpSum;

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
        return !activation.getInputLinks(false, false)
                .anyMatch(l -> l.input.markedHasCandidate != v && !l.synapse.isRecurrent && l.input.upperBound > 0.0);
    }


    public String toString() {
        return " ACT:" + activation.id +
                " " + activation.slotsToString() +
                " " + activation.identityToString() +
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

        Iterator<Map.Entry<Integer, Position>> ita = activation.slots.entrySet().iterator();
        Iterator<Map.Entry<Integer, Position>> itb = c.activation.slots.entrySet().iterator();

        Map.Entry<Integer, Position> mea = null;
        Map.Entry<Integer, Position> meb = null;
        while(ita.hasNext() || itb.hasNext()) {
            mea = ita.hasNext() ? ita.next() : null;
            meb = itb.hasNext() ? itb.next() : null;

            if(mea == null && meb == null) {
                break;
            } else if(mea == null && meb != null) {
                return -1;
            } else if(mea != null && meb == null) {
                return 1;
            }

            int r = Integer.compare(mea.getKey(), meb.getKey());
            if (r != 0) return r;
            r = Position.compare(activation.getSlot(mea.getKey()), c.activation.getSlot(meb.getKey()));
            if (r != 0) return r;
        }

        int r = Integer.compare(sequence, c.sequence);
        if (r != 0) return r;

        return Integer.compare(id, c.id);
    }
}