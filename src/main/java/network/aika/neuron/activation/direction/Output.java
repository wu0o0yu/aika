package network.aika.neuron.activation.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class Output implements Direction {

    @Override
    public Direction invert() {
        return INPUT;
    }

    @Override
    public Direction getNext(Direction dir) {
        return dir == INPUT ? SAME : null;
    }

    @Override
    public Activation getInput(Activation fromAct, Activation toAct) {
        return fromAct;
    }

    @Override
    public Activation getOutput(Activation fromAct, Activation toAct) {
        return toAct;
    }

    @Override
    public Neuron getNeuron(Synapse s) {
        return s.getOutput();
    }

    public Activation getActivation(Link l) {
        return l.getOutput();
    }

    public Stream<Link> getLinks(Activation act) {
        return act.getOutputLinks();
    }

    public Stream<? extends Synapse> getSynapses(Neuron n) {
        return n.getOutputSynapses();
    }


    public String toString() {
        return "OUTPUT";
    }
}
