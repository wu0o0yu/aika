package network.aika.neuron.conjunctive;

import network.aika.neuron.Neuron;

public interface CategoryInputSynapse<S extends ConjunctiveSynapse> {

    S init(Neuron input, Neuron output, double initialWeight);
}
