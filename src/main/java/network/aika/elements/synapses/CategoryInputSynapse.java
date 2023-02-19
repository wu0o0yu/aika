package network.aika.elements.synapses;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.BindingCategoryActivation;
import network.aika.elements.activations.CategoryActivation;
import network.aika.elements.links.BindingCategoryInputLink;
import network.aika.elements.links.CategoryInputLink;
import network.aika.elements.neurons.CategoryNeuron;
import network.aika.elements.neurons.ConjunctiveNeuron;
import network.aika.elements.neurons.Neuron;

public abstract class CategoryInputSynapse extends DisjunctiveSynapse<
        CategoryInputSynapse,
        CategoryNeuron,
        Neuron<Activation>,
        CategoryInputLink,
        CategoryActivation,
        Activation
        > {

    public CategoryInputSynapse(Scope scope) {
        super(scope);
    }

    S init(Neuron input, Neuron output);

    N getInput();

    ConjunctiveNeuron getOutput();

    @Override
    public BindingCategoryInputLink createLink(BindingCategoryActivation input, BindingActivation output) {
        return new BindingCategoryInputLink(this, input, output);
    }

    @Override
    public boolean isTrainingAllowed() {
        return false;
    }
}
