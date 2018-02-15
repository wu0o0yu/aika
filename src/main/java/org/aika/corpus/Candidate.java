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

    public InterpretationNode refinement;

    int[] debugCounts = new int[3];
    int[] debugDecisionCounts = new int[3];
    int[] debugComputed = new int[3];

    int id;
    int sequence = 0;
    int minBegin;
    int maxEnd;
    Integer minRid;

    public Candidate(InterpretationNode ref, int id) {
        this.refinement = ref;
        this.id = id;
        ref.candidate = this;
        if (ref.activation != null) {
            sequence = ref.activation.getSequence();
            minBegin = ref.activation.key.range.begin;
            maxEnd = ref.activation.key.range.end;
            minRid = ref.activation.key.rid;
        } else {
            for (NodeActivation act : ref.getNodeActivations()) {
                sequence = Math.max(sequence, ref.activation.getSequence());
                if (act.key.range != null) {
                    minBegin = Math.min(minBegin, act.key.range.begin);
                    maxEnd = Math.max(maxEnd, act.key.range.end);
                }
                minRid = Utils.nullSafeMin(minRid, act.key.rid);
            }
        }
    }


    public boolean isConflicting() {
        return refinement.conflicts.hasConflicts();
    }


    public boolean checkDependenciesSatisfied() {
        for (Activation.SynapseActivation sa : refinement.activation.neuronInputs) {
            if (!sa.input.hasCandidate && !sa.synapse.key.isRecurrent && sa.input.upperBound > 0.0) return false;
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
                " ACT-ID:" + refinement.id +
                " " + refinement.activation.key.range +
                " " + refinement.activation.key.interpretation +
                " " + refinement.activation.getLabel();
    }


    @Override
    public int compareTo(Candidate c) {
        if(!isConflicting() && c.isConflicting()) return -1;
        if(isConflicting() && !c.isConflicting()) return 1;

        int r = Integer.compare(minBegin, c.minBegin);
        if (r != 0) return r;
        r = Integer.compare(maxEnd, c.maxEnd);
        if (r != 0) return r;

        r = Integer.compare(sequence, c.sequence);
        if (r != 0) return r;

        r = Utils.compareInteger(minRid, c.minRid);
        if (r != 0) return r;
        return Integer.compare(id, c.id);
    }
}