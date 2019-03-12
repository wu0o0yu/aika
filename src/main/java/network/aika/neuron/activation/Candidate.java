package network.aika.neuron.activation;

import java.util.Iterator;
import java.util.Map;

public class Candidate  implements Comparable<Candidate> {
    SearchNode currentSearchNode;

    /**
     * The cached decision is used to avoid having to explore the same candidate twice even though nothing that
     * influences this candidate has changed.
     */
    SearchNode.Decision cachedDecision = SearchNode.Decision.UNKNOWN;
    boolean repeat = false;
    double alternativeCachedWeightSum;

    /**
     * The cached search node is used to avoid having to recompute the activation values and weights that are associated
     * with this search node.
     */
    SearchNode cachedSearchNode;
    SearchNode bestChildNode;

    Activation activation;

    int[] debugCounts = new int[3];
    int[] debugDecisionCounts = new int[3];
    int[] debugComputed = new int[3];

    int id;
    int sequence = 0;

    public Candidate(Activation act, int id) {
        this.activation = act;
        this.id = id;
        act.candidate = this;
        sequence = act.getSequence();
    }


    public void setId(int id) {
        this.id = id;
    }


    public Activation getActivation() {
        return activation;
    }


    public boolean isConflicting() {
        return !activation.getConflicts().isEmpty();
    }


    public boolean checkDependenciesSatisfied(long v) {
        return !activation.getInputLinks(false)
                .anyMatch(l -> l.getInput().markedHasCandidate != v && !l.isRecurrent() && l.getInput().getUpperBound() > 0.0);
    }


    public String toString() {
        return " ACT:" + activation.getId() +
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

        Iterator<Map.Entry<Integer, Position>> ita = activation.getSlots().entrySet().iterator();
        Iterator<Map.Entry<Integer, Position>> itb = c.activation.getSlots().entrySet().iterator();

        Map.Entry<Integer, Position> mea;
        Map.Entry<Integer, Position> meb;
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