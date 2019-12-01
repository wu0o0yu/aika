package network.aika.neuron.activation.search;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Bounds;

public interface Input {

    Synapse getSynapse();

    Bounds getBounds();
}
