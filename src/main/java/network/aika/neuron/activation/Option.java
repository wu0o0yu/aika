package network.aika.neuron.activation;

import network.aika.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.activation.Decision.UNKNOWN;


public class Option implements Comparable<Option> {

    public static final State INITIAL_STATE = new State(0.0, 1.0, 0.0, 0.0, 0.0, 0, 0.0);

    public State state = INITIAL_STATE;

    Activation act;
    public SearchNode searchNode;

    public Option parent;
    public List<Option> children = new ArrayList<>();

    boolean fixed = false;

    public Decision decision;

    public double weight;
    public int cacheFactor = 1;
    public double p;

    public Map<Activation.Link, Option> inputOptions = new TreeMap<>(INPUT_COMP);
    public Map<Activation.Link, Option> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:

    private boolean isQueued;


    public Option(Option parent, Activation act, SearchNode sn) {
        this.act = act;
        this.searchNode = sn;

        this.parent = parent;

        if(parent != null) {
            parent.children.add(this);
        }

        decision = act.getNextDecision(parent, sn);
    }


    public boolean setState(State s) {
        assert !fixed;
        if(state.equalsWithWeights(s)) {
            return false;
        }

        state = s;
        return true;
    }


    public State getState() {
        return state;
    }


    public void setQueued(boolean v) {
        isQueued = v;
    }


    public boolean isQueued() {
        return isQueued;
    }


    public void restoreState(Activation.Mode m) {
        act.currentOption = (m == Activation.Mode.OLD ? parent : this);
        assert act.currentOption.fixed;
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
        sb.append(" snId:" + (searchNode != null ? searchNode.getId() : "-") + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " value:" + Utils.round(state.value));
        return sb.toString();
    }


    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(searchNode.getId(), o.searchNode.getId());
    }
}
