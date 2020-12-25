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
    public Activation getCycleInput(Activation fromAct, Activation toAct) {
        return toAct;
    }

    @Override
    public Activation getCycleOutput(Activation fromAct, Activation toAct) {
        return fromAct;
    }

    @Override
    public Activation getPropagateInput(Activation fromAct, Activation toAct) {
        return fromAct;
    }

    @Override
    public Activation getPropagateOutput(Activation fromAct, Activation toAct) {
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
