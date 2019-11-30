package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation.Link;
import network.aika.neuron.activation.search.Decision;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;

import java.util.Deque;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.State.ZERO;
import static network.aika.neuron.activation.search.Decision.*;


public class ExcitatoryActivation extends Activation {

    public ExcitatoryActivation(int id) {
        super(id);
    }

    public ExcitatoryActivation(Document doc, INeuron neuron) {
        super(doc, neuron);
    }



    public double getP(Option o) {
        return o.p;
    }

    @Override
    protected Fired incrementFired(Fired f) {
        return new Fired(f.getInputTimestamp(), f.getFired() + 1);
    }


    public void computeOptionProbabilities() {
        getRootOption().traverse((o) -> o.computeRemainingWeight());

        final double[] offset = new double[] {Double.MAX_VALUE};
        getRootOption().traverse(o -> offset[0] = Math.min(offset[0], Math.log(o.cacheFactor) + o.remainingWeight));

        final double[] norm = new double[] {0.0};
        getRootOption().traverse(o -> norm[0] += Math.log(o.cacheFactor) + o.remainingWeight - offset[0]);

        getRootOption().traverse(o -> {
            if (o.decision == SELECTED) {
                o.setP(norm[0] != 0.0 ? Math.exp(Math.log(o.cacheFactor) + o.remainingWeight - offset[0]) / norm[0] : 1.0);
            }
        });
    }



    protected Activation getInputExcitatoryActivation() {
        return this;
    }


    @Override
    protected String getDecisionString() {
        return Utils.addPadding("" + (getFinalDecision() != null ? getFinalDecision() : "X"), 8);
    }


    @Override
    public Option getInputExcitatoryOption(Option o) {
        return o;
    }

    @Override
    public boolean addToValueQueue(Deque<Activation> queue, SearchNode sn) {
        if(getNextDecision(currentOption, sn) != UNKNOWN) {
            queue.addLast(this);
            return true;
        } else {
            return false;
        }
    }


    public State getInputState(Synapse s, Activation act, SearchNode sn) {
        State is = currentOption.getState();

        if(s.isNegative(CURRENT)) {
            if(!checkSelfReferencing(act)) {
                is = new State(is.ub, is.lb, 0.0, null, null, 0.0);
            } else {
                is = ZERO;
            }
        }

        Decision nd = act.getNextDecision(act.currentOption, sn);
        if (nd == SELECTED) {
            return new State(is.ub, is.ub, 0.0, is.firedEarliest, is.firedLatest, 0.0);
        } else if (nd == EXCLUDED) {
            return new State(is.lb, is.lb, 0.0, is.firedLatest, is.firedLatest, 0.0);
        }

        return null;
    }



    public boolean needsPropagation(SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange) {
        Decision nd = getNextDecision(currentOption, sn);
        if (nd == SELECTED) {
            return upperBoundChange;
        } else if (nd == EXCLUDED) {
            return lowerBoundChange;
        }
        return false;
    }


    public boolean checkSelfReferencingRecursiveStep(Activation act, int depth) {
        if (this == act) {
            return true;
        }

        // The activation at depth 0 might not yet be computed.
        if(depth > 0 && currentOption.getState().lb <= 0.0) {
            return false;
        }

        if(depth > MAX_SELF_REFERENCING_DEPTH) {
            return false;
        }

        for (Link l : inputLinks.values()) {
            Synapse s = l.getSynapse();
            if (!s.isWeak(CURRENT) && !s.isNegative(CURRENT) && l.getInput().checkSelfReferencingRecursiveStep(act, depth + 1)) {
                return true;
            }
        }
        return false;
    }
}
