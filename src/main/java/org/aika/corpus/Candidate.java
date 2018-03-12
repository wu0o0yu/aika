package org.aika.corpus;

import org.aika.Utils;
import org.aika.lattice.NodeActivation;
import org.aika.neuron.Activation;
import org.aika.corpus.SearchNode.Decision;



public class Candidate  implements Comparable<Candidate> {
    public SearchNode currentSearchNode;

    /**
     * The cached decision is used to avoid having to explore the same candidate twice even though nothing that
     * influences this candidate has changed.
     */
    public Decision cachedDecision = Decision.UNKNOWN;

    /**
     * The cached search node is used to avoid having to recompute the activation values and weights that are associated
     * with this search node.
     */
    public SearchNode cachedSearchNode;
    public SearchNode bestChildNode;

    public Activation activation;

    int[] debugCounts = new int[3];
    int[] debugDecisionCounts = new int[3];
    int[] debugComputed = new int[3];

    int id;
    int sequence = 0;

    public boolean queued;

    public Candidate(Activation act, int id) {
        this.activation = act;
        this.id = id;
        act.candidate = this;
        sequence = act.getSequence();
    }


    public boolean isConflicting() {
        return activation.conflicts.hasConflicts();
    }


    public boolean checkDependenciesSatisfied(long v) {
        for (Activation.SynapseActivation sa : activation.neuronInputs) {
            if (sa.input.markedHasCandidate != v && !sa.synapse.key.isRecurrent && sa.input.upperBound > 0.0) return false;
        }
        return true;
    }


    public String toString() {
        return " CID:" + id +
                " CONFLICT:" + isConflicting() +
                " LIMITED:" + debugCounts[SearchNode.DebugState.LIMITED.ordinal()] +
                " CACHED:" + debugCounts[SearchNode.DebugState.CACHED.ordinal()] +
                " EXPLORE:" + debugCounts[SearchNode.DebugState.EXPLORE.ordinal()] +
                " SELECTED:" + debugDecisionCounts[0] +
                " EXCLUDED:" + debugDecisionCounts[1] +
                " SIM-CACHED:" + debugComputed[0] +
                " SIM-COMPUTED:" + debugComputed[1] +
                " MODIFIED:" + debugComputed[2] +
                " ACT-ID:" + activation.id +
                " " + activation.key.range +
                " " + activation.getLabel();
    }


    @Override
    public int compareTo(Candidate c) {
        if(!isConflicting() && c.isConflicting()) return -1;
        if(isConflicting() && !c.isConflicting()) return 1;

        int r = Integer.compare(activation.key.range.begin, c.activation.key.range.begin);
        if (r != 0) return r;
        r = Integer.compare(activation.key.range.end, c.activation.key.range.end);
        if (r != 0) return r;

        r = Integer.compare(sequence, c.sequence);
        if (r != 0) return r;

        r = Utils.compareInteger(activation.key.rid, c.activation.key.rid);
        if (r != 0) return r;
        return Integer.compare(id, c.id);
    }
}