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

    Activation getInput(Activation fromAct, Activation toAct);

    Activation getOutput(Activation fromAct, Activation toAct);

    Neuron getNeuron(Synapse s);

    Activation getActivation(Link l);

    Stream<Link> getLinks(Activation act);

    Stream<? extends Synapse> getSynapses(Neuron n);
}
