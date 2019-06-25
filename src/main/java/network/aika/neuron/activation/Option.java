package network.aika.neuron.activation;

import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static network.aika.neuron.INeuron.ALLOW_WEAK_NEGATIVE_WEIGHTS;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.Activation.Link.INPUT_COMP;
import static network.aika.neuron.activation.Activation.Link.OUTPUT_COMP;
import static network.aika.neuron.activation.SearchNode.Decision.SELECTED;



public class Option implements Comparable<Option> {

    Activation act;
    public int snId;
    public Activation.State state;
    public SearchNode.Decision decision;

    public double weight;
    public int cacheFactor = 1;
    public double p;

    public Map<Activation.Link, Option> inputOptions = new TreeMap<>(INPUT_COMP);
    public Map<Activation.Link, Option> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:


    public Option(Activation act, int snId, SearchNode.Decision d) {
        this.snId = snId;
        this.state = act.rounds.getLast();
        this.decision = d;

        if (act.options == null) {
            act.options = new ArrayList<>();
        }
        act.options.add(this);
    }


    public Activation.State computeValueAndWeight(int round) throws Activation.RecursiveDepthExceededException {
        INeuron n = act.getINeuron();
        INeuron.SynapseSummary ss = n.getSynapseSummary();

        double net = n.getTotalBias(CURRENT);
        double posNet = n.getTotalBias(CURRENT);

        int fired = -1;

        long v = act.getDocument().getNewVisitedId();
        act.markPredecessor(v, 0);

        for (Activation.InputState is: act.getInputStates(round, v)) {
            Synapse s = is.l.synapse;
            Activation iAct = is.l.input;

            if (iAct == act) continue;

            double x = Math.min(s.getLimit(), is.s.value) * s.getWeight();
            net += x;

            net += s.computeRelationWeights(is.l);

            if(!s.isNegative(CURRENT)) {
                posNet += x;
            }

            if (!s.isRecurrent() && !s.isNegative(CURRENT) && net >= 0.0 && fired < 0) {
                fired = iAct.rounds.get(round).fired + 1;
            }
        }

        for(Synapse s : n.getPassiveInputSynapses()) {
            double x = s.getWeight() * s.getInput().getPassiveInputFunction().getActivationValue(s, this);

            net += x;
            if (!s.isNegative(CURRENT)) {
                posNet += x;
            }
        }

        double actValue = n.getActivationFunction().f(net);
        double posActValue = n.getActivationFunction().f(posNet);

        double w = Math.min(-ss.getNegRecSum(), net);

        // Compute only the recurrent part is above the threshold.
        double newWeight = act.getDecision() == SELECTED ? Math.max(0.0, w) : 0.0;

        if(act.getDecision() == SELECTED || ALLOW_WEAK_NEGATIVE_WEIGHTS) {
            return new Activation.State(
                    actValue,
                    posActValue,
                    net,
                    posNet,
                    fired,
                    newWeight
            );
        } else {
            return new Activation.State(
                    0.0,
                    posActValue,
                    0.0,
                    posNet,
                    -1,
                    newWeight
            );
        }
    }


    public void setWeight(double weight) {
        this.weight = weight;

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
        return " snId:" + snId + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " " + state;
    }

    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(snId, o.snId);
    }
}
