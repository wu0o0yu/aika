package network.aika.elements.synapses;

import network.aika.elements.neurons.CategoryNeuron;
import network.aika.elements.neurons.ConjunctiveNeuron;
import network.aika.elements.neurons.Neuron;

public interface CategoryInputSynapse<N extends CategoryNeuron, S extends ConjunctiveSynapse> {

    S init(Neuron input, Neuron output);

    N getInput();

    ConjunctiveNeuron getOutput();
}
