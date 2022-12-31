package network.aika.elements.synapses;

import network.aika.elements.neurons.Neuron;

public interface CategoryInputSynapse<S extends ConjunctiveSynapse> {

    S init(Neuron input, Neuron output, double initialWeight);
}
