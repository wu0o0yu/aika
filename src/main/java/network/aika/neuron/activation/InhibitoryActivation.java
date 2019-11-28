package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;

import java.util.Deque;

import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.State.ZERO;


public class InhibitoryActivation extends Activation {
    public InhibitoryActivation(Document doc, INeuron neuron) {
        super(doc, neuron);
    }


    public Option getInputExcitatoryOption(Option o) {
        if (o.inputOptions.isEmpty()) {
            return null;
        }

        Option io = o.inputOptions.firstEntry().getValue();

        return io.getAct().getInputExcitatoryOption(io);
    }


    @Override
    public boolean addToValueQueue(Deque<Activation> queue, SearchNode sn) {
        queue.addFirst(this);
        return true;
    }


    public double getP(Option o) {
        if(o.inputOptions.isEmpty()) {
            return 0.0;
        }
        return getP(o.inputOptions.firstEntry().getValue());
    }


    @Override
    protected Fired incrementFired(Fired f) {
        return f;
    }


    protected Fired computeFiredEarliest(Fired firedEarliest, Fired inputFiredEarliest, Synapse s) {
        return Fired.min(firedEarliest, inputFiredEarliest);
    }


    protected State getInputState(Synapse s, Activation act, SearchNode sn) {
        State is = currentOption.getState();

        if(s.isNegative(CURRENT)) {
            if(!checkSelfReferencing(act)) {
                is = new State(is.ub, is.lb, 0.0, null, null, 0.0);
            } else {
                is = ZERO;
            }
        }

        return is;
    }


    public boolean needsPropagation(SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange) {
        return upperBoundChange || lowerBoundChange;
    }


    protected Activation getInputExcitatoryActivation() {
        Link l = getStrongestLink();
        if(l == null) {
            return null;
        }
        return l.getInput().getInputExcitatoryActivation();
    }


    @Override
    protected String getDecisionString() {
        return Utils.addPadding("", 8);
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

        Link strongestLink = getStrongestLink();

        if (strongestLink == null) {
            return false;
        }

        return strongestLink.getInput().checkSelfReferencingRecursiveStep(act, depth + 1);
    }
}
