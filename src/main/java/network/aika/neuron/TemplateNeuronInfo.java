package network.aika.neuron;

import network.aika.neuron.activation.scopes.Scope;

import java.util.Collections;
import java.util.Set;

public class TemplateNeuronInfo {

    Set<Scope> inputScopes = Collections.emptySet();
    Set<Scope> outputScopes = Collections.emptySet();

    public Set<Scope> getInputScopes() {
        return inputScopes;
    }

    public Set<Scope> getOutputScopes() {
        return outputScopes;
    }
}
