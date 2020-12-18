package network.aika.neuron.activation.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class Input implements Direction {

    @Override
    public Direction invert() {
        return OUTPUT;
    }

    @Override
    public Direction getNext(Direction dir) {
        return dir == INPUT ? null : SAME;
    }

    @Override
    public Activation getInput(Activation fromAct, Activation toAct) {
        return toAct;
    }

    @Override
    public Activation getOutput(Activation fromAct, Activation toAct) {
        return fromAct;
    }

    @Override
    public Neuron getNeuron(Synapse s) {
        return s.getInput();
    }

    @Override
    public Activation getActivation(Link l) {
        return l.getInput();
    }

    @Override
    public Stream<Link> getLinks(Activation act) {
        return act.getInputLinks();
    }

    @Override
    public Stream<? extends Synapse> getSynapses(Neuron n) {
        return n.getInputSynapses();
    }

    public String toString() {
        return "INPUT";
    }
}
