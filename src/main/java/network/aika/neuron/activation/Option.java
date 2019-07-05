package network.aika.neuron.activation;

import network.aika.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.activation.Decision.UNKNOWN;


public class Option implements Comparable<Option> {

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

    private boolean[] isQueued = new boolean[3];

    public TreeMap<Integer, State> rounds = new TreeMap<>();



    public Option(Activation act, SearchNode sn, Decision d) {
        this.act = act;
        this.searchNode = sn;
        this.decision = d;

        rounds.put(0, State.ZERO);
    }


    public boolean set(int r, State s) {
        assert !fixed;
        State lr = get(r - 1);
        if(lr != null && lr.equalsWithWeights(s)) {
            State or = rounds.get(r);
            if(or != null) {
                rounds.remove(r);
                return !or.equalsWithWeights(s);
            }
            return false;
        } else {
            State or = rounds.put(r, s);

            for(Iterator<Map.Entry<Integer, State>> it = rounds.tailMap(r + 1).entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, State> me = it.next();
                if(me.getValue().equalsWithWeights(s)) it.remove();
            }
            return or == null || !or.equalsWithWeights(s);
        }
    }


    public State get(int r) {
        Map.Entry<Integer, State> me = rounds.floorEntry(r);
        return me != null ? me.getValue() : null;
    }


    public Integer getLastRound() {
        return !rounds.isEmpty() ? rounds.lastKey() : null;
    }


    public State getLast() {
        return !rounds.isEmpty() ? rounds.lastEntry().getValue() : State.ZERO;
    }


    public void setQueued(int r, boolean v) {
        if(r >= isQueued.length) {
            isQueued = Arrays.copyOf(isQueued, isQueued.length * 2);
        }
        isQueued[r] = v;
    }


    public boolean isQueued(int r) {
        return r < isQueued.length ? isQueued[r] : false;
    }


    public void reset() {
        rounds.clear();
        rounds.put(0, State.ZERO);
    }


    public boolean compare(Option r) {
        if(rounds.size() != r.rounds.size()) {
            return false;
        }
        for(Map.Entry<Integer, State> me: rounds.entrySet()) {
            State sa = me.getValue();
            State sb = r.rounds.get(me.getKey());
            if(sb == null || !sa.equalsWithWeights(sb)) {
                return false;
            }
        }

        return true;
    }


    public boolean isActive() {
        return rounds.size() <= 1 && getLast().value > 0.0;
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
        rounds.forEach((r, s) -> sb.append(r + ":" + s.value + " "));
        return sb.toString();
    }


    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(searchNode.getId(), o.searchNode.getId());
    }
}
