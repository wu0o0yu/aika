package network.aika.neuron.activation;

import network.aika.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.activation.Decision.UNKNOWN;


public class Option implements Comparable<Option> {

    public State state = State.ZERO;

    Activation act;
    public SearchNode searchNode;

    public Option parent;
    public Option child;

    boolean fixed = false;

    public Decision decision;

    public double weight;
    public int cacheFactor = 1;
    public double p;

    public Map<Activation.Link, Option> inputOptions = new TreeMap<>(INPUT_COMP);
    public Map<Activation.Link, Option> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:

    private boolean isQueued;


    public Option(Activation act, SearchNode sn, Decision d) {
        this.act = act;
        this.searchNode = sn;
        this.decision = d;
    }


    public boolean set(State s) {
        assert !fixed;
        if(state.equalsWithWeights(s)) {
            return false;
        }

        state = s;
        return true;
    }


    public State get() {
        return state;
    }


    public State getLast() {
        return state;
    }


    public void setQueued(boolean v) {
        isQueued = v;
    }


    public boolean isQueued() {
        return isQueued;
    }


    public void reset() {
        state = State.ZERO;
    }


    public boolean compare(Option r) {
        return state.equalsWithWeights(r.state);
    }


    public boolean isActive() {
        return state.value > 0.0;
    }


    public void link() {
        for(Activation.Link l: act.getInputLinks().collect(Collectors.toList())) {
            Activation iAct = l.getInput();
            if(iAct.currentOption != null && iAct.currentOption.decision != UNKNOWN && iAct.currentOption.isActive()) {
                link(l, iAct.currentOption);
            }
        }
    }


    public void setWeight(double weight) {
        this.weight = weight;
/*
        for(Activation.Link l: act.getInputLinks().collect(Collectors.toList())) {
            if(l.getInput().getDecision() == SELECTED) {
                if(l.getInput().getCandidateId() != null) {
                    if (l.getInput().getCandidateId() < act.getCandidateId()) {
                        SearchNode inputSN = l.getInput().currentSearchState.currentSearchNode.getParent();

                        link(l, inputSN.getCurrentOption());
                    }
                } else {
                    link(l, l.getInput().options.get(0));
                }
            }
        }

        for(Activation.Link l: act.getOutputLinks().collect(Collectors.toList())) {
            if(l.getInput().getDecision() == SELECTED) {
                if(l.getOutput().getCandidateId() != null) {
                    if(l.getOutput().getCandidateId() < act.getCandidateId()) {
                        SearchNode outputSN = l.getOutput().currentSearchState.currentSearchNode.getParent();

                        outputSN.getCurrentOption().link(l, this);
                    }
                }
            }
        }
*/
    }


    public void link(Activation.Link l, Option in) {
        inputOptions.put(l, in);
        in.outputOptions.put(l, this);
    }


    public void setCacheFactor(int cf) {
        cacheFactor = cf;
    }


    public Activation getAct() {
        return act;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" snId:" + searchNode.getId() + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p);
        sb.append(state.value + " ");
        return sb.toString();
    }


    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(searchNode.getId(), o.searchNode.getId());
    }
}
