package network.aika.neuron;

import network.aika.neuron.activation.scopes.Scope;

import java.util.Set;

public class TemplateNeuronInfo {

    Set<Scope> inputScopes;
    Set<Scope> outputScopes;

    public Set<Scope> getInputScopes() {
        return inputScopes;
    }

    public Set<Scope> getOutputScopes() {
        return outputScopes;
    }
}
