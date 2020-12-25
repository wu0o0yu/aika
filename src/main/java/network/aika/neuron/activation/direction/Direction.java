package network.aika.neuron.activation.direction;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public interface Direction {
    Direction INPUT = new Input();
    Direction OUTPUT = new Output();

    Direction invert();

    Activation getCycleInput(Activation fromAct, Activation toAct);

    Activation getCycleOutput(Activation fromAct, Activation toAct);

    Activation getPropagateInput(Activation fromAct, Activation toAct);

    Activation getPropagateOutput(Activation fromAct, Activation toAct);

    Neuron getNeuron(Synapse s);

    Activation getActivation(Link l);

    Stream<Link> getLinks(Activation act);

    Stream<? extends Synapse> getSynapses(Neuron n);
}
