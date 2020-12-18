package network.aika.neuron.activation.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class Same implements Direction {

    @Override
    public Direction invert() {
        return SAME;
    }

    @Override
    public Direction getNext(Direction dir) {
        return dir;
    }

    @Override
    public Activation getInput(Activation fromAct, Activation toAct) {
        return null;
    }

    @Override
    public Activation getOutput(Activation fromAct, Activation toAct) {
        return null;
    }

    @Override
    public Neuron getNeuron(Synapse s) {
        return null;
    }

    @Override
    public Activation getActivation(Link l) {
        return null;
    }

    @Override
    public Stream<Link> getLinks(Activation act) {
        return null;
    }

    @Override
    public Stream<? extends Synapse> getSynapses(Neuron n) {
        return null;
    }

    public String toString() {
        return "SAME";
    }
}
