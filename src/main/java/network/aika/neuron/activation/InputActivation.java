package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.activation.search.SearchNode;

import java.util.Deque;

public class InputActivation extends Activation {


    public InputActivation(Document doc, INeuron neuron) {
        super(doc, neuron);
    }


    @Override
    public double getP(Option o) {
        return 1;
    }

    @Override
    protected Fired incrementFired(Fired f) {
        return f;
    }

    @Override
    public Option getInputExcitatoryOption(Option o) {
        return null;
    }

    @Override
    public boolean addToValueQueue(Deque<Activation> queue, SearchNode sn) {
        return false;
    }

    @Override
    protected State getInputState(Synapse s, Activation act, SearchNode sn) {
        return null;
    }

    @Override
    public boolean needsPropagation(SearchNode sn, boolean lowerBoundChange, boolean upperBoundChange) {
        return false;
    }

    @Override
    public boolean checkSelfReferencingRecursiveStep(Activation act, int depth) {
        return false;
    }

    @Override
    protected Activation getInputExcitatoryActivation() {
        return null;
    }

    @Override
    protected String getDecisionString() {
        return "";
    }
}
